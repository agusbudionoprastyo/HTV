import re

with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'r') as f:
    content = f.read()

# Update the variable declarations
vars_to_replace = """    // Smart Home (Tuya)
    val tuyaDeviceId = mutableStateOf<String?>(null)
    val tuyaDeviceName = mutableStateOf<String?>("Smart Switch")
    val tuyaSwitch1State = mutableStateOf(false)
    val tuyaSwitch2State = mutableStateOf(false)
    val tuyaSwitch1Name = mutableStateOf<String?>("")
    val tuyaSwitch2Name = mutableStateOf<String?>("")"""

new_vars = """    // Smart Home (Tuya) - Fixed Layout
    val tuyaSwitch1Id = mutableStateOf<String?>(null)
    val tuyaSwitch2Id = mutableStateOf<String?>(null)
    val tuyaSwitch3Id = mutableStateOf<String?>(null)
    val tuyaAcId = mutableStateOf<String?>(null)
    val tuyaCurtainId = mutableStateOf<String?>(null)

    val tuyaSwitch1Name = mutableStateOf<String?>("Switch 1")
    val tuyaSwitch2Name = mutableStateOf<String?>("Switch 2")
    val tuyaSwitch3Name = mutableStateOf<String?>("Switch 3")
    val tuyaAcName = mutableStateOf<String?>("AC")
    val tuyaCurtainName = mutableStateOf<String?>("Curtain")

    val tuyaSwitch1State = mutableStateOf(false)
    val tuyaSwitch2State = mutableStateOf(false)
    val tuyaSwitch3State = mutableStateOf(false)

    val tuyaAcPowerState = mutableStateOf(false)
    val tuyaAcTemp = mutableStateOf(24)
    val tuyaAcMode = mutableStateOf("cool")

    val tuyaCurtainState = mutableStateOf("stop")"""

content = content.replace(vars_to_replace, new_vars)

# Update listeners references
listeners_to_replace = """    private var tuyaRoomListener: ValueEventListener? = null
    private var tuyaStatusListener: ValueEventListener? = null

    private var activeTuyaRoomRef: DatabaseReference? = null
    private var activeTuyaStatusRef: DatabaseReference? = null"""

new_listeners = """    private var tuyaRoomListener: ValueEventListener? = null
    private var activeTuyaRoomRef: DatabaseReference? = null
    
    private val tuyaStatusListeners = mutableMapOf<String, ValueEventListener>()
    private val activeTuyaStatusRefs = mutableMapOf<String, DatabaseReference>()"""

content = content.replace(listeners_to_replace, new_listeners)

# Update the listener logic
logic_to_replace = """        // Fetch Room for Tuya
        val tuyaPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val roomNumber = tuyaPrefs.getString("room", null)
        if (!roomNumber.isNullOrEmpty()) {
            val tuyaRef = db.child("BRANCHES").child(branchId).child("SMART_HOME").child("rooms").child(roomNumber)
            activeTuyaRoomRef = tuyaRef
            tuyaRoomListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val deviceId = snapshot.child("deviceId").getValue(String::class.java)
                        tuyaDeviceId.value = deviceId
                        tuyaDeviceName.value = snapshot.child("deviceName").getValue(String::class.java) ?: "Lampu Kamar"
                        tuyaSwitch1Name.value = snapshot.child("switch1Name").getValue(String::class.java) ?: ""
                        tuyaSwitch2Name.value = snapshot.child("switch2Name").getValue(String::class.java) ?: ""
                        
                        if (deviceId != null) {
                            // Listen to webhook-updated status
                            activeTuyaStatusRef?.removeEventListener(tuyaStatusListener!!)
                            val statusRef = db.child("SMART_HOME").child("devices").child(deviceId).child("status")
                            activeTuyaStatusRef = statusRef
                            tuyaStatusListener = object : ValueEventListener {
                                override fun onDataChange(statusSnap: DataSnapshot) {
                                    if (statusSnap.exists()) {
                                        tuyaSwitch1State.value = statusSnap.child("switch_1").getValue(Boolean::class.java) ?: false
                                        tuyaSwitch2State.value = statusSnap.child("switch_2").getValue(Boolean::class.java) ?: false
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            }
                            statusRef.addValueEventListener(tuyaStatusListener!!)
                        }
                    } else {
                        tuyaDeviceId.value = null
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            tuyaRef.addValueEventListener(tuyaRoomListener!!)
        }"""

