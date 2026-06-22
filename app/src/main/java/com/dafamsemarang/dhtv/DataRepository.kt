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

    // Flight / FIDS
    val fidsActive = mutableStateOf(true)
    val fidsIcaoCode = mutableStateOf("WARS")
    val flightArrivals = mutableStateOf<List<Flight>>(emptyList())
    val flightDepartures = mutableStateOf<List<Flight>>(emptyList())
    val flightAirportName = mutableStateOf("Ahmad Yani Airport")

    // Guest & DND
    val guestInfo = mutableStateOf<GuestInfo?>(null)
    val isDndActive = mutableStateOf(false)
    val instagramHandle = mutableStateOf<String?>(null)
    val facebookHandle = mutableStateOf<String?>(null)
    val tiktokHandle = mutableStateOf<String?>(null)
    val websiteUrl = mutableStateOf<String?>(null)
    val branchLatLng = mutableStateOf<String?>(null)  // Format: "lat,lng" from LONGLAT_BRANCH

 
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
    private var fidsSettingListener: ValueEventListener? = null
    private var flightInfoListener: ValueEventListener? = null
    private var guestInfoListener: ValueEventListener? = null
    private var dndListener: ValueEventListener? = null
    private var contactListener: ValueEventListener? = null
    private var branchLatLngListener: ValueEventListener? = null

 
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
    private var activeFidsSettingRef: DatabaseReference? = null
    private var activeFlightInfoRef: DatabaseReference? = null
    private var activeGuestInfoRef: DatabaseReference? = null
    private var activeDndRef: DatabaseReference? = null
    private var activeContactRef: DatabaseReference? = null
    private var activeBranchLatLngRef: DatabaseReference? = null

 
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
            branchLatLngListener != null

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
                isMenuLoaded.value = true
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
                isRequestLoaded.value = true
            }
        }
        requestRef.addValueEventListener(requestListener!!)
 
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
        hotelFacRef.addValueEventListener(hotelFacilitiesListener!!)

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
        roomFacRef.addValueEventListener(roomFacilitiesListener!!)

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
        emergRef.addValueEventListener(emergencyProcedureListener!!)

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
        healthRef.addValueEventListener(healthAndWellnessListener!!)

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
        discoverRef.addValueEventListener(discoverDestinationListener!!)
 
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
        slideshowRef.addValueEventListener(slideshowListener!!)
 
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
        iconRef.addValueEventListener(companyIconListener!!)

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
                        setupDndListener(db, branchId, info.folio)
                    } else {
                        guestInfo.value = null
                        setupDndListener(db, branchId, null)
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
        contactRef.addValueEventListener(contactListener!!)

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
        branchLatLngRef.addValueEventListener(branchLatLngListener!!)
    }

    private fun setupWeatherListeners(db: DatabaseReference, city: String?) {
        activeLiveWeatherRef?.let { ref -> liveWeatherListener?.let { ref.removeEventListener(it) } }
        activeForecastRef?.let { ref -> forecastListener?.let { ref.removeEventListener(it) } }
        liveWeatherListener = null
        forecastListener = null
        activeLiveWeatherRef = null
        activeForecastRef = null
 
        if (city.isNullOrEmpty()) {
            liveWeather.value = null
            forecastData.value = null
            return
        }
 
        // Live Weather Listener
        val liveRef = db.child("weather").child("liveWeather")
        activeLiveWeatherRef = liveRef
        liveWeatherListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
                            Log.d("DataRepository", "Parsed single live weather node for $city")
                        } else {
                            Log.w("DataRepository", "No live weather data found matching city: $city")
                            liveWeather.value = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DataRepository", "Error parsing live weather: ${e.message}", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Live weather listener cancelled: ${error.message}")
            }
        }
        liveRef.addValueEventListener(liveWeatherListener!!)
 
        // Forecast Listener
        val forecastRef = db.child("weather").child("forecast")
        activeForecastRef = forecastRef
        forecastListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
                            Log.d("DataRepository", "Parsed single forecast node for $city")
                        } else {
                            Log.w("DataRepository", "No forecast data found matching city: $city")
                            forecastData.value = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DataRepository", "Error parsing forecast data: ${e.message}", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataRepository", "Forecast listener cancelled: ${error.message}")
            }
        }
        forecastRef.addValueEventListener(forecastListener!!)
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
 
        val flightRef = db.child("FlightInfo")
        activeFlightInfoRef = flightRef
        flightInfoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val upperIcao = icaoCode.uppercase(Locale.US)
                    val airportSnapshot = snapshot.child(upperIcao)
                    val airportCode = airportSnapshot.key ?: upperIcao
                    
                    var name = when (icaoCode.uppercase(Locale.US)) {
                        "WARS" -> "Ahmad Yani Airport"
                        "WARR" -> "Juanda Airport"
                        else -> "$icaoCode Airport"
                    }
                    val configAirports = snapshot.child("config").child("Airports")
                    for (airportConfig in configAirports.children) {
                        val icao = airportConfig.child("ICAO_Code").getValue(String::class.java)
                        if (icao != null && icao.equals(airportCode, ignoreCase = true)) {
                            name = airportConfig.child("airpotName").getValue(String::class.java) ?: name
                            break
                        }
                    }
                    flightAirportName.value = name
 
                    // Parse Arrivals
                    val arrivalsList = mutableListOf<Flight>()
                    val arrivalsSnap = airportSnapshot.child("arrivals")
                    for (flightSnap in arrivalsSnap.children) {
                        try {
                            val scheduled = flightSnap.child("scheduledTime").getValue(String::class.java) ?: ""
                            val revised = flightSnap.child("revisedTime").getValue(String::class.java) ?: ""
                            val timeStr = revised.ifEmpty { scheduled }
                            
                            val flightDate = parseUtcToWib(timeStr)
                            if (flightDate != null && isSameDayInWib(flightDate)) {
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
                            if (flightDate != null && isSameDayInWib(flightDate)) {
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
        activeLiveWeatherRef?.let { ref -> liveWeatherListener?.let { ref.removeEventListener(it) } }
        activeForecastRef?.let { ref -> forecastListener?.let { ref.removeEventListener(it) } }
        activeFidsSettingRef?.let { ref -> fidsSettingListener?.let { ref.removeEventListener(it) } }
        activeFlightInfoRef?.let { ref -> flightInfoListener?.let { ref.removeEventListener(it) } }
        activeGuestInfoRef?.let { ref -> guestInfoListener?.let { ref.removeEventListener(it) } }
        activeDndRef?.let { ref -> dndListener?.let { ref.removeEventListener(it) } }
        activeContactRef?.let { ref -> contactListener?.let { ref.removeEventListener(it) } }
 
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

 
        activeBranchId = null
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

