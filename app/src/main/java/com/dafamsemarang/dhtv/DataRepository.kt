package com.dafamsemarang.dhtv
 
import android.util.Log
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.database.*
import java.util.Locale
import java.util.TimeZone
import java.text.SimpleDateFormat
 
object DataRepository {
    // Live data holders (Compose mutableState for reactivity)
    val menuItems = mutableStateOf<List<MenuItemData>>(emptyList())
    val requestItems = mutableStateOf<List<GuestRequest>>(emptyList())
 
    // Slideshow & Video Live data holders
    val slideshowImages = mutableStateOf<List<String>>(emptyList())
    val slideshowDurations = mutableStateOf<List<Int>>(emptyList())
    val isSlideshowActive = mutableStateOf(false)
    val isLoadingSlideshow = mutableStateOf(true)
    val currentImageIndex = mutableStateOf(0)
    
    val videoUrls = mutableStateOf<List<String>>(emptyList())
    val isLoadingVideos = mutableStateOf(true)
 
    // Loading state flags
    val isMenuLoaded = mutableStateOf(false)
    val isRequestLoaded = mutableStateOf(false)

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
 
    private var menuListener: ValueEventListener? = null
    private var requestListener: ValueEventListener? = null
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
 
    private var activeBranchId: String? = null
    private var activeMenuRef: DatabaseReference? = null
    private var activeRequestRef: DatabaseReference? = null
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
            guestInfoListener != null
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
                            isSmoking = snapshot.child("isSmoking").getValue(Boolean::class.java) == true
                        )
                        guestInfo.value = info
                        Log.d("DataRepository", "Guest info loaded: ${info.fname}, Folio: ${info.folio}")
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
                    val airportSnapshot = snapshot.child(icaoCode)
                    val airportCode = airportSnapshot.key ?: icaoCode
                    
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

        activeCompanyIconRef?.let { ref -> companyIconListener?.let { ref.removeEventListener(it) } }
        activeWeatherSettingRef?.let { ref -> weatherSettingListener?.let { ref.removeEventListener(it) } }
        activeLiveWeatherRef?.let { ref -> liveWeatherListener?.let { ref.removeEventListener(it) } }
        activeForecastRef?.let { ref -> forecastListener?.let { ref.removeEventListener(it) } }
        activeFidsSettingRef?.let { ref -> fidsSettingListener?.let { ref.removeEventListener(it) } }
        activeFlightInfoRef?.let { ref -> flightInfoListener?.let { ref.removeEventListener(it) } }
        activeGuestInfoRef?.let { ref -> guestInfoListener?.let { ref.removeEventListener(it) } }
        activeDndRef?.let { ref -> dndListener?.let { ref.removeEventListener(it) } }
 
        menuListener = null
        requestListener = null
        slideshowListener = null
        videoListener = null

        companyIconListener = null
        weatherSettingListener = null
        liveWeatherListener = null
        forecastListener = null
        fidsSettingListener = null
        flightInfoListener = null
        guestInfoListener = null
        dndListener = null
 
        activeMenuRef = null
        activeRequestRef = null
        activeSlideshowRef = null
        activeVideoRef = null

        activeCompanyIconRef = null
        activeWeatherSettingRef = null
        activeLiveWeatherRef = null
        activeForecastRef = null
        activeFidsSettingRef = null
        activeFlightInfoRef = null
        activeGuestInfoRef = null
        activeDndRef = null
 
        activeBranchId = null
        isMenuLoaded.value = false
        isRequestLoaded.value = false
        isSlideshowActive.value = false
        isLoadingSlideshow.value = true
        isLoadingVideos.value = true
        slideshowImages.value = emptyList()
        slideshowDurations.value = emptyList()
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
    }
}
