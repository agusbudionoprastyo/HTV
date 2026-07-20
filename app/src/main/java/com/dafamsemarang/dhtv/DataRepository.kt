package com.dafamsemarang.dhtv
 
import android.util.Log
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.database.*
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
 
object DataRepository {
    // Live data holders (Compose mutableState for reactivity)
    val menuItems = mutableStateOf<List<MenuItemData>>(emptyList())
    val requestItems = mutableStateOf<List<GuestRequest>>(emptyList())
 
    // Hotel Info Live data holders
    val hotelFacilities = mutableStateOf<List<Item>>(emptyList())
    val roomFacilities = mutableStateOf<List<Item>>(emptyList())
    val emergencyProcedure = mutableStateOf<List<Item>>(emptyList())
    val healthAndWellness = mutableStateOf<List<Item>>(emptyList())
    val discoverDestination = mutableStateOf<List<Item>>(emptyList())
 
    // Slideshow & Video Live data holders
    val slideshowImages = mutableStateOf<List<String>>(emptyList())
    val slideshowTitles = mutableStateOf<List<String>>(emptyList())
    val slideshowDurations = mutableStateOf<List<Int>>(emptyList())
    val slideshowTypes = mutableStateOf<List<String>>(emptyList())
    val isSlideshowActive = mutableStateOf(false)
    val isLoadingSlideshow = mutableStateOf(true)
    val currentImageIndex = mutableStateOf(0)
    
    val videoUrls = mutableStateOf<List<String>>(emptyList())
    val isLoadingVideos = mutableStateOf(true)
 
    // Loading state flags
    val isMenuLoaded = mutableStateOf(false)
    val isRequestLoaded = mutableStateOf(false)
    val isHotelFacilitiesLoaded = mutableStateOf(false)
    val isRoomFacilitiesLoaded = mutableStateOf(false)
    val isEmergencyProcedureLoaded = mutableStateOf(false)
    val isHealthWellnessLoaded = mutableStateOf(false)
    val isDiscoverDestinationLoaded = mutableStateOf(false)

    // NEW Persistent States
    // Weather
    val configuredCity = mutableStateOf<String?>(null)
    val liveWeather = mutableStateOf<LiveWeather?>(null)
    val forecastData = mutableStateOf<ForecastData?>(null)
    val companyIconUrl = mutableStateOf<String?>(null)
    val globalHotelImageUrl = mutableStateOf<String?>(null)

    // Flight / FIDS
    val fidsActive = mutableStateOf(true)
    val fidsIcaoCode = mutableStateOf("WARS")
    val flightArrivals = mutableStateOf<List<Flight>>(emptyList())
    val flightDepartures = mutableStateOf<List<Flight>>(emptyList())
    val flightAirportName = mutableStateOf("Ahmad Yani Airport")

    // Smart Home (Tuya)
    val tuyaDeviceId = mutableStateOf<String?>(null)
    val tuyaDeviceName = mutableStateOf<String?>("Smart Switch")
    val tuyaSwitch1State = mutableStateOf(false)
    val tuyaSwitch2State = mutableStateOf(false)
    val tuyaSwitch1Name = mutableStateOf<String?>("")
    val tuyaSwitch2Name = mutableStateOf<String?>("")

    // Guest & DND
    val guestInfo = mutableStateOf<GuestInfo?>(null)
    val isDndActive = mutableStateOf(false)
    private var currentDndFolioId: Int? = null  // Track which folioId the DND listener is on
    val instagramHandle = mutableStateOf<String?>(null)
    val facebookHandle = mutableStateOf<String?>(null)
    val tiktokHandle = mutableStateOf<String?>(null)
    val websiteUrl = mutableStateOf<String?>(null)
    val branchLatLng = mutableStateOf<String?>(null)  // Format: "lat,lng" from LONGLAT_BRANCH
    val branchName = mutableStateOf<String?>(null)

    // Subscription Status & Expiry Lock States
    val isAppLocked = mutableStateOf(false)
    val lockMessage = mutableStateOf("")

    private var branchNameListener: ValueEventListener? = null
    private var activeBranchNameRef: DatabaseReference? = null

    private var subStatusBranch: String? = null
    private var subStatusSetting: String? = null
    private var subExpiredBranch: Any? = null
    private var subExpiredSetting: Any? = null

    private var subscriptionStatusListener: ValueEventListener? = null
    private var subscriptionExpiredListener: ValueEventListener? = null
    private var subscriptionSettingStatusListener: ValueEventListener? = null
    private var subscriptionSettingExpiredListener: ValueEventListener? = null

    private var activeSubscriptionStatusRef: DatabaseReference? = null
    private var activeSubscriptionExpiredRef: DatabaseReference? = null
    private var activeSubscriptionSettingStatusRef: DatabaseReference? = null
    private var activeSubscriptionSettingExpiredRef: DatabaseReference? = null

