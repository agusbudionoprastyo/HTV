import re

with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'r') as f:
    content = f.read()

# Add sync fields if not already there
if 'private var syncTimestampListener: ValueEventListener? = null' not in content:
    content = content.replace(
        "private var branchLatLngListener: ValueEventListener? = null", 
        "private var branchLatLngListener: ValueEventListener? = null\n    private var syncTimestampListener: ValueEventListener? = null\n    private var activeSyncTimestampRef: DatabaseReference? = null\n"
    )

# Add to cleanup if not already there
if 'syncTimestampListener = null' not in content:
    cleanup_addition = """
        activeSyncTimestampRef?.let { ref ->
            syncTimestampListener?.let { ref.removeEventListener(it) }
        }
        syncTimestampListener = null
        activeSyncTimestampRef = null
"""
    content = content.replace(
        "activeBranchLatLngRef = null",
        "activeBranchLatLngRef = null" + cleanup_addition
    )

# Add to idempotency check
if 'syncTimestampListener != null' not in content:
    content = content.replace(
        "contactListener != null &&\n            branchLatLngListener != null",
        "contactListener != null &&\n            branchLatLngListener != null &&\n            syncTimestampListener != null"
    )

# List of targets to comment out their addValueEventListener
targets = [
    r"requestRef\.addValueEventListener\(requestListener!!\)",
    r"hotelFacRef\.addValueEventListener\(hotelFacilitiesListener!!\)",
    r"roomFacRef\.addValueEventListener\(roomFacilitiesListener!!\)",
    r"emergRef\.addValueEventListener\(emergencyProcedureListener!!\)",
    r"healthRef\.addValueEventListener\(healthAndWellnessListener!!\)",
    r"discoverRef\.addValueEventListener\(discoverDestinationListener!!\)",
    r"iconRef\.addValueEventListener\(companyIconListener!!\)",
    r"weatherRef\.addValueEventListener\(weatherSettingListener!!\)",
    r"fidsSettingRef\.addValueEventListener\(fidsSettingListener!!\)",
    r"contactRef\.addValueEventListener\(contactListener!!\)",
    r"branchLatLngRef\.addValueEventListener\(branchLatLngListener!!\)",
    r"branchNameRef\.addValueEventListener\(branchNameListener!!\)"
]

for t in targets:
    content = re.sub(t, f"// Removed to use Sync Trigger: {t}", content)

# At the end of startPreload, insert the sync listener setup.
# We'll look for the end of the startPreload function.
# The last listener setup in startPreload is branchNameRef.addValueEventListener(branchNameListener!!)
# Wait, actually flightInfoListener and dndListener are setup dynamically, but startPreload calls setupWeatherListeners etc.
# The end of startPreload is around line 630.
# Let's find: `// Removed to use Sync Trigger: branchNameRef\.addValueEventListener\(branchNameListener!!\)`
# Then insert the sync listener after it.

sync_logic = """
        // SETUP SYNC TRIGGER FOR STATIC DATA
        val syncRef = db.child("BRANCHES").child(branchId).child("SETTING").child("last_sync_timestamp")
        activeSyncTimestampRef = syncRef
        syncTimestampListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DataRepository", "Sync timestamp changed or initial load. Fetching static data...")
                
                activeRequestRef?.addListenerForSingleValueEvent(requestListener!!)
                activeHotelFacilitiesRef?.addListenerForSingleValueEvent(hotelFacilitiesListener!!)
                activeRoomFacilitiesRef?.addListenerForSingleValueEvent(roomFacilitiesListener!!)
                activeEmergencyProcedureRef?.addListenerForSingleValueEvent(emergencyProcedureListener!!)
                activeHealthAndWellnessRef?.addListenerForSingleValueEvent(healthAndWellnessListener!!)
                activeDiscoverDestinationRef?.addListenerForSingleValueEvent(discoverDestinationListener!!)
                
                activeCompanyIconRef?.addListenerForSingleValueEvent(companyIconListener!!)
                activeWeatherSettingRef?.addListenerForSingleValueEvent(weatherSettingListener!!)
                activeFidsSettingRef?.addListenerForSingleValueEvent(fidsSettingListener!!)
                
                activeContactRef?.addListenerForSingleValueEvent(contactListener!!)
                activeBranchLatLngRef?.addListenerForSingleValueEvent(branchLatLngListener!!)
                activeBranchNameRef?.addListenerForSingleValueEvent(branchNameListener!!)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Sync listener cancelled: ${error.message}")
            }
        }
        syncRef.addValueEventListener(syncTimestampListener!!)
"""

content = re.sub(
    r"(// Removed to use Sync Trigger: branchNameRef\.addValueEventListener\(branchNameListener!!\))",
    r"\1\n" + sync_logic,
    content
)

with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'w') as f:
    f.write(content)
