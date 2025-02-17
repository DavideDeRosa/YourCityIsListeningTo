package com.derosa.progettolam.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.derosa.progettolam.R
import com.derosa.progettolam.activities.AppActivity
import com.derosa.progettolam.activities.LoginActivity
import com.derosa.progettolam.activities.MapListActivity
import com.derosa.progettolam.db.AllAudioDataEntity
import com.derosa.progettolam.dialogs.AudioMetadataDialog
import com.derosa.progettolam.pojo.AudioAllData
import com.derosa.progettolam.pojo.AudioMetaData
import com.derosa.progettolam.util.DataSingleton
import com.derosa.progettolam.util.ExtraUtil
import com.derosa.progettolam.viewmodel.AudioViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.library.BuildConfig
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.util.Locale

class Mappa : Fragment() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var audioViewModel: AudioViewModel
    private var isNetworkAvailable = false
    private var id = 0
    private var markerList = ArrayList<AudioAllData>()
    private var lastCheckTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        audioViewModel = (activity as AppActivity).audioViewModel

        val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isNetworkAvailable = sharedPref.getBoolean("network_state", false)

        if (isNetworkAvailable) {
            val token = DataSingleton.token
            if (token != null) {
                audioViewModel.allAudio(token)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mappa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isNetworkAvailable) {
            view.findViewById<TextView>(R.id.txtOfflineMappa).visibility = View.VISIBLE
            view.findViewById<MapView>(R.id.map).visibility = View.GONE
            view.findViewById<FloatingActionButton>(R.id.viewMapList).visibility = View.GONE
            view.findViewById<FloatingActionButton>(R.id.myLocation).visibility = View.GONE
        } else {
            map = view.findViewById(R.id.map)
            map.setMultiTouchControls(true)

            val startingPoint = GeoPoint(41.8719, 12.5674) //Italia punto di partenza
            map.controller.setZoom(7.0)
            map.controller.setCenter(startingPoint)

            observeAllAudio()
            observeAllAudioDb()
            observeAudioById()

            automaticFetch()

            val fabViewMapList: FloatingActionButton = view.findViewById(R.id.viewMapList)
            fabViewMapList.setOnClickListener {
                val intent = Intent(activity, MapListActivity::class.java)
                startActivity(intent)
                activity?.finish()
            }

            val fabMyLocation: FloatingActionButton = view.findViewById(R.id.myLocation)
            fabMyLocation.setOnClickListener {
                if (checkPermission()) {
                    if (isLocationEnabled()) {
                        getLastLocation()
                    } else {
                        Toast.makeText(
                            activity,
                            "Abilita i servizi di localizzazione",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                } else {
                    askPermission()
                }
            }
        }
    }

    private fun observeAllAudio() {
        audioViewModel.observeAllAudioLiveData().observe(viewLifecycleOwner) {
            for (audio in it) {
                markerList.add(audio)
                addMarkerToMap(audio)
            }
        }

        audioViewModel.observeAllAudioErrorLiveData().observe(viewLifecycleOwner) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun observeAllAudioDb() {
        audioViewModel.observeAllAudioDbLiveData().observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                val token = DataSingleton.token
                if (token != null) {
                    audioViewModel.getAudioById(token, id)
                }
            } else {
                val customDialog = AudioMetadataDialog(null, list[0])
                customDialog.show(parentFragmentManager, "AudioMetaDataDialog")
            }
        }
    }

    private fun observeAudioById() {
        audioViewModel.observeAudioByIdLiveData().observe(viewLifecycleOwner) {
            saveIntoDb(it)
            val customDialog = AudioMetadataDialog(it, null)
            customDialog.show(parentFragmentManager, "AudioMetaDataDialog")
        }

        audioViewModel.observeAudioByIdErrorLiveData().observe(viewLifecycleOwner) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun addMarkerToMap(audio: AudioAllData) {
        val marker = Marker(map)
        val position = GeoPoint(audio.latitude, audio.longitude)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.maps_marker)
        map.overlays.add(marker)

        marker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                id = audio.id

                audioViewModel.getAllAudioByCoord(
                    audio.longitude,
                    audio.latitude
                )

                return true
            }
        })
    }

    private fun automaticFetch() {
        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                checkDebounced()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return true
            }
        })
    }

    private fun checkDebounced() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheckTime > 7000) {
            lastCheckTime = currentTime
            checkAndFetch()
        }
    }

    private fun checkAndFetch() {
        val visibleList = getVisibleMarkers()
        if (visibleList.size <= 10) {
            viewLifecycleOwner.lifecycleScope.launch {
                fetchData(visibleList)
            }
        }
    }

    private fun getVisibleMarkers(): List<AudioAllData> {
        val mapBounds = map.boundingBox
        val list = ArrayList<AudioAllData>()

        for (marker in markerList) {
            if (mapBounds.contains(GeoPoint(marker.latitude, marker.longitude))) {
                list.add(marker)
            }
        }

        return list
    }

    private suspend fun fetchData(list: List<AudioAllData>) {
        withContext(Dispatchers.IO) {
            for (x in list) {
                val token = DataSingleton.token
                if (token != null) {
                    audioViewModel.getAllAudioByCoordFetch(
                        token,
                        x.id,
                        x.longitude,
                        x.latitude,
                        requireContext()
                    )
                }
            }
        }
    }

    private fun saveIntoDb(audio: AudioMetaData) {
        audioViewModel.insertAllAudioDb(
            AllAudioDataEntity(
                id = audio.id,
                username = audio.creator_username,
                longitude = audio.longitude,
                latitude = audio.latitude,
                locationName = getLocationName(audio.longitude, audio.latitude),
                bpm = audio.tags.bpm,
                danceability = audio.tags.danceability,
                loudness = audio.tags.loudness,
                genre = audio.tags.genre.getMaxGenre().first,
                instrument = audio.tags.instrument.getMaxInstrument().first,
                mood = audio.tags.mood.getMaxMood().first
            )
        )
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && isLocationFresh(location)) {
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                map.controller.setZoom(13.0)
                map.controller.setCenter(currentLocation)
            } else {
                getCurrentLocation()
            }
        }.addOnFailureListener {
            getCurrentLocation()
        }
    }

    private fun isLocationFresh(location: Location): Boolean {
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < 10000
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(onTokenCanceledListener: OnTokenCanceledListener): CancellationToken {
                    return this
                }

                override fun isCancellationRequested(): Boolean {
                    return false
                }
            }).addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                map.controller.setZoom(13.0)
                map.controller.setCenter(currentLocation)
            } else {
                startLocationUpdates()
            }
        }.addOnFailureListener {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        val currentLocation = GeoPoint(location.latitude, location.longitude)
                        map.controller.setZoom(13.0)
                        map.controller.setCenter(currentLocation)
                        fusedLocationClient.removeLocationUpdates(this)
                    } else {
                        Toast.makeText(
                            activity,
                            "Non è possibile trovare la tua ultima posizione",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            null
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (isLocationEnabled()) {
                    getLastLocation()
                } else {
                    Toast.makeText(
                        activity,
                        "Abilita i servizi di localizzazione",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(activity, "Permesso negato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private fun getLocationName(longitude: Double, latitude: Double): String {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        var locationName = ""

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                locationName = addresses[0].getAddressLine(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return locationName
    }

    override fun onResume() {
        super.onResume()
        if (isNetworkAvailable) {
            map.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isNetworkAvailable) {
            map.onPause()
        }
    }

    private fun goToLogin() {
        DataSingleton.token = null
        DataSingleton.username = null

        ExtraUtil.clearTokenAndUsername(requireContext())

        val intent = Intent(activity, LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}