import re

with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'r') as f:
    content = f.read()

# Replace the 5 fixed devices variables with the list
vars_to_replace = """    // Smart Home (Tuya) - Fixed Layout
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

new_vars = """    // Smart Home (Tuya) - Dynamic Layout
    data class SmartDeviceData(
        val deviceId: String,
        val deviceName: String,
        val type: String, // "switch", "ac", "curtain"
        // Specific to switch
        val switch1Name: String? = null,
        val switch2Name: String? = null,
        val switch3Name: String? = null,
        // Status properties
        var switch1State: Boolean = false,
        var switch2State: Boolean = false,
        var switch3State: Boolean = false,
        var acPowerState: Boolean = false,
        var acTemp: Int = 24,
        var acMode: String = "cool",
        var curtainState: String = "stop"
    )

    val smartDevicesList = mutableStateOf<List<SmartDeviceData>>(emptyList())"""

content = content.replace(vars_to_replace, new_vars)

# Replace the fetching logic
logic_to_replace = """        // Fetch Room for Tuya
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

new_logic = """        // Fetch Room for Tuya (Dynamic Devices)
        val tuyaPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val roomNumber = tuyaPrefs.getString("room", null)
        if (!roomNumber.isNullOrEmpty()) {
            val tuyaRef = db.child("BRANCHES").child(branchId).child("SMART_HOME").child("rooms").child(roomNumber).child("devices")
            activeTuyaRoomRef = tuyaRef
            tuyaRoomListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear old listeners
                    activeTuyaStatusRefs.forEach { (id, ref) ->
                        tuyaStatusListeners[id]?.let { ref.removeEventListener(it) }
                    }
                    activeTuyaStatusRefs.clear()
                    tuyaStatusListeners.clear()

                    val newDevices = mutableListOf<SmartDeviceData>()
                    
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val deviceId = child.child("deviceId").getValue(String::class.java) ?: continue
                            val deviceName = child.child("deviceName").getValue(String::class.java) ?: "Unknown Device"
                            val type = child.child("type").getValue(String::class.java) ?: "switch"
                            
                            val switch1Name = child.child("switch1Name").getValue(String::class.java)
                            val switch2Name = child.child("switch2Name").getValue(String::class.java)
                            val switch3Name = child.child("switch3Name").getValue(String::class.java)
                            
                            val deviceData = SmartDeviceData(
                                deviceId = deviceId,
                                deviceName = deviceName,
                                type = type,
                                switch1Name = switch1Name,
                                switch2Name = switch2Name,
                                switch3Name = switch3Name
                            )
                            newDevices.add(deviceData)
                        }
                    }
                    
                    // Assign to state and setup listeners
                    smartDevicesList.value = newDevices
                    
                    newDevices.forEach { device ->
                        val statusRef = db.child("SMART_HOME").child("devices").child(device.deviceId).child("status")
                        val listener = object : ValueEventListener {
                            override fun onDataChange(statusSnap: DataSnapshot) {
                                if (statusSnap.exists()) {
                                    val currentList = smartDevicesList.value.toMutableList()
                                    val index = currentList.indexOfFirst { it.deviceId == device.deviceId }
                                    if (index != -1) {
                                        val updatedDevice = currentList[index].copy()
                                        when (updatedDevice.type) {
                                            "switch" -> {
                                                updatedDevice.switch1State = statusSnap.child("switch_1").getValue(Boolean::class.java) ?: false
                                                updatedDevice.switch2State = statusSnap.child("switch_2").getValue(Boolean::class.java) ?: false
                                                updatedDevice.switch3State = statusSnap.child("switch_3").getValue(Boolean::class.java) ?: false
                                            }
                                            "ac" -> {
                                                updatedDevice.acPowerState = statusSnap.child("switch").getValue(Boolean::class.java) ?: false
                                                updatedDevice.acTemp = statusSnap.child("temp_set").getValue(Int::class.java) ?: 24
                                                updatedDevice.acMode = statusSnap.child("mode").getValue(String::class.java) ?: "cool"
                                            }
                                            "curtain" -> {
                                                updatedDevice.curtainState = statusSnap.child("control").getValue(String::class.java) ?: "stop"
                                            }
                                        }
                                        currentList[index] = updatedDevice
                                        smartDevicesList.value = currentList
                                    }
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        }
                        activeTuyaStatusRefs[device.deviceId] = statusRef
                        tuyaStatusListeners[device.deviceId] = listener
                        statusRef.addValueEventListener(listener)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            tuyaRef.addValueEventListener(tuyaRoomListener!!)
        }"""

content = content.replace(logic_to_replace, new_logic)

with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'w') as f:
    f.write(content)