    private fun updateSubscriptionState() {
        val status = subStatusBranch ?: subStatusSetting ?: "active"
        val expired = subExpiredBranch ?: subExpiredSetting

        val isStatusActive = status == "active"
        val isExpired = try {
            if (expired == null) {
                false
            } else {
                when (expired) {
                    is Long -> System.currentTimeMillis() > expired
                    is String -> {
                        if (expired.trim().isEmpty()) {
                            false
                        } else {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val expiredDate = sdf.parse(expired)
                            if (expiredDate != null) {
                                val calCurrent = java.util.Calendar.getInstance()
                                calCurrent.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                calCurrent.set(java.util.Calendar.MINUTE, 0)
                                calCurrent.set(java.util.Calendar.SECOND, 0)
                                calCurrent.set(java.util.Calendar.MILLISECOND, 0)
                                
                                val calExpired = java.util.Calendar.getInstance()
                                calExpired.time = expiredDate
                                calExpired.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                calExpired.set(java.util.Calendar.MINUTE, 59)
                                calExpired.set(java.util.Calendar.SECOND, 59)
                                
                                calCurrent.after(calExpired)
                            } else {
                                false
                            }
                        }
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            false
        }

        if (!isStatusActive) {
            isAppLocked.value = true
            lockMessage.value = "Layanan Dinonaktifkan\nStatus Langganan Tidak Aktif"
        } else if (isExpired) {
            isAppLocked.value = true
            lockMessage.value = "Layanan Sudah Berakhir\nMasa Berlaku Langganan Telah Habis"
        } else {
            isAppLocked.value = false
            lockMessage.value = ""
        }
    }

    private var menuListener: ValueEventListener? = null
    private var requestListener: ValueEventListener? = null
    private var hotelFacilitiesListener: ValueEventListener? = null
    private var roomFacilitiesListener: ValueEventListener? = null
    private var emergencyProcedureListener: ValueEventListener? = null
    private var healthAndWellnessListener: ValueEventListener? = null
    private var discoverDestinationListener: ValueEventListener? = null
    private var slideshowListener: ValueEventListener? = null
    private var videoListener: ValueEventListener? = null

    // NEW Listeners
    private var companyIconListener: ValueEventListener? = null
    private var weatherSettingListener: ValueEventListener? = null
    private var liveWeatherListener: ValueEventListener? = null
    private var forecastListener: ValueEventListener? = null
    private var weatherSyncListener: ValueEventListener? = null
    private var fidsSettingListener: ValueEventListener? = null
    private var flightInfoListener: ValueEventListener? = null
    private var guestInfoListener: ValueEventListener? = null
    private var dndListener: ValueEventListener? = null
    private var contactListener: ValueEventListener? = null
    private var branchLatLngListener: ValueEventListener? = null
    private var syncBannerListener: ValueEventListener? = null
    private var activeSyncBannerRef: DatabaseReference? = null
    
    private var syncMenuListener: ValueEventListener? = null
    private var activeSyncMenuRef: DatabaseReference? = null
    
    private var syncHotelInfoListener: ValueEventListener? = null
    private var activeSyncHotelInfoRef: DatabaseReference? = null
    
    private var syncRequestListener: ValueEventListener? = null
    private var activeSyncRequestRef: DatabaseReference? = null
    
    
    private var tuyaStatusListener: ValueEventListener? = null
    private var activeTuyaStatusRef: DatabaseReference? = null

 
    private var activeBranchId: String? = null
    private var activeMenuRef: DatabaseReference? = null
    private var activeRequestRef: DatabaseReference? = null
    private var activeHotelFacilitiesRef: DatabaseReference? = null
    private var activeRoomFacilitiesRef: DatabaseReference? = null
    private var activeEmergencyProcedureRef: DatabaseReference? = null
    private var activeHealthAndWellnessRef: DatabaseReference? = null
    private var activeDiscoverDestinationRef: DatabaseReference? = null
    private var activeSlideshowRef: DatabaseReference? = null
    private var activeVideoRef: DatabaseReference? = null

    // NEW Refs
    private var activeCompanyIconRef: DatabaseReference? = null
    private var activeWeatherSettingRef: DatabaseReference? = null
    private var activeLiveWeatherRef: DatabaseReference? = null
    private var activeForecastRef: DatabaseReference? = null
    private var activeWeatherSyncRef: DatabaseReference? = null
    private var activeFidsSettingRef: DatabaseReference? = null
    private var activeFlightInfoRef: DatabaseReference? = null
    private var activeGuestInfoRef: DatabaseReference? = null
    private var activeDndRef: DatabaseReference? = null
    private var activeContactRef: DatabaseReference? = null
    private var activeBranchLatLngRef: DatabaseReference? = null
    private var activeTuyaRoomRef: DatabaseReference? = null
    private var tuyaRoomListener: ValueEventListener? = null
 
 
    fun startPreload(context: android.content.Context, branchId: String?) {
        if (branchId == null) return
        
        // Idempotency check: if already preloading this branch, do nothing
        if (branchId == activeBranchId && 
            menuListener != null && 
            requestListener != null && 
            slideshowListener != null && 
            videoListener != null &&
            companyIconListener != null &&
            weatherSettingListener != null &&
            fidsSettingListener != null &&
            guestInfoListener != null &&
            hotelFacilitiesListener != null &&
            roomFacilitiesListener != null &&
            emergencyProcedureListener != null &&
            healthAndWellnessListener != null &&
            discoverDestinationListener != null &&
            contactListener != null &&
            branchLatLngListener != null &&
            syncBannerListener != null &&
            syncMenuListener != null &&
            syncHotelInfoListener != null &&
            syncRequestListener != null

        ) {
            Log.d("DataRepository", "Preload already active for branch: $branchId")
            return
        }
 
        Log.d("DataRepository", "Starting/restarting preload for branch: $branchId")
        cleanup()
 
        activeBranchId = branchId
        val db = FirebaseDatabase.getInstance().reference

        // Fetch Room for Tuya
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
        }
        
        // Preload Menu items
        val menuRef = db.child("BRANCHES").child(branchId).child("FOOD_BEVERAGE").child("food")
        activeMenuRef = menuRef
        menuListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val parsedItem = child.getValue(MenuItemData::class.java)?.copy(branchId = branchId)
                    // Ensure the isActive flag is correctly mapped from the raw data
                    parsedItem?.copy(isActive = child.child("isActive").getValue(Boolean::class.java) ?: true)
                }
                menuItems.value = items
                isMenuLoaded.value = true
                Log.d("DataRepository", "Menu items loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Menu preload cancelled: ${error.message}")
                isMenuLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: menuRef.addValueEventListener(menuListener!!)
 
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
                isRequestLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: requestRef\.addValueEventListener\(requestListener!!\)
 
        // Preload Hotel Info - Hotel Facility
        val hotelFacRef = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("HOTEL_FACILITY")
        activeHotelFacilitiesRef = hotelFacRef
        hotelFacilitiesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                hotelFacilities.value = items
                isHotelFacilitiesLoaded.value = true
                Log.d("DataRepository", "Hotel facilities loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                isHotelFacilitiesLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: hotelFacRef\.addValueEventListener\(hotelFacilitiesListener!!\)

        // Preload Hotel Info - Rooms Facility
        val roomFacRef = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("ROOMS_FACILITY")
        activeRoomFacilitiesRef = roomFacRef
        roomFacilitiesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                roomFacilities.value = items
                isRoomFacilitiesLoaded.value = true
                Log.d("DataRepository", "Room facilities loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                isRoomFacilitiesLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: roomFacRef\.addValueEventListener\(roomFacilitiesListener!!\)

        // Preload Hotel Info - Emergency Procedure
        val emergRef = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("EMERGENCY_PROCEDURE")
        activeEmergencyProcedureRef = emergRef
        emergencyProcedureListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                emergencyProcedure.value = items
                isEmergencyProcedureLoaded.value = true
                Log.d("DataRepository", "Emergency procedures loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                isEmergencyProcedureLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: emergRef\.addValueEventListener\(emergencyProcedureListener!!\)

        // Preload Hotel Info - Health & Wellness
        val healthRef = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("HEALTH_WELLNESS")
        activeHealthAndWellnessRef = healthRef
        healthAndWellnessListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                healthAndWellness.value = items
                isHealthWellnessLoaded.value = true
                Log.d("DataRepository", "Health & Wellness loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                isHealthWellnessLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: healthRef\.addValueEventListener\(healthAndWellnessListener!!\)

        // Preload Hotel Info - Discover Destination
        val discoverRef = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("DISCOVER_DESTINATION")
        activeDiscoverDestinationRef = discoverRef
        discoverDestinationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                discoverDestination.value = items
                isDiscoverDestinationLoaded.value = true
                Log.d("DataRepository", "Discover destinations loaded: ${items.size}")
            }
            override fun onCancelled(error: DatabaseError) {
                isDiscoverDestinationLoaded.value = true
            }
        }
        // Removed to use Sync Trigger: discoverRef\.addValueEventListener\(discoverDestinationListener!!\)
 
        // Preload Banners (from BANNER node)
        val slideshowRef = db.child("BRANCHES").child(branchId).child("BANNER")
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
                            val type = slideSnapshot.child("type").getValue(String::class.java) ?: "image"
                            if (url != null && status == "active") BannerSlideData(url, duration, title, type) else null
                        } catch (e: Exception) { null }
                    }
                    if (activeSlides.isNotEmpty()) {
                        isSlideshowActive.value = true
                        slideshowImages.value = activeSlides.map { it.url }
                        slideshowDurations.value = activeSlides.map { it.duration }
                        slideshowTitles.value = activeSlides.map { it.title ?: "" }
                        slideshowTypes.value = activeSlides.map { it.type }
                        
                        // If there are video banners, extract their URLs and trigger preload/caching
                        val videoUrlsList = activeSlides.filter { it.type == "video" }.map { it.url }
                        if (videoUrlsList.isNotEmpty()) {
                            preloadVideos(context, videoUrlsList)
                        }
                    } else {
                        isSlideshowActive.value = false
                        slideshowImages.value = emptyList()
                        slideshowDurations.value = emptyList()
                        slideshowTitles.value = emptyList()
                        slideshowTypes.value = emptyList()
                    }
                    isLoadingSlideshow.value = false
                    Log.d("DataRepository", "Banners preloaded successfully: ${slideshowImages.value.size} active banners")
                } catch (e: Exception) {
                    isLoadingSlideshow.value = false
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Banner preload cancelled: ${error.message}")
                isLoadingSlideshow.value = false
            }
        }
        // Removed to use Sync Trigger: slideshowRef.addValueEventListener(slideshowListener!!)
 
