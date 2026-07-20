import re

with open('app/src/main/java/com/dafamsemarang/dhtv/FooterSection.kt', 'r') as f:
    content = f.read()

target = """    val tuyaSwitch1Id by DataRepository.tuyaSwitch1Id
    val tuyaSwitch2Id by DataRepository.tuyaSwitch2Id
    val tuyaSwitch3Id by DataRepository.tuyaSwitch3Id
    val tuyaAcId by DataRepository.tuyaAcId
    val tuyaCurtainId by DataRepository.tuyaCurtainId
    val hasSmartRoom = !tuyaSwitch1Id.isNullOrEmpty() || 
                       !tuyaSwitch2Id.isNullOrEmpty() || 
                       !tuyaSwitch3Id.isNullOrEmpty() || 
                       !tuyaAcId.isNullOrEmpty() || 
                       !tuyaCurtainId.isNullOrEmpty()"""

replacement = """    val smartDevices by DataRepository.smartDevicesList
    val hasSmartRoom = smartDevices.isNotEmpty()"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/dafamsemarang/dhtv/FooterSection.kt', 'w') as f:
    f.write(content)