new_logic = """        // Fetch Room for Tuya
        val tuyaPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val roomNumber = tuyaPrefs.getString("room", null)
        if (!roomNumber.isNullOrEmpty()) {
            val tuyaRef = db.child("BRANCHES").child(branchId).child("SMART_HOME").child("rooms").child(roomNumber)
            activeTuyaRoomRef = tuyaRef
            tuyaRoomListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear old listeners
                    activeTuyaStatusRefs.forEach { (id, ref) ->
                        tuyaStatusListeners[id]?.let { ref.removeEventListener(it) }
                    }
                    activeTuyaStatusRefs.clear()
                    tuyaStatusListeners.clear()

                    if (snapshot.exists()) {
                        tuyaSwitch1Id.value = snapshot.child("switch1").getValue(String::class.java)
                        tuyaSwitch2Id.value = snapshot.child("switch2").getValue(String::class.java)
                        tuyaSwitch3Id.value = snapshot.child("switch3").getValue(String::class.java)
                        tuyaAcId.value = snapshot.child("ac").getValue(String::class.java)
                        tuyaCurtainId.value = snapshot.child("curtain").getValue(String::class.java)
                        
                        tuyaSwitch1Name.value = snapshot.child("switch1Name").getValue(String::class.java) ?: "Switch 1"
                        tuyaSwitch2Name.value = snapshot.child("switch2Name").getValue(String::class.java) ?: "Switch 2"
                        tuyaSwitch3Name.value = snapshot.child("switch3Name").getValue(String::class.java) ?: "Switch 3"
                        tuyaAcName.value = snapshot.child("acName").getValue(String::class.java) ?: "AC"
                        tuyaCurtainName.value = snapshot.child("curtainName").getValue(String::class.java) ?: "Curtain"

                        fun attachListener(id: String?, type: String) {
                            if (id.isNullOrEmpty()) return
                            val statusRef = db.child("SMART_HOME").child("devices").child(id).child("status")
                            val listener = object : ValueEventListener {
                                override fun onDataChange(statusSnap: DataSnapshot) {
                                    if (statusSnap.exists()) {
                                        when (type) {
                                            "switch1" -> tuyaSwitch1State.value = statusSnap.child("switch_1").getValue(Boolean::class.java) ?: false
                                            "switch2" -> tuyaSwitch2State.value = statusSnap.child("switch_1").getValue(Boolean::class.java) ?: false
                                            "switch3" -> tuyaSwitch3State.value = statusSnap.child("switch_1").getValue(Boolean::class.java) ?: false
                                            "ac" -> {
                                                tuyaAcPowerState.value = statusSnap.child("switch").getValue(Boolean::class.java) ?: false
                                                tuyaAcTemp.value = statusSnap.child("temp_set").getValue(Int::class.java) ?: 24
                                                tuyaAcMode.value = statusSnap.child("mode").getValue(String::class.java) ?: "cool"
                                            }
                                            "curtain" -> tuyaCurtainState.value = statusSnap.child("control").getValue(String::class.java) ?: "stop"
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            }
                            activeTuyaStatusRefs[id] = statusRef
                            tuyaStatusListeners[id] = listener
                            statusRef.addValueEventListener(listener)
                        }

                        attachListener(tuyaSwitch1Id.value, "switch1")
                        attachListener(tuyaSwitch2Id.value, "switch2")
                        attachListener(tuyaSwitch3Id.value, "switch3")
                        attachListener(tuyaAcId.value, "ac")
                        attachListener(tuyaCurtainId.value, "curtain")
                    } else {
                        tuyaSwitch1Id.value = null
                        tuyaSwitch2Id.value = null
                        tuyaSwitch3Id.value = null
                        tuyaAcId.value = null
                        tuyaCurtainId.value = null
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            tuyaRef.addValueEventListener(tuyaRoomListener!!)
        }"""

content = content.replace(logic_to_replace, new_logic)

# Cleanup logic
cleanup_replace = """        activeTuyaRoomRef?.removeEventListener(tuyaRoomListener!!)
        activeTuyaStatusRef?.removeEventListener(tuyaStatusListener!!)"""

new_cleanup = """        activeTuyaRoomRef?.removeEventListener(tuyaRoomListener!!)
        activeTuyaStatusRefs.forEach { (id, ref) ->
            tuyaStatusListeners[id]?.let { ref.removeEventListener(it) }
        }"""

content = content.replace(cleanup_replace, new_cleanup)


with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'w') as f:
    f.write(content)

