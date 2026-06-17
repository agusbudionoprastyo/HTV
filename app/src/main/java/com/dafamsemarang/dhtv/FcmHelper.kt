package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GoogleOAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int
)

object FcmHelper {

    private fun getPrivateKey(pemKey: String): java.security.PrivateKey {
        val privateKeyPEM = pemKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
        val decoded = android.util.Base64.decode(privateKeyPEM, android.util.Base64.DEFAULT)
        val spec = java.security.spec.PKCS8EncodedKeySpec(decoded)
        val kf = java.security.KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    private fun generateJwt(clientEmail: String, privateKeyPem: String): String {
        val header = android.util.Base64.encodeToString(
            "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
        )
        val iat = System.currentTimeMillis() / 1000
        val exp = iat + 3600
        val claims = "{\"iss\":\"$clientEmail\",\"scope\":\"https://www.googleapis.com/auth/firebase.messaging\",\"aud\":\"https://oauth2.googleapis.com/token\",\"exp\":$exp,\"iat\":$iat}"
        val payload = android.util.Base64.encodeToString(
            claims.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
        )
        val signatureInput = "$header.$payload"
        val privateKey = getPrivateKey(privateKeyPem)
        val signature = java.security.Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signatureInput.toByteArray(Charsets.UTF_8))
        }.sign()
        val signatureString = android.util.Base64.encodeToString(
            signature,
            android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
        )
        return "$signatureInput.$signatureString"
    }

    fun sendFcmNotification(
        context: Context,
        type: String, // "DND" or "REQUEST" or "ROOM_SERVICE"
        title: String,
        bodyText: String,
        additionalData: Map<String, String> = emptyMap()
    ) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val branchId = sharedPreferences.getString("branchId", null) ?: return
        
        val database = FirebaseDatabase.getInstance().reference
        // Read global FCM V1 credentials directly from FCM_GATEWAY at root level
        val globalFcmRef = database.child("FCM_GATEWAY")
        // Read branch-specific topic
        val branchFcmRef = database.child("BRANCHES").child(branchId).child("FCM_GATEWAY")
        
        globalFcmRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(globalSnapshot: DataSnapshot) {
                val projectId = globalSnapshot.child("projectId").getValue(String::class.java)
                val clientEmail = globalSnapshot.child("clientEmail").getValue(String::class.java)
                val privateKey = globalSnapshot.child("privateKey").getValue(String::class.java)
                
                if (projectId.isNullOrEmpty() || clientEmail.isNullOrEmpty() || privateKey.isNullOrEmpty()) {
                    Log.e("FcmHelper", "Cannot send FCM V1: Credentials (projectId, clientEmail, privateKey) are missing in FCM_GATEWAY")
                    return
                }
                
                branchFcmRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(branchSnapshot: DataSnapshot) {
                        val configuredTopic = branchSnapshot.child("topic").getValue(String::class.java)
                        
                        // Fallback to "BRANCH_$branchId" if topic is not configured
                        val topic = if (!configuredTopic.isNullOrEmpty()) configuredTopic else "BRANCH_$branchId"
                        
                        val fcmV1Request = FcmV1MessageRequest(
                            message = FcmV1Message(
                                topic = topic,
                                notification = FcmV1Notification(
                                    title = title,
                                    body = bodyText
                                ),
                                data = mapOf(
                                    "type" to type,
                                    "title" to title,
                                    "body" to bodyText
                                ) + additionalData
                            )
                        )
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val client = HttpClient(Android) {
                                    install(ContentNegotiation) {
                                        json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
                                    }
                                }
                                
                                // Step 1: Generate JWT & Request Google Access Token
                                val jwt = generateJwt(clientEmail, privateKey)
                                val tokenResponse = client.post("https://oauth2.googleapis.com/token") {
                                    contentType(ContentType.Application.FormUrlEncoded)
                                    setBody("grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt")
                                }
                                
                                if (tokenResponse.status != HttpStatusCode.OK) {
                                    Log.e("FcmHelper", "OAuth2 Token request failed: ${tokenResponse.status}, body: ${tokenResponse.bodyAsText()}")
                                    client.close()
                                    return@launch
                                }
                                
                                val tokenResponseBody = tokenResponse.bodyAsText()
                                val oauthResponse = Json.decodeFromString<GoogleOAuthTokenResponse>(tokenResponseBody)
                                val accessToken = oauthResponse.accessToken
                                
                                // Step 2: Send FCM HTTP V1 Notification
                                val endpointUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
                                Log.d("FcmHelper", "Sending FCM V1 to topic: $topic, type: $type")
                                
                                val fcmResponse = client.post(endpointUrl) {
                                    contentType(ContentType.Application.Json)
                                    header("Authorization", "Bearer $accessToken")
                                    setBody(fcmV1Request)
                                }
                                
                                val responseBody = fcmResponse.bodyAsText()
                                if (fcmResponse.status == HttpStatusCode.OK) {
                                    Log.d("FcmHelper", "FCM V1 sent successfully: $responseBody")
                                } else {
                                    Log.e("FcmHelper", "FCM V1 failed with status: ${fcmResponse.status}, body: $responseBody")
                                }
                                client.close()
                            } catch (e: Exception) {
                                Log.e("FcmHelper", "Error sending FCM V1 notification: ${e.message}", e)
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FcmHelper", "Failed to load branch FCM settings: ${error.message}")
                    }
                })
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("FcmHelper", "Failed to load global FCM credentials: ${error.message}")
            }
        })
    }
}
