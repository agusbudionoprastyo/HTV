import re

with open('app/src/main/java/com/dafamsemarang/dhtv/DataRepository.kt', 'r') as f:
    content = f.read()

# Add syncTimestamp fields
new_fields = """
    private var syncTimestampListener: ValueEventListener? = null
    private var activeSyncTimestampRef: DatabaseReference? = null
"""
content = content.replace("private var branchLatLngListener: ValueEventListener? = null", 
                          "private var branchLatLngListener: ValueEventListener? = null\n" + new_fields)

# Modify cleanup
cleanup_addition = """
        activeSyncTimestampRef?.let { ref ->
            syncTimestampListener?.let { ref.removeEventListener(it) }
        }
        syncTimestampListener = null
        activeSyncTimestampRef = null
"""
content = content.replace("activeBranchLatLngRef = null", 
                          "activeBranchLatLngRef = null\n" + cleanup_addition)

# Define the targets to move
targets = [
    ("requestRef", "requestListener"),
    ("hotelFacRef", "hotelFacilitiesListener"),
    ("roomFacRef", "roomFacilitiesListener"),
    ("emergRef", "emergencyProcedureListener"),
    ("healthRef", "healthAndWellnessListener"),
    ("discoverRef", "discoverDestinationListener"),
    ("iconRef", "companyIconListener"),
    ("weatherRef", "weatherSettingListener"),
    ("fidsSettingRef", "fidsSettingListener"),
    ("contactRef", "contactListener"),
    ("branchLatLngRef", "branchLatLngListener"),
    ("branchNameRef", "branchNameListener")
]

# We need to replace .addValueEventListener with .addListenerForSingleValueEvent for these targets
for ref_name, listener_name in targets:
    pattern = rf"{ref_name}\.addValueEventListener\({listener_name}!!\)"
    replacement = rf"{ref_name}.addListenerForSingleValueEvent({listener_name}!!)"
    content = re.sub(pattern, replacement, content)

# Extract the static fetching block from startPreload
# It's tricky to regex parse all of them because they are spread out.
# Let's just create a block that does the fetching and insert it.
# Actually, if we just leave the instantiation of the listeners where they are, 
# and wrap them inside a sync listener, that's easier.
# But they depend on context, db, branchId which are available in startPreload.

# Let's manually write the patch since it's large and complex.