        // Old VIDEO node disabled/bypassed as per banner migration
        videoUrls.value = emptyList()
        isLoadingVideos.value = false

        // Preload Company Icon
        val iconRef = db.child("BRANCHES").child(branchId).child("SETTING").child("COMPANY_ICON")
        activeCompanyIconRef = iconRef
        companyIconListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                companyIconUrl.value = snapshot.child("iconUrl").getValue(String::class.java)
                Log.d("DataRepository", "Company icon loaded: ${companyIconUrl.value}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Company icon preload failed: ${error.message}")
            }
        }
        // Removed to use Sync Trigger: iconRef\.addValueEventListener\(companyIconListener!!\)

        // Preload Weather config
        val weatherRef = db.child("BRANCHES").child(branchId).child("SETTING").child("WEATHER")
        activeWeatherSettingRef = weatherRef
        weatherSettingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val city = snapshot.child("CITY").getValue(String::class.java)
                    ?: snapshot.child("city").getValue(String::class.java)
                configuredCity.value = city
                Log.d("DataRepository", "Weather config loaded city: $city")
                setupWeatherListeners(db, city)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Weather setting preload failed: ${error.message}")
            }
        }
        weatherRef.addValueEventListener(weatherSettingListener!!)

        // Preload FIDS Settings
        val fidsSettingRef = db.child("BRANCHES").child(branchId).child("SETTING").child("FIDS")
        activeFidsSettingRef = fidsSettingRef
        fidsSettingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val activeVal = snapshot.child("ACTIVE").value
                    val active = when (activeVal) {
                        is Boolean -> activeVal
                        is String -> activeVal.toBoolean()
                        else -> true
                    }
                    val icaoVal = snapshot.child("ICAO_CODE").getValue(String::class.java)
                    
                    fidsActive.value = active
                    fidsIcaoCode.value = icaoVal ?: "WARS"
                    Log.d("DataRepository", "FIDS Config loaded: ACTIVE=$active, ICAO_CODE=$icaoVal")
                } else {
                    fidsActive.value = true
                    fidsIcaoCode.value = "WARS"
                    Log.d("DataRepository", "FIDS Config path not found, using defaults")
                }
                setupFlightInfoListener(db, fidsIcaoCode.value, fidsActive.value)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "FIDS Config listener cancelled: ${error.message}")
            }
        }
        fidsSettingRef.addValueEventListener(fidsSettingListener!!)

        // Preload Guest Info
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val roomId = sharedPrefs.getString("room", null)
        if (roomId != null) {
            val guestPath = "BRANCHES/$branchId/FOGUEST/$roomId"
            val guestRef = db.child(guestPath)
            activeGuestInfoRef = guestRef
            guestInfoListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val info = GuestInfo(
                            folio = snapshot.child("folio").getValue(Int::class.java) ?: 0,
                            dateci = snapshot.child("dateci").getValue(String::class.java) ?: "",
                            dateco = snapshot.child("dateco").getValue(String::class.java) ?: "",
                            datecreate = snapshot.child("datecreate").getValue(String::class.java) ?: "",
                            fname = snapshot.child("fname").getValue(String::class.java) ?: "",
                            foliostatus = snapshot.child("foliostatus").getValue(String::class.java) ?: "",
                            email = snapshot.child("email").getValue(String::class.java) ?: "",
                            phone = snapshot.child("phone").getValue(String::class.java) ?: "",
                            room = snapshot.child("room").getValue(String::class.java) ?: "",
                            roomnight = snapshot.child("roomnight").getValue(Int::class.java) ?: 0,
                            roomtype = snapshot.child("roomtype").getValue(String::class.java) ?: "",
                            guestImageUrl = snapshot.child("guestImageUrl").getValue(String::class.java) ?: "",
                            isSmoking = snapshot.child("isSmoking").getValue(Boolean::class.java) == true,
                            gender = snapshot.child("gender").getValue(String::class.java) ?: ""
                        )
                        guestInfo.value = info
                        Log.d("DataRepository", "Guest info loaded: fname=${info.fname}, gender=${info.gender}, Folio: ${info.folio}")
                        // Only re-setup DND listener if folioId actually changed,
                        // prevents DND from resetting when other guest fields are updated
                        val newFolio = if (info.folio != 0) info.folio else null
                        if (newFolio != currentDndFolioId) {
                            currentDndFolioId = newFolio
                            setupDndListener(db, branchId, newFolio)
                        }
                    } else {
                        guestInfo.value = null
                        if (currentDndFolioId != null) {
                            currentDndFolioId = null
                            setupDndListener(db, branchId, null)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("DataRepository", "Guest info listener cancelled: ${error.message}")
                    guestInfo.value = null
                    setupDndListener(db, branchId, null)
                }
            }
            guestRef.addValueEventListener(guestInfoListener!!)
        }

        // Preload Instagram config from SETTING/CONTACT
        val contactRef = db.child("BRANCHES").child(branchId).child("SETTING").child("CONTACT")
        activeContactRef = contactRef
        contactListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                instagramHandle.value = snapshot.child("INSTAGRAM").getValue(String::class.java) ?: snapshot.child("instagram").getValue(String::class.java)
                facebookHandle.value = snapshot.child("FACEBOOK").getValue(String::class.java) ?: snapshot.child("facebook").getValue(String::class.java)
                tiktokHandle.value = snapshot.child("TIKTOK").getValue(String::class.java) ?: snapshot.child("tiktok").getValue(String::class.java)
                websiteUrl.value = snapshot.child("WEBSITE").getValue(String::class.java) ?: snapshot.child("website").getValue(String::class.java) ?: snapshot.child("WEB").getValue(String::class.java) ?: snapshot.child("web").getValue(String::class.java)
                Log.d("DataRepository", "Social handles loaded: IG=${instagramHandle.value}, FB=${facebookHandle.value}, TT=${tiktokHandle.value}, WEB=${websiteUrl.value}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Contact preload failed: ${error.message}")
            }
        }
        // Removed to use Sync Trigger: contactRef\.addValueEventListener\(contactListener!!\)

        // Preload hotel coordinates from BRANCHES/{branchId}/LONGLAT_BRANCH
        val branchLatLngRef = db.child("BRANCHES").child(branchId).child("LONGLAT_BRANCH")
        activeBranchLatLngRef = branchLatLngRef
        branchLatLngListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                branchLatLng.value = snapshot.getValue(String::class.java)
                Log.d("DataRepository", "Branch LatLng loaded: ${branchLatLng.value}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "BranchLatLng preload failed: ${error.message}")
            }
        }
        // Removed to use Sync Trigger: branchLatLngRef\.addValueEventListener\(branchLatLngListener!!\)

        // Preload Subscription Config from BRANCHES/{branchId}/SUBSCRIPTION_STATUS & EXPIRED_DATE
        val subStatusBranchRef = db.child("BRANCHES").child(branchId).child("SUBSCRIPTION_STATUS")
        activeSubscriptionStatusRef = subStatusBranchRef
        subscriptionStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subStatusBranch = snapshot.getValue(String::class.java)
                updateSubscriptionState()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "subStatusBranch preload failed: ${error.message}")
            }
        }
        subStatusBranchRef.addValueEventListener(subscriptionStatusListener!!)

        val subExpiredBranchRef = db.child("BRANCHES").child(branchId).child("EXPIRED_DATE")
        activeSubscriptionExpiredRef = subExpiredBranchRef
        subscriptionExpiredListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subExpiredBranch = snapshot.value
                updateSubscriptionState()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "subExpiredBranch preload failed: ${error.message}")
            }
        }
        subExpiredBranchRef.addValueEventListener(subscriptionExpiredListener!!)

        // Preload Subscription Config from BRANCHES/{branchId}/SETTING/SUBSCRIPTION_STATUS & EXPIRED_DATE
        val subStatusSettingRef = db.child("BRANCHES").child(branchId).child("SETTING").child("SUBSCRIPTION_STATUS")
        activeSubscriptionSettingStatusRef = subStatusSettingRef
        subscriptionSettingStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subStatusSetting = snapshot.getValue(String::class.java)
                updateSubscriptionState()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "subStatusSetting preload failed: ${error.message}")
            }
        }
        subStatusSettingRef.addValueEventListener(subscriptionSettingStatusListener!!)

        val subExpiredSettingRef = db.child("BRANCHES").child(branchId).child("SETTING").child("EXPIRED_DATE")
        activeSubscriptionSettingExpiredRef = subExpiredSettingRef
        subscriptionSettingExpiredListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subExpiredSetting = snapshot.value
                updateSubscriptionState()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "subExpiredSetting preload failed: ${error.message}")
            }
        }
        subExpiredSettingRef.addValueEventListener(subscriptionSettingExpiredListener!!)

        // Preload Branch Name
        val branchNameRef = db.child("BRANCHES").child(branchId).child("BRANCH_NAME")
        activeBranchNameRef = branchNameRef
        branchNameListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    branchName.value = snapshot.getValue(String::class.java)
                } else {
                    // Fallback to name, NAME or branchId
                    db.child("BRANCHES").child(branchId).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(nameSnapshot: DataSnapshot) {
                            if (nameSnapshot.exists()) {
                                branchName.value = nameSnapshot.getValue(String::class.java)
                            } else {
                                db.child("BRANCHES").child(branchId).child("NAME").addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(nameSnapshot2: DataSnapshot) {
                                        branchName.value = nameSnapshot2.getValue(String::class.java) ?: branchId
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "BranchName preload failed: ${error.message}")
            }
        }
        // Removed to use Sync Trigger: branchNameRef.addValueEventListener(branchNameListener!!)
        
        // 1. BANNER SYNC TRIGGER
        val syncBannerRef = db.child("BRANCHES").child(branchId).child("SETTING").child("last_sync_banner")
        activeSyncBannerRef = syncBannerRef
        syncBannerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DataRepository", "Banner Sync timestamp changed. Fetching banners...")
                activeSlideshowRef?.addListenerForSingleValueEvent(slideshowListener!!)
                activeCompanyIconRef?.addListenerForSingleValueEvent(companyIconListener!!)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Banner Sync listener cancelled: ${error.message}")
            }
        }
        syncBannerRef.addValueEventListener(syncBannerListener!!)

        // 2. MENU F&B SYNC TRIGGER
        val syncMenuRef = db.child("BRANCHES").child(branchId).child("SETTING").child("last_sync_menu_fnb")
        activeSyncMenuRef = syncMenuRef
        syncMenuListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DataRepository", "Menu Sync timestamp changed. Fetching menus...")
                activeMenuRef?.addListenerForSingleValueEvent(menuListener!!)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Menu Sync listener cancelled: ${error.message}")
            }
        }
        syncMenuRef.addValueEventListener(syncMenuListener!!)

        // 3. HOTEL INFO SYNC TRIGGER
        val syncHotelInfoRef = db.child("BRANCHES").child(branchId).child("SETTING").child("last_sync_hotel_info")
        activeSyncHotelInfoRef = syncHotelInfoRef
        syncHotelInfoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DataRepository", "Hotel Info Sync timestamp changed. Fetching hotel info...")
                activeHotelFacilitiesRef?.addListenerForSingleValueEvent(hotelFacilitiesListener!!)
                activeRoomFacilitiesRef?.addListenerForSingleValueEvent(roomFacilitiesListener!!)
                activeEmergencyProcedureRef?.addListenerForSingleValueEvent(emergencyProcedureListener!!)
                activeHealthAndWellnessRef?.addListenerForSingleValueEvent(healthAndWellnessListener!!)
                activeDiscoverDestinationRef?.addListenerForSingleValueEvent(discoverDestinationListener!!)
                activeBranchLatLngRef?.addListenerForSingleValueEvent(branchLatLngListener!!)
                activeBranchNameRef?.addListenerForSingleValueEvent(branchNameListener!!)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Hotel Info Sync listener cancelled: ${error.message}")
            }
        }
        syncHotelInfoRef.addValueEventListener(syncHotelInfoListener!!)

        // 4. REQUEST SERVICE SYNC TRIGGER
        val syncRequestRef = db.child("BRANCHES").child(branchId).child("SETTING").child("last_sync_request_service")
        activeSyncRequestRef = syncRequestRef
        syncRequestListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DataRepository", "Request Sync timestamp changed. Fetching requests...")
                activeRequestRef?.addListenerForSingleValueEvent(requestListener!!)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Request Sync listener cancelled: ${error.message}")
            }
        }
        syncRequestRef.addValueEventListener(syncRequestListener!!)
    }

    private fun setupWeatherListeners(db: DatabaseReference, city: String?) {
        activeWeatherSyncRef?.let { ref -> weatherSyncListener?.let { ref.removeEventListener(it) } }
        weatherSyncListener = null
        activeWeatherSyncRef = null
 
        if (city.isNullOrEmpty()) {
            liveWeather.value = null
            forecastData.value = null
            return
        }
 
        val liveRef = db.child("weather").child("liveWeather")
        val forecastRef = db.child("weather").child("forecast")
        val weatherSync = db.child("weather").child("last_updated")

        activeWeatherSyncRef = weatherSync
        weatherSyncListener = object : ValueEventListener {
            override fun onDataChange(syncSnapshot: DataSnapshot) {
                Log.d("DataRepository", "Weather Sync trigger changed, fetching live and forecast data...")
                liveRef.get().addOnSuccessListener { snapshot ->
                    try {
                        val matchingNode = if (snapshot.child(city).exists()) {
                            snapshot.child(city)
                        } else {
                            snapshot.children.find { 
                                it.child("city").getValue(String::class.java)?.equals(city, ignoreCase = true) == true 
                            }
                        }
                        if (matchingNode != null) {
                            liveWeather.value = FirebaseWeatherData.parseLiveWeather(matchingNode)
                            Log.d("DataRepository", "Parsed live weather for $city")
                        } else {
                            if (snapshot.child("city").getValue(String::class.java)?.equals(city, ignoreCase = true) == true) {
                                liveWeather.value = FirebaseWeatherData.parseLiveWeather(snapshot)
                            }
                        }
                    } catch (e: Exception) { }
                }

                forecastRef.get().addOnSuccessListener { snapshot ->
                    try {
                        val matchingNode = if (snapshot.child(city).exists()) {
                            snapshot.child(city)
                        } else {
                            snapshot.children.find { 
                                it.child("city").getValue(String::class.java)?.equals(city, ignoreCase = true) == true 
                            }
                        }
                        if (matchingNode != null) {
                            forecastData.value = FirebaseWeatherData.parseForecastData(matchingNode)
                            Log.d("DataRepository", "Parsed forecast data for $city")
                        } else {
                            if (snapshot.child("city").getValue(String::class.java)?.equals(city, ignoreCase = true) == true) {
                                forecastData.value = FirebaseWeatherData.parseForecastData(snapshot)
                            }
                        }
                    } catch (e: Exception) { }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        weatherSync.addValueEventListener(weatherSyncListener!!)
    }

    private fun setupFlightInfoListener(db: DatabaseReference, icaoCode: String, active: Boolean) {
        activeFlightInfoRef?.let { ref -> flightInfoListener?.let { ref.removeEventListener(it) } }
        flightInfoListener = null
        activeFlightInfoRef = null
 
        if (!active) {
            flightArrivals.value = emptyList()
            flightDepartures.value = emptyList()
            return
        }
 
        val upperIcao = icaoCode.uppercase(Locale.US)
        var name = "$upperIcao Airport"
        db.child("FlightInfo").child("config").child("Airports").get().addOnSuccessListener { configSnap ->
            for (airportConfig in configSnap.children) {
                val icao = airportConfig.child("ICAO_Code").getValue(String::class.java)
                if (icao != null && icao.equals(upperIcao, ignoreCase = true)) {
                    val fetchedName = airportConfig.child("airpotName").getValue(String::class.java)
                    if (fetchedName != null) {
                        name = fetchedName
                        flightAirportName.value = name
                    }
                    break
                }
            }
        }
        flightAirportName.value = name
 
        // 2. Real-time listener ONLY for the specific airport to save bandwidth
        val flightRef = db.child("FlightInfo").child(upperIcao)
        activeFlightInfoRef = flightRef
        flightInfoListener = object : ValueEventListener {
            override fun onDataChange(airportSnapshot: DataSnapshot) {
                try {
                    // Parse Arrivals
                    val arrivalsList = mutableListOf<Flight>()
                    val arrivalsSnap = airportSnapshot.child("arrivals")
                    val currentTimeMillis = System.currentTimeMillis()
                    val filterThreshold = currentTimeMillis - (15 * 60 * 1000L) // 15-minute buffer
                    
                    for (flightSnap in arrivalsSnap.children) {
                        try {
                            val scheduled = flightSnap.child("scheduledTime").getValue(String::class.java) ?: ""
                            val revised = flightSnap.child("revisedTime").getValue(String::class.java) ?: ""
                            val timeStr = revised.ifEmpty { scheduled }
                            
                            val flightDate = parseUtcToWib(timeStr)
                            if (flightDate != null && isSameDayInWib(flightDate) && flightDate.time >= filterThreshold) {
                                val f = Flight(
                                    flightNumber = flightSnap.child("flightNumber").getValue(String::class.java) ?: "",
                                    airline = flightSnap.child("airline").getValue(String::class.java) ?: "",
                                    otherAirport = flightSnap.child("otherAirport").getValue(String::class.java) ?: "",
                                    scheduledTime = scheduled,
                                    revisedTime = revised,
                                    status = flightSnap.child("status").getValue(String::class.java) ?: "",
                                    direction = "arrival",
                                    gate = flightSnap.child("gate").getValue(String::class.java) ?: "",
                                    terminal = flightSnap.child("terminal").getValue(String::class.java) ?: ""
                                )
                                arrivalsList.add(f)
                            }
                        } catch (e: Exception) {}
                    }
                    arrivalsList.sortBy { 
                         val timeStr = it.revisedTime.ifEmpty { it.scheduledTime }
                         parseUtcToWib(timeStr)?.time ?: Long.MAX_VALUE
                    }
                    flightArrivals.value = arrivalsList
 
                    // Parse Departures
                    val departuresList = mutableListOf<Flight>()
                    val departuresSnap = airportSnapshot.child("departures")
                    for (flightSnap in departuresSnap.children) {
                        try {
                            val scheduled = flightSnap.child("scheduledTime").getValue(String::class.java) ?: ""
                            val revised = flightSnap.child("revisedTime").getValue(String::class.java) ?: ""
                            val timeStr = revised.ifEmpty { scheduled }
                            
                            val flightDate = parseUtcToWib(timeStr)
                            if (flightDate != null && isSameDayInWib(flightDate) && flightDate.time >= filterThreshold) {
                                val f = Flight(
                                    flightNumber = flightSnap.child("flightNumber").getValue(String::class.java) ?: "",
                                    airline = flightSnap.child("airline").getValue(String::class.java) ?: "",
                                    otherAirport = flightSnap.child("otherAirport").getValue(String::class.java) ?: "",
                                    scheduledTime = scheduled,
                                    revisedTime = revised,
                                    status = flightSnap.child("status").getValue(String::class.java) ?: "",
                                    direction = "departure",
                                    gate = flightSnap.child("gate").getValue(String::class.java) ?: "",
                                    terminal = flightSnap.child("terminal").getValue(String::class.java) ?: ""
                                )
                                departuresList.add(f)
                            }
                        } catch (e: Exception) {}
                    }
                    departuresList.sortBy { 
                        val timeStr = it.revisedTime.ifEmpty { it.scheduledTime }
                        parseUtcToWib(timeStr)?.time ?: Long.MAX_VALUE
                    }
                    flightDepartures.value = departuresList
 
                    Log.d("DataRepository", "FlightInfo parsed successfully: ${arrivalsList.size} arrivals, ${departuresList.size} departures")
                } catch (e: Exception) {
                    Log.e("DataRepository", "Error parsing FlightInfo: ${e.message}", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "FlightInfo listener cancelled: ${error.message}")
            }
        }
        flightRef.addValueEventListener(flightInfoListener!!)
    }

    private fun setupDndListener(db: DatabaseReference, branchId: String, folioId: Int?) {
        activeDndRef?.let { ref -> dndListener?.let { ref.removeEventListener(it) } }
        dndListener = null
        activeDndRef = null
 
        if (folioId == null) {
            isDndActive.value = false
            return
        }
 
        val dndRef = db.child("BRANCHES").child(branchId).child("DND_STATUS").child(folioId.toString())
        activeDndRef = dndRef
        dndListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isDndActive.value = snapshot.getValue(Boolean::class.java) == true
                Log.d("DataRepository", "DND Status updated: ${isDndActive.value}")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "DND listener cancelled: ${error.message}")
            }
        }
        dndRef.addValueEventListener(dndListener!!)
    }
 
    fun cleanup() {
        activeMenuRef?.let { ref -> menuListener?.let { ref.removeEventListener(it) } }
        activeRequestRef?.let { ref -> requestListener?.let { ref.removeEventListener(it) } }
        activeSlideshowRef?.let { ref -> slideshowListener?.let { ref.removeEventListener(it) } }
        activeVideoRef?.let { ref -> videoListener?.let { ref.removeEventListener(it) } }
        activeHotelFacilitiesRef?.let { ref -> hotelFacilitiesListener?.let { ref.removeEventListener(it) } }
        activeRoomFacilitiesRef?.let { ref -> roomFacilitiesListener?.let { ref.removeEventListener(it) } }
        activeEmergencyProcedureRef?.let { ref -> emergencyProcedureListener?.let { ref.removeEventListener(it) } }
        activeHealthAndWellnessRef?.let { ref -> healthAndWellnessListener?.let { ref.removeEventListener(it) } }
        activeDiscoverDestinationRef?.let { ref -> discoverDestinationListener?.let { ref.removeEventListener(it) } }

        activeCompanyIconRef?.let { ref -> companyIconListener?.let { ref.removeEventListener(it) } }
        activeWeatherSettingRef?.let { ref -> weatherSettingListener?.let { ref.removeEventListener(it) } }
        activeWeatherSyncRef?.let { ref -> weatherSyncListener?.let { ref.removeEventListener(it) } }
        weatherSyncListener = null
        activeWeatherSyncRef = null
        activeLiveWeatherRef?.let { ref -> liveWeatherListener?.let { ref.removeEventListener(it) } }
        activeForecastRef?.let { ref -> forecastListener?.let { ref.removeEventListener(it) } }
        activeFidsSettingRef?.let { ref -> fidsSettingListener?.let { ref.removeEventListener(it) } }
        activeFlightInfoRef?.let { ref -> flightInfoListener?.let { ref.removeEventListener(it) } }
        activeGuestInfoRef?.let { ref -> guestInfoListener?.let { ref.removeEventListener(it) } }
        activeDndRef?.let { ref -> dndListener?.let { ref.removeEventListener(it) } }
        activeContactRef?.let { ref -> contactListener?.let { ref.removeEventListener(it) } }
        activeSubscriptionStatusRef?.let { ref -> subscriptionStatusListener?.let { ref.removeEventListener(it) } }
        activeSubscriptionExpiredRef?.let { ref -> subscriptionExpiredListener?.let { ref.removeEventListener(it) } }
        activeSubscriptionSettingStatusRef?.let { ref -> subscriptionSettingStatusListener?.let { ref.removeEventListener(it) } }
        activeSubscriptionSettingExpiredRef?.let { ref -> subscriptionSettingExpiredListener?.let { ref.removeEventListener(it) } }
        activeBranchNameRef?.let { ref -> branchNameListener?.let { ref.removeEventListener(it) } }
 
        menuListener = null
        requestListener = null
        slideshowListener = null
        videoListener = null
        hotelFacilitiesListener = null
        roomFacilitiesListener = null
        emergencyProcedureListener = null
        healthAndWellnessListener = null
        discoverDestinationListener = null

        companyIconListener = null
        weatherSettingListener = null
        liveWeatherListener = null
        forecastListener = null
        fidsSettingListener = null
        flightInfoListener = null
        guestInfoListener = null
        dndListener = null
        contactListener = null
        branchLatLngListener = null
        subscriptionStatusListener = null
        subscriptionExpiredListener = null
        subscriptionSettingStatusListener = null
        subscriptionSettingExpiredListener = null
        branchNameListener = null

 
        activeMenuRef = null
        activeRequestRef = null
        activeSlideshowRef = null
        activeVideoRef = null
        activeHotelFacilitiesRef = null
        activeRoomFacilitiesRef = null
        activeEmergencyProcedureRef = null
        activeHealthAndWellnessRef = null
        activeDiscoverDestinationRef = null

        activeCompanyIconRef = null
        activeWeatherSettingRef = null
        activeLiveWeatherRef = null
        activeForecastRef = null
        activeFidsSettingRef = null
        activeFlightInfoRef = null
        activeGuestInfoRef = null
        activeDndRef = null
        activeContactRef = null
        activeBranchLatLngRef = null
        activeSyncBannerRef?.let { ref -> syncBannerListener?.let { ref.removeEventListener(it) } }
        syncBannerListener = null
        activeSyncBannerRef = null
        
        activeSyncMenuRef?.let { ref -> syncMenuListener?.let { ref.removeEventListener(it) } }
        syncMenuListener = null
        activeSyncMenuRef = null
        
        activeSyncHotelInfoRef?.let { ref -> syncHotelInfoListener?.let { ref.removeEventListener(it) } }
        syncHotelInfoListener = null
        activeSyncHotelInfoRef = null
        
        activeSyncRequestRef?.let { ref -> syncRequestListener?.let { ref.removeEventListener(it) } }
        syncRequestListener = null
        activeSyncRequestRef = null

        activeSubscriptionStatusRef = null
        activeSubscriptionExpiredRef = null
        activeSubscriptionSettingStatusRef = null
        activeSubscriptionSettingExpiredRef = null
        activeBranchNameRef = null

 
        activeBranchId = null
        currentDndFolioId = null
        isMenuLoaded.value = false
        isRequestLoaded.value = false
        isHotelFacilitiesLoaded.value = false
        isRoomFacilitiesLoaded.value = false
        isEmergencyProcedureLoaded.value = false
        isHealthWellnessLoaded.value = false
        isDiscoverDestinationLoaded.value = false

        hotelFacilities.value = emptyList()
        roomFacilities.value = emptyList()
        emergencyProcedure.value = emptyList()
        healthAndWellness.value = emptyList()
        discoverDestination.value = emptyList()
        isSlideshowActive.value = false
        isLoadingSlideshow.value = true
        isLoadingVideos.value = true
        slideshowImages.value = emptyList()
        slideshowTitles.value = emptyList()
        slideshowDurations.value = emptyList()
        slideshowTypes.value = emptyList()
        videoUrls.value = emptyList()
        currentImageIndex.value = 0

        configuredCity.value = null
        liveWeather.value = null
        forecastData.value = null
        companyIconUrl.value = null
 
        fidsActive.value = true
        fidsIcaoCode.value = "WARS"
        flightArrivals.value = emptyList()
        flightDepartures.value = emptyList()
        flightAirportName.value = "Ahmad Yani Airport"
 
        guestInfo.value = null
        isDndActive.value = false
        instagramHandle.value = null
        facebookHandle.value = null
        tiktokHandle.value = null
        websiteUrl.value = null
        branchLatLng.value = null
        branchName.value = null
    }

    private fun preloadVideos(context: android.content.Context, urls: List<String>) {
        val scope = CoroutineScope(Dispatchers.IO)
        for (url in urls) {
            scope.launch {
                try {
                    if (url.isEmpty()) return@launch
                    val cacheFileStandard = java.io.File(context.cacheDir, url.hashCode().toString() + ".mp4")
                    val cacheFileBanner = java.io.File(context.cacheDir, url.hashCode().toString() + "_banner.mp4")
                    
                    if (cacheFileStandard.exists() && cacheFileStandard.length() > 0 && 
                        cacheFileBanner.exists() && cacheFileBanner.length() > 0) {
                        Log.d("DataRepository", "Video already cached: $url")
                        return@launch
                    }
                    Log.d("DataRepository", "Start preloading video: $url")
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.inputStream.use { input ->
                        val tempFile = java.io.File(context.cacheDir, url.hashCode().toString() + ".tmp")
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        
                        if (!cacheFileStandard.exists() || cacheFileStandard.length() == 0L) {
                            try {
                                tempFile.copyTo(cacheFileStandard, overwrite = true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        if (cacheFileBanner.exists()) {
                            try {
                                cacheFileBanner.delete()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        if (tempFile.renameTo(cacheFileBanner)) {
                            Log.d("DataRepository", "Video preloaded successfully: $url")
                            
                            val thumbFile = java.io.File(context.cacheDir, url.hashCode().toString() + "_thumb.jpg")
                            if (!thumbFile.exists() || thumbFile.length() == 0L) {
                                val retriever = android.media.MediaMetadataRetriever()
                                try {
                                    retriever.setDataSource(cacheFileBanner.absolutePath)
                                    var bitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    if (bitmap == null) {
                                        bitmap = retriever.getFrameAtTime()
                                    }
                                    if (bitmap != null) {
                                        java.io.FileOutputStream(thumbFile).use { out ->
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                        }
                                        Log.d("DataRepository", "Video thumbnail preloaded and saved to: ${thumbFile.name}")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    try {
                                        retriever.release()
                                    } catch (ex: Exception) {}
                                }
                            }
                        } else {
                            tempFile.delete()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    Log.e("DataRepository", "Failed to preload video: $url, error: ${e.message}")
                }
            }
        }
    }
}

data class BannerSlideData(
    val url: String,
    val duration: Int,
    val title: String?,
    val type: String
)

