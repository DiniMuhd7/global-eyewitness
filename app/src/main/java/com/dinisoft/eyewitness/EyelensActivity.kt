package com.dinisoft.eyewitness

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.internal.ViewUtils.getContentView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.dinisoft.eyewitness.adapters.EyelensAdapter
import com.dinisoft.eyewitness.model.Incident
import com.dinisoft.eyewitness.setting.WebviewGPSTrack
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.internal.ViewUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.eyelens_main.*
import kotlinx.android.synthetic.main.eyelens_main.cardList
import kotlinx.android.synthetic.main.eyelens_main.defaultMessageTextView
import kotlinx.android.synthetic.main.layout_dash.*
import kotlinx.android.synthetic.main.review_main.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class EyelensActivity : AppCompatActivity() {

    private val TAG: String = "--EyelensActivity"

    // firebase auth, database and incident object
    lateinit var auth: FirebaseAuth
    lateinit var db: FirebaseFirestore
    lateinit var mIncident: Incident
    lateinit var mState: String // For security check
    lateinit var mCountry: String
    lateinit var mGlobal: String
    lateinit var eyelensView: String

    lateinit var uuid: String
    lateinit var docID: String
    lateinit var type: String
    lateinit var title: String
    lateinit var subtype: String
    lateinit var desc: String
    lateinit var address: String
    lateinit var reportedTo: String
    lateinit var reviewStat: String
    lateinit var reviewInfo: String
    lateinit var date: String
    lateinit var eyestate: String
    lateinit var eyecountry: String
    lateinit var evid: String
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    private val mIncidentMute = mutableListOf<Incident>()

    private lateinit var mEyelensAdapter: EyelensAdapter
    private lateinit var mContext: Context
    private lateinit var mStorageDialogPopUp: PopupWindow

    lateinit var mStorage: FirebaseStorage
    lateinit var mStorageRef: StorageReference
    lateinit var mRefVideo: StorageReference
    lateinit var mRefAudio: StorageReference
    lateinit var mRefPhoto: StorageReference
    lateinit var mRefPDF: StorageReference
    lateinit var mRefDOC: StorageReference

    val TOC_EYELENS = "Eyelens"

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2200
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private const val LOCATION_PERMISSION_CODE = 103
        private const val PHONE_PERMISSION_CODE = 104
        private const val COAST_LOCATION_PERMISSION_CODE = 105
        private const val READ_STORAGE_PERMISSION_CODE = 106
    }


    val PDF: Int = 0
    val DOCX: Int = 1
    val AUDIO: Int = 2
    val VIDEO: Int = 3
    val PHOTO: Int = 4

    lateinit var filePath: Uri

    // The entry point to the Fused Location Provider.
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private val DEFAULT_ZOOM = 15
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private val locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    // Keys for storing activity state.
    // [START maps_current_place_state_keys]
    private val KEY_CAMERA_POSITION = "camera_position"
    private val KEY_LOCATION = "location"

    // [END maps_current_place_state_keys]
    lateinit var geocoder: Geocoder
    private val marker: Marker? = null

    private var currentItemPosition = -1

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eyelens_main)
        mContext = this
        // Initialize Firebase Auth
        auth = Firebase.auth
        db = Firebase.firestore

        mStorage = FirebaseStorage.getInstance()
        mStorageRef = mStorage.getReference()

        val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())
        mRefPhoto = mStorageRef.child("eyewitness/evidence/image/" + UUID.randomUUID().toString())
        //mRefVideo = mStorageRef.child("eyewitness/evidence/video/" + UUID.randomUUID().toString())
        mRefAudio = mStorageRef.child("eyewitness/evidence/audio/" + UUID.randomUUID().toString())
        mRefDOC = mStorageRef.child("eyewitness/evidence/docx/" + UUID.randomUUID().toString())
        mRefPDF = mStorageRef.child("eyewitness/evidence/pdf/" + UUID.randomUUID().toString())

        mIncident = Incident()

        geocoder = Geocoder(this@EyelensActivity, Locale.getDefault())
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext as EyelensActivity)



        val userDp = findViewById<View>(R.id.userDp) as ImageView
        val txtStorageUsed = findViewById<View>(R.id.txt_storage_used) as TextView
        val txtStorageCap = findViewById<View>(R.id.txt_storage_cap) as TextView

        // Show buy storage bundle dialog
        val mCardStorage = findViewById<View>(R.id.card_item_userDp) as CardView
        mCardStorage.setOnClickListener { showStorageDialog() }

        val btnState = findViewById<View>(R.id.btn_state) as AppCompatButton
        val btnCountry = findViewById<View>(R.id.btn_country) as AppCompatButton
        val btnGlobal = findViewById<View>(R.id.btn_global) as AppCompatButton


        btnState.setOnClickListener {
            btnState.setBackgroundColor(Color.parseColor("#2196F3"))
            btnState.setTextColor(Color.WHITE)
            btnGlobal.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnGlobal.setTextColor(Color.BLACK)
            btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnCountry.setTextColor(Color.BLACK)

            mIncidentMute.clear() // Clear all contents from list objrct
            mEyelensAdapter.deleteItem(cardList) // Delete all items in recycler view

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }
            fusedLocationProviderClient?.getLastLocation()
                    ?.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            //val clocation = LatLng(location.latitude, location.longitude)
                            var addresses: List<Address?> = ArrayList()

                            try {
                                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                //val address = addresses[0]!!.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                                val city = addresses[0]!!.locality
                                val state = addresses[0]!!.adminArea
                                val country = addresses[0]!!.countryName
                                val postalCode = addresses[0]!!.postalCode
                                val knownName = addresses[0]!!.featureName

                                mState = state // Save state for anonymous inner class
                                mCountry = country

                                // Feed User Dashboard with Data
                                if (knownName != null && state != null && country != null) {
                                    //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                    //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                    // Format the action buttons
                                    btnState.text = state
                                    btnCountry.text = country

                                    // Eye lens view setup
                                    val user = auth.currentUser
                                    val uid = user?.uid
                                    if (uid != null) {
                                        db.collection("subscription")
                                                .whereEqualTo("uid", uid)
                                                //.orderBy("", Query.Direction.DESCENDING)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    if (!result.isEmpty){
                                                        for (document in result){
                                                            var uid = document.getString("uid").toString()
                                                            val agencyID = document.getString("agencyID").toString()
                                                            if (!uid.isEmpty() && !agencyID.isEmpty()){
                                                                db.collection("agency")
                                                                        .whereEqualTo("name", agencyID)
                                                                        //.orderBy("", Query.Direction.DESCENDING)
                                                                        .get()
                                                                        .addOnSuccessListener { res ->
                                                                            if (!res.isEmpty) {
                                                                                for (doc in res){
                                                                                    val eyelensView = doc.getString("eyelensView").toString()
                                                                                    val agencyID = doc.getString("agencyID").toString()
                                                                                    if (!eyelensView.isEmpty() && eyelensView.equals("enabled", true)) {

                                                                                        // Retrieve Officer collection
                                                                                        db.collection("eyelens")
                                                                                                .whereEqualTo("reportedTo", agencyID)
                                                                                                //.orderBy("", Query.Direction.DESCENDING)
                                                                                                .get()
                                                                                                .addOnSuccessListener { result ->
                                                                                                    for (document in result) {
                                                                                                        uuid = document.getString("id").toString()
                                                                                                        docID = document.getString("documentID").toString()
                                                                                                        type = document.getString("type").toString()
                                                                                                        title = document.getString("title").toString()
                                                                                                        subtype = document.getString("subType").toString()
                                                                                                        desc = document.getString("desc").toString()
                                                                                                        address = document.getString("address").toString()
                                                                                                        reportedTo = document.getString("reportedTo").toString()
                                                                                                        reviewStat = document.getString("status").toString()
                                                                                                        reviewInfo = document.getString("reviewInfo").toString()
                                                                                                        eyestate = document.getString("state").toString()
                                                                                                        date = document.getString("date").toString()
                                                                                                        evid = document.getString("evidenceURL").toString()
                                                                                                        latitude = document.getDouble("latitude")!!.toDouble()
                                                                                                        longitude = document.getDouble("longitude")!!.toDouble()


                                                                                                    }
                                                                                                }
                                                                                                .addOnFailureListener { exception ->
                                                                                                    //Log.d(TAG, "Error getting documents: ", exception)
                                                                                                    Toast.makeText(this@EyelensActivity, "Sorry, something went wrong. Login again.", Toast.LENGTH_LONG).show()
                                                                                                }


                                                                                    } else {
                                                                                        defaultMessageTextView.text = "Nothing to show yet... Eye Lens View have not yet been enabled. Please check back later or subscribe to an agency that support Eye Lens View."
                                                                                        Toast.makeText(mContext, "Eye Lens View have not yet been enabled. Please check back later.", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                            }
                                                        }

                                                        // Update the Eye Lens Liew UI
                                                        if (::eyestate.isInitialized){
                                                             if (!eyestate.isEmpty() && eyestate.equals(state,true)){

                                                                                                            // Incident model object
                                                                                                            mIncident = Incident()
                                                                                                            mIncident.setIncidentID(uuid)
                                                                                                            mIncident.setDocumentID(docID)
                                                                                                            mIncident.setIncidentTitle(title)
                                                                                                            mIncident.setIncidentDesc(desc)
                                                                                                            mIncident.setIncidentType(type)
                                                                                                            mIncident.setIncidentSubType(subtype)
                                                                                                            mIncident.setIncidentAddress(address)
                                                                                                            mIncident.setIncidentEvidenceURL(evid)
                                                                                                            mIncident.setIncidentLatitude(latitude!!)
                                                                                                            mIncident.setIncidentLongitude(longitude!!)
                                                                                                            mIncident.setIncidentDate(date)
                                                                                                            mIncident.setIncidentReportedTo(reportedTo)
                                                                                                            mIncident.setIncidentReviewStatus(reviewStat)
                                                                                                            mIncident.setIncidentReviewInfo(reviewInfo)
                                                                                                            mIncidentMute.add(mIncident)


                                                                                                            //Toast.makeText(this, document.data.toString(), Toast.LENGTH_LONG).show()
                                                                                                            mEyelensAdapter = EyelensAdapter(mIncidentMute, mContext)
                                                                                                            cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                                                                                            cardList.adapter = mEyelensAdapter
                                                                                                            defaultMessageTextView.visibility = View.GONE
                                                                                                        }
                                                        }

                                                    } else {
                                                        defaultMessageTextView.text = "Nothing to show yet... To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View."
                                                        Toast.makeText(mContext, "To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View.", Toast.LENGTH_LONG).show()
                                                    }
                                                }

                                    }


                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Sorry ♥, your location is unknown. Try again.", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }


                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    })


        }


        btnCountry.setOnClickListener {
            btnCountry.setBackgroundColor(Color.parseColor("#2196F3"))
            btnCountry.setTextColor(Color.WHITE)
            btnGlobal.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnGlobal.setTextColor(Color.BLACK)
            btnState.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnState.setTextColor(Color.BLACK)

            mIncidentMute.clear() // Clear all contents from list objrct
            mEyelensAdapter.deleteItem(cardList) // Delete all items in recycler view

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }
            fusedLocationProviderClient?.getLastLocation()
                    ?.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            //val clocation = LatLng(location.latitude, location.longitude)
                            var addresses: List<Address?> = ArrayList()

                            try {
                                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                //val address = addresses[0]!!.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                                val city = addresses[0]!!.locality
                                val state = addresses[0]!!.adminArea
                                val country = addresses[0]!!.countryName
                                val postalCode = addresses[0]!!.postalCode
                                val knownName = addresses[0]!!.featureName

                                mState = state // Save state for anonymous inner class
                                mCountry = country

                                // Feed User Dashboard with Data
                                if (knownName != null && state != null && country != null) {
                                    //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                    //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                    // Format the action buttons
                                    btnState.text = state
                                    btnCountry.text = country

                                    // Eye lens view setup
                                    val user = auth.currentUser
                                    val uid = user?.uid
                                    if (uid != null) {
                                        db.collection("subscription")
                                                .whereEqualTo("uid", uid)
                                                //.orderBy("", Query.Direction.DESCENDING)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    if (!result.isEmpty){
                                                        for (document in result){
                                                            val uid = document.getString("uid").toString()
                                                            val agencyID = document.getString("agencyID").toString()
                                                            if (!uid.isEmpty() && !agencyID.isEmpty()){
                                                                db.collection("agency")
                                                                        .whereEqualTo("name", agencyID)
                                                                        //.orderBy("", Query.Direction.DESCENDING)
                                                                        .get()
                                                                        .addOnSuccessListener { res ->
                                                                            if (!res.isEmpty) {
                                                                                for (doc in res){
                                                                                    val eyelensView = doc.getString("eyelensView").toString()
                                                                                    val agencyID = doc.getString("agencyID").toString()
                                                                                    if (!eyelensView.isEmpty() && eyelensView.equals("enabled", true)) {

                                                                                        // Retrieve Officer collection
                                                                                        db.collection("eyelens")
                                                                                                .whereEqualTo("reportedTo", agencyID)
                                                                                                //.orderBy("", Query.Direction.DESCENDING)
                                                                                                .get()
                                                                                                .addOnSuccessListener { result ->
                                                                                                    for (document in result) {
                                                                                                        uuid = document.getString("id").toString()
                                                                                                        docID = document.getString("documentID").toString()
                                                                                                        type = document.getString("type").toString()
                                                                                                        title = document.getString("title").toString()
                                                                                                        subtype = document.getString("subType").toString()
                                                                                                        desc = document.getString("desc").toString()
                                                                                                        address = document.getString("address").toString()
                                                                                                        reportedTo = document.getString("reportedTo").toString()
                                                                                                        reviewStat = document.getString("status").toString()
                                                                                                        reviewInfo = document.getString("reviewInfo").toString()
                                                                                                        eyecountry = document.getString("country").toString()
                                                                                                        date = document.getString("date").toString()
                                                                                                        evid = document.getString("evidenceURL").toString()
                                                                                                        latitude = document.getDouble("latitude")!!.toDouble()
                                                                                                        longitude = document.getDouble("longitude")!!.toDouble()


                                                                                                    }
                                                                                                }
                                                                                                .addOnFailureListener { exception ->
                                                                                                    //Log.d(TAG, "Error getting documents: ", exception)
                                                                                                    Toast.makeText(this@EyelensActivity, "Sorry, something went wrong. Login again.", Toast.LENGTH_LONG).show()
                                                                                                }


                                                                                    } else {
                                                                                        defaultMessageTextView.text = "Nothing to show yet... Eye Lens View have not yet been enabled. Please check back later or subscribe to an agency that support Eye Lens View."
                                                                                        Toast.makeText(mContext, "Eye Lens View have not yet been enabled. Please check back later.", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                            }
                                                        }

                                                        // Update the Eye Lens Liew UI
                                                        if (::eyecountry.isInitialized){
                                                            if (!eyecountry.isEmpty() && eyecountry.equals(country,true)){

                                                                // Incident model object
                                                                mIncident = Incident()
                                                                mIncident.setIncidentID(uuid)
                                                                mIncident.setDocumentID(docID)
                                                                mIncident.setIncidentTitle(title)
                                                                mIncident.setIncidentDesc(desc)
                                                                mIncident.setIncidentType(type)
                                                                mIncident.setIncidentSubType(subtype)
                                                                mIncident.setIncidentAddress(address)
                                                                mIncident.setIncidentEvidenceURL(evid)
                                                                mIncident.setIncidentLatitude(latitude!!)
                                                                mIncident.setIncidentLongitude(longitude!!)
                                                                mIncident.setIncidentDate(date)
                                                                mIncident.setIncidentReportedTo(reportedTo)
                                                                mIncident.setIncidentReviewStatus(reviewStat)
                                                                mIncident.setIncidentReviewInfo(reviewInfo)
                                                                mIncidentMute.add(mIncident)


                                                                //Toast.makeText(this, document.data.toString(), Toast.LENGTH_LONG).show()
                                                                mEyelensAdapter = EyelensAdapter(mIncidentMute, mContext)
                                                                cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                                                cardList.adapter = mEyelensAdapter
                                                                defaultMessageTextView.visibility = View.GONE
                                                            }
                                                        }

                                                    } else {
                                                        defaultMessageTextView.text = "Nothing to show yet... To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View."
                                                        Toast.makeText(mContext, "To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View.", Toast.LENGTH_LONG).show()
                                                    }
                                                }

                                    }


                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Sorry ♥, your location is unknown. Try again.", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }


                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    })


        }


        btnGlobal.setOnClickListener {
            btnGlobal.setBackgroundColor(Color.parseColor("#2196F3"))
            btnGlobal.setTextColor(Color.WHITE)
            btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnCountry.setTextColor(Color.BLACK)
            btnState.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnState.setTextColor(Color.BLACK)

            mIncidentMute.clear() // Clear all contents from list objrct
            mEyelensAdapter.deleteItem(cardList) // Delete all items in recycler view

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }
            fusedLocationProviderClient?.getLastLocation()
                    ?.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            //val clocation = LatLng(location.latitude, location.longitude)
                            var addresses: List<Address?> = ArrayList()

                            try {
                                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                //val address = addresses[0]!!.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                                val city = addresses[0]!!.locality
                                val state = addresses[0]!!.adminArea
                                val country = addresses[0]!!.countryName
                                val postalCode = addresses[0]!!.postalCode
                                val knownName = addresses[0]!!.featureName

                                mState = state // Save state for anonymous inner class
                                mCountry = country

                                // Feed User Dashboard with Data
                                if (knownName != null && state != null && country != null) {
                                    //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                    //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                    // Format the action buttons
                                    btnState.text = state
                                    btnCountry.text = country

                                    // Eye lens view setup
                                    val user = auth.currentUser
                                    val uid = user?.uid
                                    if (uid != null) {
                                        db.collection("subscription")
                                                .whereEqualTo("uid", uid)
                                                //.orderBy("", Query.Direction.DESCENDING)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    if (!result.isEmpty){
                                                        for (document in result){
                                                            val uid = document.getString("uid").toString()
                                                            val agencyID = document.getString("agencyID").toString()
                                                            if (!uid.isEmpty() && !agencyID.isEmpty()){
                                                                db.collection("agency")
                                                                        .whereEqualTo("name", agencyID)
                                                                        //.orderBy("", Query.Direction.DESCENDING)
                                                                        .get()
                                                                        .addOnSuccessListener { res ->
                                                                            if (!res.isEmpty) {
                                                                                for (doc in res){
                                                                                    val eyelensView = doc.getString("eyelensView").toString()
                                                                                    val agencyID = doc.getString("agencyID").toString()
                                                                                    if (!eyelensView.isEmpty() && eyelensView.equals("enabled", true)) {

                                                                                        // Retrieve Officer collection
                                                                                        db.collection("eyelens")
                                                                                                .whereEqualTo("reportedTo", agencyID)
                                                                                                //.orderBy("", Query.Direction.DESCENDING)
                                                                                                .get()
                                                                                                .addOnSuccessListener { result ->
                                                                                                    for (document in result) {
                                                                                                        uuid = document.getString("id").toString()
                                                                                                        docID = document.getString("documentID").toString()
                                                                                                        type = document.getString("type").toString()
                                                                                                        title = document.getString("title").toString()
                                                                                                        subtype = document.getString("subType").toString()
                                                                                                        desc = document.getString("desc").toString()
                                                                                                        address = document.getString("address").toString()
                                                                                                        reportedTo = document.getString("reportedTo").toString()
                                                                                                        reviewStat = document.getString("status").toString()
                                                                                                        reviewInfo = document.getString("reviewInfo").toString()
                                                                                                        eyecountry = document.getString("country").toString()
                                                                                                        date = document.getString("date").toString()
                                                                                                        evid = document.getString("evidenceURL").toString()
                                                                                                        latitude = document.getDouble("latitude")!!.toDouble()
                                                                                                        longitude = document.getDouble("longitude")!!.toDouble()


                                                                                                    }
                                                                                                }
                                                                                                .addOnFailureListener { exception ->
                                                                                                    //Log.d(TAG, "Error getting documents: ", exception)
                                                                                                    Toast.makeText(this@EyelensActivity, "Sorry, something went wrong. Login again.", Toast.LENGTH_LONG).show()
                                                                                                }


                                                                                    } else {
                                                                                        defaultMessageTextView.text = "Nothing to show yet... Eye Lens View have not yet been enabled. Please check back later or subscribe to an agency that support Eye Lens View."
                                                                                        Toast.makeText(mContext, "Eye Lens View have not yet been enabled. Please check back later.", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                            }
                                                        }

                                                        // Update the Eye Lens Liew UI
                                                        if (::eyecountry.isInitialized){
                                                            if (!eyecountry.isEmpty() && !eyecountry.equals(country,true)){

                                                                // Incident model object
                                                                mIncident = Incident()
                                                                mIncident.setIncidentID(uuid)
                                                                mIncident.setDocumentID(docID)
                                                                mIncident.setIncidentTitle(title)
                                                                mIncident.setIncidentDesc(desc)
                                                                mIncident.setIncidentType(type)
                                                                mIncident.setIncidentSubType(subtype)
                                                                mIncident.setIncidentAddress(address)
                                                                mIncident.setIncidentEvidenceURL(evid)
                                                                mIncident.setIncidentLatitude(latitude!!)
                                                                mIncident.setIncidentLongitude(longitude!!)
                                                                mIncident.setIncidentDate(date)
                                                                mIncident.setIncidentReportedTo(reportedTo)
                                                                mIncident.setIncidentReviewStatus(reviewStat)
                                                                mIncident.setIncidentReviewInfo(reviewInfo)
                                                                mIncidentMute.add(mIncident)


                                                                //Toast.makeText(this, document.data.toString(), Toast.LENGTH_LONG).show()
                                                                mEyelensAdapter = EyelensAdapter(mIncidentMute, mContext)
                                                                cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                                                cardList.adapter = mEyelensAdapter
                                                                defaultMessageTextView.visibility = View.GONE
                                                            }
                                                        }


                                                    } else {
                                                        defaultMessageTextView.text = "Nothing to show yet... To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View."
                                                        Toast.makeText(mContext, "To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View.", Toast.LENGTH_LONG).show()
                                                    }
                                                }

                                    }


                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Sorry ♥, your location is unknown. Try again.", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }


                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    })


        }

        val btnOfficer = findViewById<View>(R.id.img_police) as ImageView
        val btnAccident = findViewById<View>(R.id.img_accident) as ImageView
        val btnTheft = findViewById<View>(R.id.img_robbery) as ImageView
        val btnSexAssault = findViewById<View>(R.id.img_rape) as ImageView
        val btnTerrorism = findViewById<View>(R.id.img_terrorist) as ImageView

        btnOfficer.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                db.collection("officer")
                        .whereEqualTo("status", "Approved")
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.size() != 0) {
                                val playList = result.documents
                                val random = Random()
                                val playIndex = random.nextInt(playList.size)

                                val mVisibility = playList[playIndex].getString("visibility")
                                if (mVisibility != null && mVisibility.equals("public", true)) {
                                    // Query officer collection
                                    db.collection("officer")
                                            .whereEqualTo("visibility", mVisibility)
                                            .get()
                                            .addOnSuccessListener { res ->
                                                // Get the total number of approved incident report that has been set to public
                                                if (res.size() != null) {
                                                    val mPlayList = res.documents
                                                    val mrandom = Random()
                                                    val mPlayIndex = mrandom.nextInt(mPlayList.size)

                                                    val mEvidURL = mPlayList[mPlayIndex].getString("evidenceURL")
                                                    if (mEvidURL != null && mEvidURL.endsWith("-video", true)) {
                                                        // Download Video to Local File
                                                        downloadVideoToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-video")))
                                                    } else if (mEvidURL != null && mEvidURL.endsWith("-image", true)) {
                                                        downloadImageToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-image")))
                                                    } else {
                                                        val parentLayout: View = findViewById(android.R.id.content)
                                                        Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                                .show()
                                                    }

                                                }
                                            }
                                }

                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                            }
                        }

            }
        }

        btnAccident.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                db.collection("accident")
                        .whereEqualTo("status", "Approved")
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.size() != 0) {
                                val playList = result.documents
                                val random = Random()
                                val playIndex = random.nextInt(playList.size)

                                val mVisibility = playList[playIndex].getString("visibility")
                                if (mVisibility != null && mVisibility.equals("public", true)) {
                                    // Query accident collection
                                    db.collection("accident")
                                            .whereEqualTo("visibility", mVisibility)
                                            .get()
                                            .addOnSuccessListener { res ->
                                                // Get the total number of approved incident report that has been set to public
                                                if (res.size() != null) {
                                                    val mPlayList = res.documents
                                                    val mrandom = Random()
                                                    val mPlayIndex = mrandom.nextInt(mPlayList.size)

                                                    val mEvidURL = mPlayList[mPlayIndex].getString("evidenceURL")
                                                    if (mEvidURL != null && mEvidURL.endsWith("-video", true)) {
                                                        // Download Video to Local File
                                                        downloadVideoToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-video")))
                                                    } else if (mEvidURL != null && mEvidURL.endsWith("-image", true)) {
                                                        downloadImageToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-image")))
                                                    } else {
                                                        val parentLayout: View = findViewById(android.R.id.content)
                                                        Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                                .show()
                                                    }

                                                }
                                            }
                                }

                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                            }
                        }
            }
        }

        btnTheft.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                db.collection("theft")
                        .whereEqualTo("status", "Approved")
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.size() != 0) {
                                val playList = result.documents
                                val random = Random()
                                val playIndex = random.nextInt(playList.size)

                                val mVisibility = playList[playIndex].getString("visibility")
                                if (mVisibility != null && mVisibility.equals("public", true)) {
                                    // Query theft collection
                                    db.collection("theft")
                                            .whereEqualTo("visibility", mVisibility)
                                            .get()
                                            .addOnSuccessListener { res ->
                                                // Get the total number of approved incident report that has been set to public
                                                if (res.size() != null) {
                                                    val mPlayList = res.documents
                                                    val mrandom = Random()
                                                    val mPlayIndex = mrandom.nextInt(mPlayList.size)

                                                    val mEvidURL = mPlayList[mPlayIndex].getString("evidenceURL")
                                                    if (mEvidURL != null && mEvidURL.endsWith("-video", true)) {
                                                        // Download Video to Local File
                                                        downloadVideoToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-video")))
                                                    } else if (mEvidURL != null && mEvidURL.endsWith("-image", true)) {
                                                        downloadImageToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-image")))
                                                    } else {
                                                        val parentLayout: View = findViewById(android.R.id.content)
                                                        Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                                .show()
                                                    }

                                                }
                                            }
                                }

                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                            }
                        }

            }
        }

        btnSexAssault.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                db.collection("sexual-assault")
                        .whereEqualTo("status", "Approved")
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.size() != 0) {
                                val playList = result.documents
                                val random = Random()
                                val playIndex = random.nextInt(playList.size)

                                val mVisibility = playList[playIndex].getString("visibility")
                                if (mVisibility != null && mVisibility.equals("public", true)) {
                                    // Query sexual-assault collection
                                    db.collection("sexual-assault")
                                            .whereEqualTo("visibility", mVisibility)
                                            .get()
                                            .addOnSuccessListener { res ->
                                                // Get the total number of approved incident report that has been set to public
                                                if (res.size() != null) {
                                                    val mPlayList = res.documents
                                                    val mrandom = Random()
                                                    val mPlayIndex = mrandom.nextInt(mPlayList.size)

                                                    val mEvidURL = mPlayList[mPlayIndex].getString("evidenceURL")
                                                    if (mEvidURL != null && mEvidURL.endsWith("-video", true)) {
                                                        // Download Video to Local File
                                                        downloadVideoToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-video")))
                                                    } else if (mEvidURL != null && mEvidURL.endsWith("-image", true)) {
                                                        downloadImageToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-image")))
                                                    } else {
                                                        val parentLayout: View = findViewById(android.R.id.content)
                                                        Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                                .show()
                                                    }

                                                }
                                            }
                                }

                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                            }
                        }
            }
        }

        btnTerrorism.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                db.collection("terrorism")
                        .whereEqualTo("status", "Approved")
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.size() != 0) {
                                val playList = result.documents
                                val random = Random()
                                val playIndex = random.nextInt(playList.size)

                                val mVisibility = playList[playIndex].getString("visibility")
                                if (mVisibility != null && mVisibility.equals("public", true)) {
                                    // Query terrorism collection
                                    db.collection("terrorism")
                                            .whereEqualTo("visibility", mVisibility)
                                            .get()
                                            .addOnSuccessListener { res ->
                                                // Get the total number of approved incident report that has been set to public
                                                if (res.size() != null) {
                                                    val mPlayList = res.documents
                                                    val mrandom = Random()
                                                    val mPlayIndex = mrandom.nextInt(mPlayList.size)

                                                    val mEvidURL = mPlayList[mPlayIndex].getString("evidenceURL")
                                                    if (mEvidURL != null && mEvidURL.endsWith("-video", true)) {
                                                        // Download Video to Local File
                                                        downloadVideoToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-video")))
                                                    } else if (mEvidURL != null && mEvidURL.endsWith("-image", true)) {
                                                        downloadImageToLocalFile(mEvidURL.substring(0, mEvidURL.toString().lastIndexOf("-image")))
                                                    } else {
                                                        val parentLayout: View = findViewById(android.R.id.content)
                                                        Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                                .show()
                                                    }

                                                }
                                            }
                                }

                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "No incident reports available. Try again later.", Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                            }
                        }
            }
        }

        // Get user credentials
        val photoURL = auth.currentUser?.photoUrl.toString()
        Glide.with(this).load(photoURL).into(userDp)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
            val uid = currentUser?.uid
            db.collection("users")
                    .whereEqualTo("uid", uid)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {

                            val mStorageUsed = document.getString("storageUsed")!!.toLong()
                            val mStorageCap = document.getString("storageCap")!!.toLong()

                            txtStorageUsed.setText(mStorageUsed.toString() + " MB: Used")
                            txtStorageCap.setText(mStorageCap.toString() + " MB: Total")
                        }

                    }
        }



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient?.getLastLocation()
                ?.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        //val clocation = LatLng(location.latitude, location.longitude)
                        var addresses: List<Address?> = ArrayList()

                        try {
                            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            //val address = addresses[0]!!.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                            val city = addresses[0]!!.locality
                            val state = addresses[0]!!.adminArea
                            val country = addresses[0]!!.countryName
                            val postalCode = addresses[0]!!.postalCode
                            val knownName = addresses[0]!!.featureName

                            mState = state // Save state for anonymous inner class
                            mCountry = country

                            // Feed User Dashboard with Data
                            if (knownName != null && state != null && country != null) {
                                //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                // Format the action buttons
                                btnState.text = state
                                btnCountry.text = country

                                btnState.setBackgroundColor(Color.parseColor("#2196F3"))
                                btnState.setTextColor(Color.WHITE)
                                btnGlobal.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                                btnGlobal.setTextColor(Color.BLACK)
                                btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                                btnCountry.setTextColor(Color.BLACK)

                                // Eye lens view setup
                                val user = auth.currentUser
                                val uid = user?.uid
                                if (uid != null) {
                                    // Retrieve Officer collection
                                    db.collection("eyelens")
                                            .whereEqualTo("state", state)
                                            //.orderBy("", Query.Direction.DESCENDING)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                for (document in result) {
                                                    uuid = document.getString("id").toString()
                                                    docID = document.getString("documentID").toString()
                                                    type = document.getString("type").toString()
                                                    title = document.getString("title").toString()
                                                    subtype = document.getString("subType").toString()
                                                    desc = document.getString("desc").toString()
                                                    address = document.getString("address").toString()
                                                    reportedTo = document.getString("reportedTo").toString()
                                                    reviewStat = document.getString("status").toString()
                                                    reviewInfo = document.getString("reviewInfo").toString()
                                                    eyestate = document.getString("state").toString()
                                                    date = document.getString("date").toString()
                                                    evid = document.getString("evidenceURL").toString()
                                                    latitude = document.getDouble("latitude")!!
                                                    longitude = document.getDouble("longitude")!!


                                                    // Update the Eye Lens Liew UI
                                                    if (::eyestate.isInitialized) {
                                                        // Incident model object
                                                        mIncident = Incident()
                                                        mIncident.setIncidentID(uuid)
                                                        mIncident.setDocumentID(docID)
                                                        mIncident.setIncidentTitle(title)
                                                        mIncident.setIncidentDesc(desc)
                                                        mIncident.setIncidentType(type)
                                                        mIncident.setIncidentSubType(subtype)
                                                        mIncident.setIncidentAddress(address)
                                                        mIncident.setIncidentEvidenceURL(evid)
                                                        mIncident.setIncidentLatitude(latitude!!)
                                                        mIncident.setIncidentLongitude(longitude!!)
                                                        mIncident.setIncidentDate(date)
                                                        mIncident.setIncidentReportedTo(reportedTo)
                                                        mIncident.setIncidentReviewStatus(reviewStat)
                                                        mIncident.setIncidentReviewInfo(reviewInfo)
                                                        mIncidentMute.add(mIncident)
                                                        //mIncidentMute.
                                                        if (!eyestate.isEmpty() && eyestate.equals(state, true)) {
                                                            val counter = result.size() - result.size() + 1
                                                            if (counter == 1) {
                                                                //Toast.makeText(this, document.data.toString(), Toast.LENGTH_LONG).show()
                                                                mEyelensAdapter = EyelensAdapter(mIncidentMute, mContext)
                                                                cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                                                cardList.adapter = mEyelensAdapter
                                                                defaultMessageTextView.visibility = View.GONE

                                                            }

                                                        }
                                                    }

                                                }


                                            }
                                }
                            }



                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "Sorry ♥, your location is unknown. Try again.", Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                            }

                    })


        // Get Local time
        var current = LocalDateTime.now()
        var fullLocaleFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
        var fullLocaleTime = current.format(fullLocaleFormat) // Retrieve only date

            // Delete irrelevant records in eyelens collections
        db.collection("eyelens")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        //Log.d(TAG, "${document.id} => ${document.data}")
                        //val documentID = document.getString("documentID").toString()
                        val date = document.getString("date")

                        val cleanDate = date.toString().replaceAfter("at", " ").trim().removeSuffix("at").trim()
                        val cleanNow = fullLocaleTime.toString().replaceAfter("at", " ").trim().removeSuffix("at").trim()

                        if (cleanDate != null && !cleanDate.equals(cleanNow, true)) {
                            // Delete document from collection
                            db.collection("eyelens").document(document.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d(TAG, "DocumentSnapshot successfully deleted!")
                                        val parentLayout: View = findViewById(android.R.id.content)
                                        Snackbar.make(parentLayout, "Synchronized Sucessfully.", Snackbar.LENGTH_LONG)
                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                .show()
                                    }
                                    .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }

                        }
                    }


                }
                .addOnFailureListener { exception ->
                    //Log.d(TAG, "Error getting documents: ", exception)
                }


        val mFab = findViewById<View>(R.id.fabButton) as FloatingActionButton
        mFab.setOnClickListener {
            // Request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }

            fusedLocationProviderClient?.getLastLocation()
                    ?.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            //val clocation = LatLng(location.latitude, location.longitude)
                            var addresses: List<Address?> = java.util.ArrayList()

                            try {
                                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                //val address = addresses[0]!!.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                                val city = addresses[0]!!.locality
                                val state = addresses[0]!!.adminArea
                                val country = addresses[0]!!.countryName
                                val postalCode = addresses[0]!!.postalCode
                                val knownName = addresses[0]!!.featureName

                                mState = state // Save state for anonymous inner class

                                if (knownName != null && state != null && country != null) {
                                    // Eye lens view setup
                                    val user = auth.currentUser
                                    val uid = user?.uid
                                    if (uid != null) {
                                        db.collection("subscription")
                                                .whereEqualTo("uid", uid)
                                                //.orderBy("", Query.Direction.DESCENDING)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    if (!result.isEmpty) {
                                                        for (document in result) {
                                                            val uid = document.getString("uid").toString()
                                                            val agencyID = document.getString("agencyID").toString()
                                                            if (!uid.isEmpty() && !agencyID.isEmpty()) {
                                                                db.collection("agency")
                                                                        //.whereEqualTo("name", agencyID)
                                                                        //.orderBy("", Query.Direction.DESCENDING)
                                                                        .get()
                                                                        .addOnSuccessListener { res ->
                                                                            if (!res.isEmpty) {
                                                                                for (doc in res) {
                                                                                    eyelensView = doc.getString("eyelensView").toString()
                                                                                    //val agencyID = doc.getString("agencyID").toString()
                                                                                }

                                                                            }
                                                                        }
                                                            }
                                                        }

                                                        // Trigger camera to record eye lens view
                                                        if (::eyelensView.isInitialized){
                                                            if (!eyelensView.isEmpty() && eyelensView.equals("enabled", true)) {
                                                                // Pass values to showReportDialogHome
                                                                showEyelensCamera("", "", knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude, state, country)
                                                                //Toast.makeText(this@MapActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()
                                                            } else {
                                                                Toast.makeText(mContext, "Permission Denied. Eye Lens View have not yet been enabled. Please check back later.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }

                                                    } else {
                                                        defaultMessageTextView.text = "Nothing to show yet... To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View."
                                                        Toast.makeText(mContext, "To capture and access Eye Lens View, you must subscribe to an agency that support Eye Lens View.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Sorry ♥, you select an unknown address. Please try again.", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }

                                //mMap.addMarker(MarkerOptions().position(location).title(knownName.toString() +""+", "+ city))

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    })

            //val location = LatLng(lat, lon)
            //mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM.toFloat()))
        }


    }


    // Duplicate method for instant incident reporting
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showEyelensCamera(title: String, desc: String, address: String, lat: Double, lon: Double, state: String, country: String) {
        val user = auth.currentUser!!
        if (user != null) {
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, VIDEO)
                }
            }

            val crimetype = TOC_EYELENS
            var current = LocalDateTime.now()
            var fullLocaleFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
            var fullLocaleTime = current.format(fullLocaleFormat) // Retrieve only date

            //showUploadDialog(title, desc, address, crimetype, fullLocaleTime, lat, lon)

            val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
            // Add data to firebase
            addDataToFirebase(user!!.uid, crimetype, title, desc, address, fullLocaleTime, lat, lon, mStamp.toString(), "video", "", state, country)
        } else {
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Sorry, something went wrong. Login again.", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }

    }


    private fun addDataToFirebase(ID: String, mType: String, Title: String, Desc: String, Address: String, mDate: String, Latitude: Double, Longitude: Double, mEvid: String, MimeType: String, mReportedTo: String, mState: String, mCountry: String) {
        mIncident.setIncidentID(ID)
        mIncident.setIncidentType(mType)
        mIncident.setIncidentTitle(Title)
        mIncident.setIncidentDesc(Desc)
        mIncident.setIncidentAddress(Address)
        mIncident.setIncidentDate(mDate)
        mIncident.setIncidentLatitude(Latitude)
        mIncident.setIncidentLongitude(Longitude)
        mIncident.setIncidentMimeType(MimeType)
        mIncident.setIncidentEvidenceURL(mEvid)
        mIncident.setIncidentReportedTo(mReportedTo)
        mIncident.setIncidentState(mState)
        mIncident.setIncidentCountry(mCountry)

        val id = mIncident.getIncidentID()
        val type = mIncident.getIncidentType()
        val title = mIncident.getIncidentTitle()
        val desc = mIncident.getIncidentDesc()
        val address = mIncident.getIncidentAddress()
        val date = mIncident.getIncidentDate()
        val lat = mIncident.getIncidentLatitude()
        val lon = mIncident.getIncidentLongitude()
        val mimetype = mIncident.getIncidentMimeType()
        val evidence = mIncident.getIncidentEvidenceURL()
        val reportedTo = mIncident.getIncidentReportedTo()
        val state = mIncident.getIncidentState()
        val country = mIncident.getIncidentCountry()

        val incident = hashMapOf(
                "id" to id,
                "type" to type,
                "title" to title,
                "description" to desc,
                "address" to address,
                "mimetype" to mimetype,
                "date" to date,
                "latitude" to lat,
                "longitude" to lon,
                "evidenceURL" to evidence,
                "documentID" to "",
                "subType" to "",
                "reportedTo" to reportedTo,
                "status" to "Eyelens View",
                "state" to state,
                "country" to country,
                "visibility" to "public",
                "notifyStatus" to "unread",
                "reviewedBy" to "",
                "reviewInfo" to "The following complaint is an Eye Lens View instant report which will disappear in 24hrs."+"\n"+address.toString(),
                "weatherDesc" to "",
                "weatherTemp" to "",
                "weatherTempMax" to "",
                "weatherTempMin" to "",
                "weatherClouds" to "",
                "weatherFeelsLike" to "",
                "weatherPressure" to "",
                "weatherWindSpeed" to "",
                "weatherWindAngle" to "",
                "weatherHumidity" to "",
                "weatherSunrise" to "",
                "weatherSunset" to ""
        )

            // Add a new document with a generated ID
            db.collection("eyelens")
                    .add(incident)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                        // Update the collection and add docuemnt ID
                        val mDocumentID = documentReference.id
                        val modifyRef = db.collection("eyelens").document(documentReference.id)
                        modifyRef.update("documentID", mDocumentID)
                                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                        val parentLayout: View = findViewById(android.R.id.content)
                        Snackbar.make(parentLayout, "Sorry ♥, something went wrong. Try again.", Snackbar.LENGTH_LONG)
                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                .show()

                    }



    }


        fun downloadVideoToLocalFile(evidURL: String) {
            if (evidURL != null) {

                mRefVideo = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(this)
                progress.setTitle("Loading....")
                progress.show()

                val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
                val rootPath = File(mContext.filesDir, "EyeWitness")
                val subPath = File(rootPath, "Video")
                if (!subPath.exists()) {
                    subPath.mkdirs();
                }
                val localFile = File(subPath, mStamp.toString() + ".mp4")

                if (!localFile.canRead()) {
                    try {
                        mRefVideo.getFile(localFile)
                                .addOnSuccessListener {
                                    if (localFile.canRead()) {
                                        progress.dismiss()
                                        startActivity(Intent(this, ExoActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))

                                    }
                                }
                                .addOnFailureListener { exception ->

                                    Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                                }
                                .addOnProgressListener { taskSnapshot ->
                                    // progress percentage
                                    val progres_time = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                                    progress.setMessage("Please wait " + progres_time.toInt() + " %")
                                }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                } else {

                    progress.dismiss()
                    startActivity(Intent(this, ExoActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))
                }


            } else {
                Toast.makeText(this, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }

        }


        fun downloadImageToLocalFile(evidURL: String) {
            if (evidURL != null) {

                mRefPhoto = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(this)
                progress.setTitle("Loading....")
                progress.show()

                val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
                val rootPath = File(mContext.filesDir, "EyeWitness")
                val subPath = File(rootPath, "Image")
                if (!subPath.exists()) {
                    subPath.mkdirs();
                }
                val localFile = File(subPath, mStamp.toString() + ".jpg")

                try {
                    mRefPhoto.getFile(localFile)
                            .addOnSuccessListener {
                                if (localFile.canRead()) {
                                    progress.dismiss()
                                    startActivity(Intent(this, ImageActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))

                                }
                            }
                            .addOnFailureListener { exception ->

                                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                            }
                            .addOnProgressListener { taskSnapshot ->
                                // progress percentage
                                val progres_time = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                                progress.setMessage("Please wait " + progres_time.toInt() + " %")
                            }
                } catch (e: IOException) {
                    e.printStackTrace()
                }


            } else {
                Toast.makeText(this, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }


        }


        // [START on_start_check_user]
        override fun onStart() {
            super.onStart()
            // Check if user is signed in (non-null) and update UI accordingly.
            val currentUser = auth.currentUser
            updateUI(currentUser)

        }
        // [END on_start_check_user]


        private fun updateUI(user: FirebaseUser?) {

        }

    override fun onStop() {
        super.onStop()
    }


    private fun uploadVideo() {
        if (filePath != null) {
            val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())
            mRefVideo = mStorageRef.child("eyewitness/evidence/video/" + UUID.randomUUID().toString())
            val mPath: AssetFileDescriptor = applicationContext.contentResolver.openAssetFileDescriptor(filePath, "r")!!
            // Get length of file in Bytes
            val mByteSize: Long = mPath.length
            // Convert the bytes to Kilobytes
            val mKBSize: Long = mByteSize / 1024
            // Convert the kilobytes to Megabytes
            val mMBSize: Long = mKBSize / 1024
            // Convert the megabytes to Gigabytes
            val mGBSize: Long = mMBSize / 1024

            val currentUserId = auth.currentUser?.uid.toString()
            val userStatus = "user"
            if (userStatus.equals("user", true)) {
                db.collection("users")
                        .whereEqualTo("uid", currentUserId)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID")
                                val storageCap: Long? = document.getString("storageCap")?.toLong()
                                val storageUsed: Long? = document.getString("storageUsed")?.toLong()
                                val sCapSize: Long? = storageCap
                                val sUsedSize: Long? = storageUsed
                                if (mMBSize < sCapSize!!) {
                                    if (sUsedSize!! < sCapSize) {
                                        val progress = ProgressDialog(this)
                                        progress.setTitle("Uploading....")
                                        progress.show()
                                        mRefVideo.putFile(filePath).addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot?> {
                                            override fun onSuccess(p0: UploadTask.TaskSnapshot?) {
                                                progress.dismiss()
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Uploaded successfully.", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()

                                                // Modigy Used Storage
                                                val modify = db.collection("users").document(documentID.toString())
                                                val mTotalStorageUsed = mMBSize + sUsedSize
                                                val mUpdatedStorageUsed = mTotalStorageUsed.toString()
                                                modify.update("storageUsed", mUpdatedStorageUsed)
                                                        .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                                        .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                                mRefVideo.downloadUrl.addOnSuccessListener { evidURL ->
                                                    if (evidURL != null) {
                                                        // Update the collection and add evidenceURL
                                                        val type = mIncident.getIncidentType().toString().toLowerCase()
                                                        // init for metadata
                                                        val latmeta = mIncident.getIncidentLatitude()
                                                        val lonmeta = mIncident.getIncidentLongitude()
                                                        val addsmeta = mIncident.getIncidentAddress().toString()
                                                        val datemeta = mIncident.getIncidentDate().toString()
                                                        val mimetype = mIncident.getIncidentMimeType().toString()
                                                        val typemeta = mIncident.getIncidentType().toString() +" Incident Report"
                                                        val evidmeta = evidURL.toString()

                                                        /* Obtain the public key material to initiate the encryption of metadata.
                                                        val publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
                                                        // Get the primitive. For Encryption
                                                        val hybridEncrypt: HybridEncrypt = publicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
                                                        // Use the primitive and encrypt the metatdata
                                                        val encryptedlat = hybridEncrypt.encrypt(latmeta.toString().toByteArray(), latmeta.toString().toByteArray())
                                                        val encryptedlon = hybridEncrypt.encrypt(lonmeta.toString().toByteArray(), lonmeta.toString().toByteArray())
                                                        val encryptedadds = hybridEncrypt.encrypt(addsmeta.toByteArray(), addsmeta.toByteArray())
                                                        val encrypteddate = hybridEncrypt.encrypt(datemeta.toByteArray(), datemeta.toByteArray())
                                                        val encryptedmime = hybridEncrypt.encrypt(mimetype.toByteArray(), mimetype.toByteArray())
                                                        val encryptedtype = hybridEncrypt.encrypt(typemeta.toByteArray(), typemeta.toByteArray())
                                                        val encryptedevid = hybridEncrypt.encrypt(evidmeta.toByteArray(), evidmeta.toByteArray())

                                                        // Creating and applying the encryptted metadata to the evidence file
                                                        val mStorageMeta = StorageMetadata.Builder().setCustomMetadata("coordinates", encryptedlat.toString()+", "+encryptedlon.toString())
                                                        .setCustomMetadata("address", encryptedadds.toString()).setCustomMetadata("date", encrypteddate.toString()).setCustomMetadata("incident", encryptedtype.toString())
                                                                .setCustomMetadata("mimetype", encryptedmime.toString()).setCustomMetadata("evidence", encryptedevid.toString())
                                                                .build()

                                                        // Update the evidence file with the above metadata
                                                        mRefVideo.updateMetadata(mStorageMeta).addOnSuccessListener { updatedMetadata ->
                                                                // Evidence file has been updated with metadata
                                                                //Toast.makeText(mContext, updatedMetadata.toString(), Toast.LENGTH_LONG).show()
                                                        }.addOnFailureListener {
                                                                // Uh-oh, an error occurred!
                                                        }
                                                        */

                                                        // Start an asynchronous task for the weather api
                                                        var mGenerateURL = "https://api.openweathermap.org/data/2.5/weather?lat="+latmeta+"&lon="+lonmeta+"&appid="+getString(R.string.weather_api_key)+"&units=metric"
                                                        //mGenerateURL = mGenerateURL.replace(" ".toRegex(), "%20")

                                                        db.collection(type)
                                                                .whereEqualTo("evidenceURL", mIncident.getIncidentEvidenceURL().toString())
                                                                .get()
                                                                .addOnSuccessListener { result ->
                                                                    for (document in result) {
                                                                        //Log.d(TAG, "${document.id} => ${document.data}")
                                                                        val documentID = document.getString("documentID")
                                                                        val mEvidURL = evidURL.toString() + "-video"
                                                                        val modify = db.collection(type).document(document.id)
                                                                        modify.update("evidenceURL", mEvidURL)
                                                                                .addOnSuccessListener {
                                                                                    Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                    // Init metadata for the following file incident

                                                                                    // Setting up a HTTP Get Request to obtain the weather api json response
                                                                                }
                                                                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                                                        // Retrieve wheather information
                                                                        AndroidNetworking.get(mGenerateURL).setPriority(Priority.MEDIUM).build()
                                                                                .getAsJSONObject(object: JSONObjectRequestListener {
                                                                                    override fun onResponse(response: JSONObject) {
                                                                                        try {

                                                                                            // Retrieve the Generated JSONObject & JSONArray
                                                                                            val mWeather = response.getJSONArray("weather").getJSONObject(0)
                                                                                            val mWind = response.getJSONObject("wind")
                                                                                            val mMain = response.getJSONObject("main")
                                                                                            val mSys = response.getJSONObject("sys")
                                                                                            val mSky = response.getJSONObject("clouds")

                                                                                            val mDesc = mWeather.getString("description")
                                                                                            val mTemp = mMain.getString("temp") + "°C"
                                                                                            val mPressure = mMain.getString("pressure") + "hPs"
                                                                                            val mHumidity = mMain.getString("humidity") + " %"
                                                                                            val mWindSpeed = mWind.getString("speed") + " m/s"
                                                                                            val mWindAngle = mWind.getString("deg") + " degrees"
                                                                                            val mFeelsLike = mMain.getString("feels_like") + "°C"
                                                                                            val mTempMin = mMain.getString("temp_min") + "°C"
                                                                                            val mTempMax = mMain.getString("temp_max") + "°C"
                                                                                            val mSunrise = mSys.getString("sunrise")
                                                                                            val mSunset = mSys.getString("sunset")
                                                                                            val mCloud = mSky.getString("all") + " %"



                                                                                            val modify = db.collection(type).document(document.id)
                                                                                            modify.update("weatherDesc", mDesc)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Get user status from an input string extra
                                                                                                        val mStatus = "user"
                                                                                                        // Create an array and pass user credentials via intent put extra
                                                                                                        val array = arrayOf(type, mEvidURL, documentID, mStatus) // Pass to an array of string
                                                                                                        // Start activity to describe incident
                                                                                                        //startActivity(Intent(mContext, DescribeIncident::class.java).putExtra("Tag", array))
                                                                                                        Toast.makeText(mContext, "Uploaded successfully", Toast.LENGTH_LONG).show()


                                                                                                    }
                                                                                                    .addOnFailureListener {
                                                                                                        // Get user status from an input string extra
                                                                                                        val mStatus = "user"
                                                                                                        // Create an array and pass user credentials via intent put extra
                                                                                                        val array = arrayOf(type, mEvidURL, documentID, mStatus) // Pass to an array of string
                                                                                                        // Start activity to describe incident
                                                                                                        //startActivity(Intent(mContext, DescribeIncident::class.java).putExtra("Tag", array))
                                                                                                        Toast.makeText(mContext, "Uploaded successfully", Toast.LENGTH_LONG).show()
                                                                                                    }

                                                                                            modify.update("weatherTemp", mTemp)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherPressure", mPressure)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherHumidity", mHumidity)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherWindSpeed", mWindSpeed)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}


                                                                                            modify.update("weatherWindAngle", mWindAngle)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherFeelsLike", mFeelsLike)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherTempMin", mTempMin)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherTempMax", mTempMax)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherSunrise", mSunrise)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherSunset", mSunset)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherClouds", mCloud)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}




                                                                                            //Toast.makeText(mContext, mTemp, Toast.LENGTH_LONG).show()

                                                                                        } catch (e: Exception) {
                                                                                            Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show()
                                                                                        }
                                                                                    }

                                                                                    override fun onError(anError: ANError?) {
                                                                                        Toast.makeText(mContext, anError.toString(), Toast.LENGTH_LONG).show()
                                                                                        // Get user status from an input string extra
                                                                                        val mStatus = "user"
                                                                                        // Create an array and pass user credentials via intent put extra
                                                                                        val array = arrayOf(type, mEvidURL, documentID, mStatus) // Pass to an array of string
                                                                                        // Start activity to describe incident
                                                                                        //startActivity(Intent(mContext, DescribeIncident::class.java).putExtra("Tag", array))
                                                                                        Toast.makeText(mContext, "Uploaded successfully", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                })


                                                                    }
                                                                }
                                                                .addOnFailureListener { exception ->
                                                                    //Log.d(TAG, "Error getting documents: ", exception)
                                                                }
                                                    }
                                                }.addOnFailureListener { err ->
                                                    Toast.makeText(this@EyelensActivity, err.toString(), Toast.LENGTH_LONG).show()
                                                }

                                            }
                                        }).addOnFailureListener(object : OnFailureListener {
                                            override fun onFailure(@NonNull e: Exception) {
                                                progress.dismiss()
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Sorry ♥, something went wrong. Try again.", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()

                                            }
                                        }).addOnProgressListener(object : OnProgressListener<UploadTask.TaskSnapshot?> {
                                            override fun onProgress(taskSnapshot: UploadTask.TaskSnapshot) {
                                                val progres_time = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                                                progress.setMessage("Uploaded " + progres_time.toInt() + " %")

                                            }
                                        })

                                    } else {
                                        val parentLayout: View = findViewById(android.R.id.content)
                                        Snackbar.make(parentLayout, "Insufficient Memory! Buy storage", Snackbar.LENGTH_LONG)
                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                .show()
                                    }

                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Insufficient Memory! Buy storage", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }


                            }

                        }
                        .addOnFailureListener { e ->


                        }

                // Not useful anyway. Ignore the else statement
            } else if (userStatus.equals("agency", true)) {
                db.collection("agency")
                        .whereEqualTo("uid", currentUserId)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID")
                                val storageCap: Long? = document.getString("storageCap")?.toLong()
                                val storageUsed: Long? = document.getString("storageUsed")?.toLong()
                                val sCapSize: Long? = storageCap
                                val sUsedSize: Long? = storageUsed
                                if (mMBSize < sCapSize!!) {
                                    if (sUsedSize!! < sCapSize) {
                                        val progress = ProgressDialog(this)
                                        progress.setTitle("Uploading....")
                                        progress.show()
                                        mRefVideo.putFile(filePath).addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot?> {
                                            override fun onSuccess(p0: UploadTask.TaskSnapshot?) {
                                                progress.dismiss()
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Uploaded successfully", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()

                                                // Modify Used Storage
                                                val modify = db.collection("agency").document(documentID.toString())
                                                val mTotalStorageUsed = mMBSize + sUsedSize
                                                val mUpdatedStorageUsed = mTotalStorageUsed.toString()
                                                modify.update("storageUsed", mUpdatedStorageUsed)
                                                        .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                                        .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                                mRefVideo.downloadUrl.addOnSuccessListener { evidURL ->
                                                    if (evidURL != null) {
                                                        // Update the collection and add evidenceURL
                                                        val type = mIncident.getIncidentType().toString().toLowerCase()
                                                        // init for metadata
                                                        val latmeta = mIncident.getIncidentLatitude()
                                                        val lonmeta = mIncident.getIncidentLongitude()
                                                        val addsmeta = mIncident.getIncidentAddress().toString()
                                                        val datemeta = mIncident.getIncidentDate().toString()
                                                        val mimetype = mIncident.getIncidentMimeType().toString()
                                                        val typemeta = mIncident.getIncidentType().toString() +" Incident Report"
                                                        val evidmeta = evidURL.toString()

                                                        /* Obtain the public key material to initiate the encryption of metadata.
                                                        val publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
                                                        // Get the primitive. For Encryption
                                                        val hybridEncrypt: HybridEncrypt = publicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
                                                        // Use the primitive and encrypt the metatdata
                                                        val encryptedlat = hybridEncrypt.encrypt(latmeta.toString().toByteArray(), latmeta.toString().toByteArray())
                                                        val encryptedlon = hybridEncrypt.encrypt(lonmeta.toString().toByteArray(), lonmeta.toString().toByteArray())
                                                        val encryptedadds = hybridEncrypt.encrypt(addsmeta.toByteArray(), addsmeta.toByteArray())
                                                        val encrypteddate = hybridEncrypt.encrypt(datemeta.toByteArray(), datemeta.toByteArray())
                                                        val encryptedmime = hybridEncrypt.encrypt(mimetype.toByteArray(), mimetype.toByteArray())
                                                        val encryptedtype = hybridEncrypt.encrypt(typemeta.toByteArray(), typemeta.toByteArray())
                                                        val encryptedevid = hybridEncrypt.encrypt(evidmeta.toByteArray(), evidmeta.toByteArray())

                                                        // Creating and applying the encryptted metadata to the evidence file
                                                        val mStorageMeta = StorageMetadata.Builder().setCustomMetadata("coordinates", encryptedlat.toString()+", "+encryptedlon.toString())
                                                        .setCustomMetadata("address", encryptedadds.toString()).setCustomMetadata("date", encrypteddate.toString()).setCustomMetadata("incident", encryptedtype.toString())
                                                                .setCustomMetadata("mimetype", encryptedmime.toString()).setCustomMetadata("evidence", encryptedevid.toString())
                                                                .build()

                                                        // Update the evidence file with the above metadata
                                                        mRefVideo.updateMetadata(mStorageMeta).addOnSuccessListener { updatedMetadata ->
                                                                // Evidence file has been updated with metadata
                                                                //Toast.makeText(mContext, updatedMetadata.toString(), Toast.LENGTH_LONG).show()
                                                        }.addOnFailureListener {
                                                                // Uh-oh, an error occurred!
                                                        }
                                                        */

                                                        // Start an asynchronous task for the weather api
                                                        var mGenerateURL = "https://api.openweathermap.org/data/2.5/weather?lat="+latmeta+"&lon="+lonmeta+"&appid="+getString(R.string.weather_api_key)+"&units=metric"
                                                        //mGenerateURL = mGenerateURL.replace(" ".toRegex(), "%20")

                                                        db.collection(type)
                                                                .whereEqualTo("evidenceURL", mIncident.getIncidentEvidenceURL().toString())
                                                                .get()
                                                                .addOnSuccessListener { result ->
                                                                    for (document in result) {
                                                                        //Log.d(TAG, "${document.id} => ${document.data}")
                                                                        val documentID = document.getString("documentID")
                                                                        val mEvidURL = evidURL.toString() + "-video"
                                                                        val modify = db.collection(type).document(document.id)
                                                                        modify.update("evidenceURL", mEvidURL)
                                                                                .addOnSuccessListener {
                                                                                    Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                    // Init metadata for the following file incident

                                                                                    // Setting up a HTTP Get Request to obtain the weather api json response
                                                                                }
                                                                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                                                        AndroidNetworking.get(mGenerateURL).setPriority(Priority.MEDIUM).build()
                                                                                .getAsJSONObject(object: JSONObjectRequestListener {
                                                                                    override fun onResponse(response: JSONObject) {
                                                                                        try {

                                                                                            // Retrieve the Generated JSONObject & JSONArray
                                                                                            val mWeather = response.getJSONArray("weather").getJSONObject(0)
                                                                                            val mWind = response.getJSONObject("wind")
                                                                                            val mMain = response.getJSONObject("main")
                                                                                            val mSys = response.getJSONObject("sys")
                                                                                            val mSky = response.getJSONObject("clouds")

                                                                                            val mDesc = mWeather.getString("description")
                                                                                            val mTemp = mMain.getString("temp") + "°C"
                                                                                            val mPressure = mMain.getString("pressure") + "hPs"
                                                                                            val mHumidity = mMain.getString("humidity") + " %"
                                                                                            val mWindSpeed = mWind.getString("speed") + " m/s"
                                                                                            val mWindAngle = mWind.getString("deg") + " degrees"
                                                                                            val mFeelsLike = mMain.getString("feels_like") + "°C"
                                                                                            val mTempMin = mMain.getString("temp_min") + "°C"
                                                                                            val mTempMax = mMain.getString("temp_max") + "°C"
                                                                                            val mSunrise = mSys.getString("sunrise")
                                                                                            val mSunset = mSys.getString("sunset")
                                                                                            val mCloud = mSky.getString("all") + " %"



                                                                                            val modify = db.collection(type).document(document.id)
                                                                                            modify.update("weatherDesc", mDesc)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Get user status from an input string extra
                                                                                                        val mStatus = intent.getStringExtra("Tag")
                                                                                                        // Create an array and pass user credentials via intent put extra
                                                                                                        val array = arrayOf(type, mEvidURL, documentID, mStatus) // Pass to an array of string
                                                                                                        // Start activity to describe incident
                                                                                                        //startActivity(Intent(mContext, DescribeIncident::class.java).putExtra("Tag", array))
                                                                                                        Toast.makeText(mContext, "Uploaded successfully", Toast.LENGTH_LONG).show()


                                                                                                    }
                                                                                                    .addOnFailureListener {
                                                                                                        // Get user status from an input string extra
                                                                                                        val mStatus = intent.getStringExtra("Tag")
                                                                                                        // Create an array and pass user credentials via intent put extra
                                                                                                        val array = arrayOf(type, mEvidURL, documentID, mStatus) // Pass to an array of string
                                                                                                        // Start activity to describe incident
                                                                                                        //startActivity(Intent(mContext, DescribeIncident::class.java).putExtra("Tag", array))
                                                                                                        Toast.makeText(mContext, "Uploaded successfully", Toast.LENGTH_LONG).show()
                                                                                                    }

                                                                                            modify.update("weatherTemp", mTemp)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherPressure", mPressure)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherHumidity", mHumidity)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherWindSpeed", mWindSpeed)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}


                                                                                            modify.update("weatherWindAngle", mWindAngle)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherFeelsLike", mFeelsLike)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherTempMin", mTempMin)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherTempMax", mTempMax)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherSunrise", mSunrise)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherSunset", mSunset)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}

                                                                                            modify.update("weatherClouds", mCloud)
                                                                                                    .addOnSuccessListener {
                                                                                                        //Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                                        // Init metadata for the following file incident


                                                                                                    }
                                                                                                    .addOnFailureListener { e -> /*Log.w(TAG, "Error writing document", e) */}




                                                                                            //Toast.makeText(mContext, mTemp, Toast.LENGTH_LONG).show()

                                                                                        } catch (e: Exception) {
                                                                                            Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show()
                                                                                        }
                                                                                    }

                                                                                    override fun onError(anError: ANError?) {
                                                                                        Toast.makeText(mContext, anError.toString(), Toast.LENGTH_LONG).show()
                                                                                        // Get user status from an input string extra
                                                                                        val mStatus = intent.getStringExtra("Tag")
                                                                                        // Create an array and pass user credentials via intent put extra
                                                                                        val array = arrayOf(type, mEvidURL, documentID, mStatus) // Pass to an array of string
                                                                                        // Start activity to describe incident
                                                                                        //startActivity(Intent(mContext, DescribeIncident::class.java).putExtra("Tag", array))
                                                                                        Toast.makeText(mContext, "Uploaded successfully", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                })

                                                                    }
                                                                }
                                                                .addOnFailureListener { exception ->
                                                                    //Log.d(TAG, "Error getting documents: ", exception)
                                                                }
                                                    }
                                                }.addOnFailureListener { err ->
                                                    Toast.makeText(this@EyelensActivity, err.toString(), Toast.LENGTH_LONG).show()
                                                }

                                            }
                                        }).addOnFailureListener(object : OnFailureListener {
                                            override fun onFailure(@NonNull e: Exception) {
                                                progress.dismiss()
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Sorry ♥, something went wrong. Try again.", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()

                                            }
                                        }).addOnProgressListener(object : OnProgressListener<UploadTask.TaskSnapshot?> {
                                            override fun onProgress(taskSnapshot: UploadTask.TaskSnapshot) {
                                                val progres_time = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                                                progress.setMessage("Uploaded " + progres_time.toInt() + " %")

                                            }
                                        })

                                    } else {
                                        val parentLayout: View = findViewById(android.R.id.content)
                                        Snackbar.make(parentLayout, "Insufficient Memory! Buy storage", Snackbar.LENGTH_LONG)
                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                .show()
                                    }

                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Insufficient Memory! Buy storage", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }


                            }

                        }
                        .addOnFailureListener { e ->


                        }
            }

        }
    }

        @SuppressLint("RestrictedApi")
        private fun showStorageDialog() {
            lateinit var selectedSpinItem: String
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popUpView = inflater.inflate(R.layout.layout_storage, null)

            val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
            colorDrawable.alpha = 70

            mStorageDialogPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT)
            mStorageDialogPopUp.setBackgroundDrawable(colorDrawable)
            mStorageDialogPopUp.isOutsideTouchable = true


            val btnPremium = popUpView.findViewById<View>(R.id.btn_prem) as AppCompatButton
            val btnUltimate = popUpView.findViewById<View>(R.id.btn_ulti) as AppCompatButton

            btnPremium.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnPremium.setTextColor(Color.BLACK)
            btnUltimate.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnUltimate.setTextColor(Color.BLACK)

            val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton

            btnConfirm.background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                            ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

            if (Build.VERSION.SDK_INT >= 21) {
                mStorageDialogPopUp.setElevation(5.0f)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mStorageDialogPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
            } else {
                mStorageDialogPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
            }

            btnPremium.setOnClickListener{
                btnPremium.setBackgroundColor(Color.parseColor("#2196F3"))
                btnPremium.setTextColor(Color.WHITE)
                btnUltimate.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnUltimate.setTextColor(Color.BLACK)

                val spinStorage = popUpView.findViewById<View>(R.id.spin_storage) as Spinner
                // Assign list of storage bundle to the spinner
                val items = arrayOf("Choose Bundle", "ÒmÌnÌra", "Aradu", "Lagwada", "Alikwara") // Asusu: 1GB, Aradu: 2GB, Lagwada: 4GB, Alikwara: 8GB
                getContentView(popUpView)?.let {
                    ArrayAdapter<String>(mContext,
                            android.R.layout.simple_spinner_item, items)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinStorage.adapter = adapter
                                spinStorage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItem = selectedItem
                                        if (selectedSpinItem != null && selectedSpinItem.equals("ÒmÌnÌra", true)) {
                                            val mAsusuBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_asusu), "1GB", "300", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mAsusuBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else if (selectedSpinItem != null && selectedSpinItem.equals("Aradu", true)) {
                                            val mAraduBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_aradu), "2GB", "500", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mAraduBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else if (selectedSpinItem != null && selectedSpinItem.equals("Lagwada", true)) {
                                            val mLagwadaBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_lagwada), "4GB", "800", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mLagwadaBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else if (selectedSpinItem != null && selectedSpinItem.equals("Alikwara", true)) {
                                            val mAlikwaraBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_alikwara), "8GB", "1500", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mAlikwaraBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(mContext, "Select a storage bundle", Toast.LENGTH_LONG).show()
                                        }

                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }

            }


            btnUltimate.setOnClickListener{
                btnUltimate.setBackgroundColor(Color.parseColor("#2196F3"))
                btnUltimate.setTextColor(Color.WHITE)
                btnPremium.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnPremium.setTextColor(Color.BLACK)

                val spinStorage = popUpView.findViewById<View>(R.id.spin_storage) as Spinner
                // Assign list of storage bundle to the spinner
                val items = arrayOf("Choose Bundle", "Linzami", "Gidanwaya", "Raminkura", "Gwantanamo") // Hawainiya: 32GB, Gidanwaya: 64GB, Raminkura: 128GB, Gwantanamo: 256GB
                getContentView(popUpView)?.let {
                    ArrayAdapter<String>(mContext,
                            android.R.layout.simple_spinner_item, items)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinStorage.adapter = adapter
                                spinStorage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItem = selectedItem
                                        if (selectedSpinItem != null && selectedSpinItem.equals("Linzami", true)) {
                                            val mHawainiyaBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_hawainiya), "32GB", "9600", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mHawainiyaBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else if (selectedSpinItem != null && selectedSpinItem.equals("Gidanwaya", true)) {
                                            val mEagleEyeBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_eagle_eye), "64GB", "16000", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mEagleEyeBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else if (selectedSpinItem != null && selectedSpinItem.equals("Raminkura", true)) {
                                            val mRaminkuraBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_raminkura), "128GB", "25600", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mRaminkuraBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else if (selectedSpinItem != null && selectedSpinItem.equals("Gwantanamo", true)) {
                                            val mGwantanamoBundle = arrayOf(selectedSpinItem, getString(R.string.bundle_gwantanamo), "256GB", "48000", "agency")
                                            startActivity(Intent(mContext, BuyStorageActivity::class.java).putExtra("Tag", mGwantanamoBundle))
                                            Toast.makeText(mContext, selectedSpinItem, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(mContext, "Select a storage bundle", Toast.LENGTH_LONG).show()
                                        }

                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }
            }


            btnConfirm.setOnClickListener {
                mStorageDialogPopUp.dismiss()
            }

        }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //val uriTxt = findViewById<View>(R.id.uriTxt) as TextView
        if (resultCode == RESULT_OK) {
            if (requestCode == PDF) {
                filePath = data!!.data!!
                //uploadPdf()
            } else if (requestCode == DOCX) {
                filePath = data!!.data!!
                //uploadDoc()
            } else if (requestCode == AUDIO) {
                filePath = data!!.data!!
                //uploadAudio()
            } else if (requestCode == VIDEO) {
                filePath = data!!.data!!
                uploadVideo()
            } else if (requestCode == PHOTO) {
                filePath = data!!.data!!
                //uploadPhoto()
            }
        }

    }



}
