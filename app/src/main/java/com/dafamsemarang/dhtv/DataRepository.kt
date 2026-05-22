package com.dafamsemarang.dhtv
 
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.database.*
 
object DataRepository {
    // Live data holders (Compose mutableState for reactivity)
    val menuItems = mutableStateOf<List<MenuItemData>>(emptyList())
    val requestItems = mutableStateOf<List<GuestRequest>>(emptyList())
 
    // Slideshow & Video Live data holders
    val slideshowImages = mutableStateOf<List<String>>(emptyList())
    val slideshowDurations = mutableStateOf<List<Int>>(emptyList())
    val isSlideshowActive = mutableStateOf(false)
    val isLoadingSlideshow = mutableStateOf(true)
    
    val videoUrls = mutableStateOf<List<String>>(emptyList())
    val isLoadingVideos = mutableStateOf(true)

    // Loading state flags
    val isMenuLoaded = mutableStateOf(false)
    val isRequestLoaded = mutableStateOf(false)
 
    private var menuListener: ValueEventListener? = null
    private var requestListener: ValueEventListener? = null
    private var slideshowListener: ValueEventListener? = null
    private var videoListener: ValueEventListener? = null

    private var activeBranchId: String? = null
    private var activeMenuRef: DatabaseReference? = null
    private var activeRequestRef: DatabaseReference? = null
    private var activeSlideshowRef: DatabaseReference? = null
    private var activeVideoRef: DatabaseReference? = null
 
    fun startPreload(context: android.content.Context, branchId: String?) {
        if (branchId == null) return
        
        // Idempotency check: if already preloading this branch, do nothing
        if (branchId == activeBranchId && 
            menuListener != null && 
            requestListener != null && 
            slideshowListener != null && 
            videoListener != null
        ) {
            Log.d("DataRepository", "Preload already active for branch: $branchId")
            return
        }
 
        Log.d("DataRepository", "Starting/restarting preload for branch: $branchId")
        cleanup()
 
        activeBranchId = branchId
        val db = FirebaseDatabase.getInstance().reference
        
        // Preload Menu items
        val menuRef = db.child("BRANCHES").child(branchId).child("FOOD_BEVERAGE").child("food")
        activeMenuRef = menuRef
        menuListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val raw = child.value as? Map<*, *>
                    val isActive = raw?.get("isActive") as? Boolean == true
                    if (!isActive) return@mapNotNull null
                    child.getValue(MenuItemData::class.java)?.copy(branchId = branchId)
                }
                menuItems.value = items
                isMenuLoaded.value = true
                Log.d("DataRepository", "Menu items loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Menu preload cancelled: ${error.message}")
                isMenuLoaded.value = true // set true to allow UI to proceed out of loading state
            }
        }
        menuRef.addValueEventListener(menuListener!!)
 
        // Preload Requests
        val requestRef = db.child("BRANCHES").child(branchId).child("GUEST_REQUEST").child("requests")
        activeRequestRef = requestRef
        requestListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = snapshot.children.mapNotNull { it.getValue(GuestRequest::class.java) }
                requestItems.value = requests
                isRequestLoaded.value = true
                Log.d("DataRepository", "Requests loaded: ${requests.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Request preload cancelled: ${error.message}")
                isRequestLoaded.value = true // set true to allow UI to proceed out of loading state
            }
        }
        requestRef.addValueEventListener(requestListener!!)

        // Preload Slideshow
        val slideshowRef = db.child("BRANCHES").child(branchId).child("SLIDESHOW").child("imageUrls")
        activeSlideshowRef = slideshowRef
        slideshowListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val activeSlides = snapshot.children.mapNotNull { slideSnapshot ->
                        try {
                            val url = slideSnapshot.child("url").getValue(String::class.java)
                            val duration = slideSnapshot.child("duration").getValue(Int::class.java) ?: 5
                            val status = slideSnapshot.child("status").getValue(String::class.java)
                            val title = slideSnapshot.child("title").getValue(String::class.java)
                            if (url != null && status == "active") SlideData(url, duration, title) else null
                        } catch (e: Exception) { null }
                    }
                    if (activeSlides.isNotEmpty()) {
                        isSlideshowActive.value = true
                        slideshowImages.value = activeSlides.map { it.url }
                        slideshowDurations.value = activeSlides.map { it.duration }
                    } else {
                        isSlideshowActive.value = false
                        slideshowImages.value = emptyList()
                        slideshowDurations.value = emptyList()
                    }
                    isLoadingSlideshow.value = false
                    Log.d("DataRepository", "Slideshow preloaded successfully: ${slideshowImages.value.size} active slides")
                } catch (e: Exception) {
                    isLoadingSlideshow.value = false
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Slideshow preload cancelled: ${error.message}")
                isLoadingSlideshow.value = false
            }
        }
        slideshowRef.addValueEventListener(slideshowListener!!)

        // Preload Video
        val videoRef = db.child("BRANCHES").child(branchId).child("VIDEO").child("videoUrl")
        activeVideoRef = videoRef
        videoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val urls = snapshot.children.mapNotNull { videoSnapshot ->
                        val status = videoSnapshot.child("status").getValue(String::class.java)
                        val url = videoSnapshot.child("url").getValue(String::class.java)
                        if (status == "active" && url != null) url else null
                    }
                    videoUrls.value = urls
                    isLoadingVideos.value = false
                    Log.d("DataRepository", "Videos preloaded successfully: ${urls.size} active videos")
                } catch (e: Exception) {
                    isLoadingVideos.value = false
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Video preload cancelled: ${error.message}")
                isLoadingVideos.value = false
            }
        }
        videoRef.addValueEventListener(videoListener!!)
    }
 
    fun cleanup() {
        activeMenuRef?.let { ref ->
            menuListener?.let { listener ->
                ref.removeEventListener(listener)
            }
        }
        activeRequestRef?.let { ref ->
            requestListener?.let { listener ->
                ref.removeEventListener(listener)
            }
        }
        activeSlideshowRef?.let { ref ->
            slideshowListener?.let { listener ->
                ref.removeEventListener(listener)
            }
        }
        activeVideoRef?.let { ref ->
            videoListener?.let { listener ->
                ref.removeEventListener(listener)
            }
        }
        menuListener = null
        requestListener = null
        slideshowListener = null
        videoListener = null
        activeMenuRef = null
        activeRequestRef = null
        activeSlideshowRef = null
        activeVideoRef = null
        activeBranchId = null
        isMenuLoaded.value = false
        isRequestLoaded.value = false
        isSlideshowActive.value = false
        isLoadingSlideshow.value = true
        isLoadingVideos.value = true
        slideshowImages.value = emptyList()
        slideshowDurations.value = emptyList()
        videoUrls.value = emptyList()
    }
}
