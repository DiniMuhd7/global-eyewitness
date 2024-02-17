package com.dinisoft.eyewitness

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
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
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.dinisoft.eyewitness.adapters.RedListAdapter
import com.dinisoft.eyewitness.model.Incident
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.internal.ViewUtils
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
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_red_list_add.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class RedListAddActivity : AppCompatActivity(){

    private val TAG: String = "--RedListActivity"

    // firebase auth, database and incident object
    lateinit var auth: FirebaseAuth
    lateinit var db: FirebaseFirestore
    lateinit var mIncident: Incident
    lateinit var mState: String // For security check
    lateinit var mCountry: String
    lateinit var mGlobal: String

    private val mIncidentMute = mutableListOf<Incident>()

    private lateinit var mRedListAdapter: RedListAdapter
    private lateinit var mContext: Context
    private lateinit var mStorageDialogPopUp: PopupWindow

    lateinit var mStorage: FirebaseStorage
    lateinit var mStorageRef: StorageReference
    lateinit var mRefVideo: StorageReference
    lateinit var mRefAudio: StorageReference
    lateinit var mRefPhoto: StorageReference
    lateinit var mRefPDF: StorageReference
    lateinit var mRefDOC: StorageReference

    val TOC_REDLIST = "Redlist"


    val PDF: Int = 0
    val DOCX: Int = 1
    val AUDIO: Int = 2
    val VIDEO: Int = 3
    val PHOTO: Int = 4
    val CSVF: Int = 5

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
        setContentView(R.layout.activity_red_list_add)
        mContext = this
        // Initialize Firebase Auth
        auth = Firebase.auth
        db = Firebase.firestore

        mStorage = FirebaseStorage.getInstance()
        mStorageRef = mStorage.getReference()

        val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())
        //mRefPhoto = mStorageRef.child("eyewitness/evidence/image/" + UUID.randomUUID().toString())
        mRefVideo = mStorageRef.child("eyewitness/evidence/video/" + UUID.randomUUID().toString())
        mRefAudio = mStorageRef.child("eyewitness/evidence/audio/" + UUID.randomUUID().toString())
        mRefDOC = mStorageRef.child("eyewitness/evidence/docx/" + UUID.randomUUID().toString())
        mRefPDF = mStorageRef.child("eyewitness/evidence/pdf/" + UUID.randomUUID().toString())

        mIncident = Incident()

        geocoder = Geocoder(this@RedListAddActivity, Locale.getDefault())
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext as RedListAddActivity)

        val etPersonName = findViewById<View>(R.id.txtPersonName) as EditText
        val etPersonDOB = findViewById<View>(R.id.txtPersonDOB) as EditText
        val etPersonSex = findViewById<View>(R.id.txtPersonSex) as EditText
        val etPersonRace = findViewById<View>(R.id.txtPersonRace) as EditText
        val etPersonBodyType = findViewById<View>(R.id.txtPersonBodyType) as EditText
        val etPersonBodyMark = findViewById<View>(R.id.txtPersonBodyMark) as EditText
        val etPersonHairType = findViewById<View>(R.id.txtPersonHairType) as EditText
        val etPersonOccupation = findViewById<View>(R.id.txtPersonOccupation) as EditText
        val etPersonLastAddress = findViewById<View>(R.id.txtPersonLastAddress) as EditText
        val etPersonStatus = findViewById<View>(R.id.txtPersonStatus) as EditText
        val etPersonDesc = findViewById<View>(R.id.personDesc) as EditText
        val etPersonCaseDetails = findViewById<View>(R.id.personCaseDetails) as EditText
        val etPersonRewardBounty = findViewById<View>(R.id.personRewardBounty) as EditText

        val userDp = findViewById<View>(R.id.userDp) as ImageView
        val txtStorageUsed = findViewById<View>(R.id.txt_storage_used) as TextView
        val txtStorageCap = findViewById<View>(R.id.txt_storage_cap) as TextView

        val btnState = findViewById<View>(R.id.btn_state) as AppCompatButton
        val btnCountry = findViewById<View>(R.id.btn_country) as AppCompatButton
        val btnGlobal = findViewById<View>(R.id.btn_global) as AppCompatButton

        // Show buy storage bundle dialog
        val mCardStorage = findViewById<View>(R.id.card_item_userDp) as CardView
        mCardStorage.setOnClickListener { showStorageDialog() }


        btnState.setOnClickListener {
            btnState.setBackgroundColor(Color.parseColor("#2196F3"))
            btnState.setTextColor(Color.WHITE)
            btnGlobal.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnGlobal.setTextColor(Color.BLACK)
            btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
            btnCountry.setTextColor(Color.BLACK)


            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                                //mCountry = country

                                // Feed User Dashboard with Data
                                if (knownName != null && state != null && country != null) {
                                    //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                    //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                    // Format the action buttons
                                    btnState.text = state
                                    btnCountry.text = country



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

                                //mState = state // Save state for anonymous inner class
                                mCountry = country

                                // Feed User Dashboard with Data
                                if (knownName != null && state != null && country != null) {
                                    //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                    //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                    // Format the action buttons
                                    btnState.text = state
                                    btnCountry.text = country


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

                                //mState = state // Save state for anonymous inner class
                                //mCountry = country

                                // Feed User Dashboard with Data
                                if (knownName != null && state != null && country != null) {
                                    //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                    //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                    // Format the action buttons
                                    btnState.text = state
                                    btnCountry.text = country


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

        val mFabButton = findViewById<View>(R.id.fabButton) as FloatingActionButton
        mFabButton.setOnClickListener {
            val vPersonalName = etPersonName.text.toString()
            val vPersonalDOB = etPersonDOB.text.toString()
            val vPersonalRace = etPersonRace.text.toString()
            val vPersonalSex = etPersonSex.text.toString()
            val vPersonalBodyType = etPersonBodyType.text.toString()
            val vPersonalBodyMark = etPersonBodyMark.text.toString()
            val vPersonalHairType = etPersonHairType.text.toString()
            val vPersonalOccupation = etPersonOccupation.text.toString()
            val vPersonalStatus = etPersonStatus.text.toString()
            val vPersonalDesc = etPersonDesc.text.toString()
            val vPersonalLastAddress = etPersonLastAddress.text.toString()
            val vPersonalCaseDetails = etPersonCaseDetails.text.toString()
            val vPersonalRewardBounty = etPersonRewardBounty.text.toString()

            if (!vPersonalName.isEmpty() || !vPersonalDOB.isEmpty() || !vPersonalRace.isEmpty() || !vPersonalBodyType.isEmpty() || !vPersonalBodyMark.isEmpty() || !vPersonalHairType.isEmpty() || !vPersonalOccupation.isEmpty() || !vPersonalStatus.isEmpty() || !vPersonalDesc.isEmpty() || !vPersonalCaseDetails.isEmpty() || !vPersonalRewardBounty.isEmpty() || !vPersonalSex.isEmpty() || !vPersonalLastAddress.isEmpty()) {
                // Show the gallery to select person or victim photograph
                val currentUser = auth.currentUser
                val authName = currentUser!!.displayName.toString()
                val uid = currentUser!!.uid
                if (uid != null && authName != null) {
                    if (::mState.isInitialized){
                        val vInfoRegion = mState
                        showRedListGallery(vPersonalName, vPersonalDOB, vPersonalSex, vPersonalRace, vPersonalBodyType, vPersonalBodyMark, vPersonalHairType, vPersonalOccupation, vPersonalLastAddress, vPersonalStatus, vPersonalDesc, vPersonalCaseDetails, vPersonalRewardBounty, vInfoRegion, authName)
                    } else if (::mCountry.isInitialized){
                        val vInfoRegion = mCountry
                        showRedListGallery(vPersonalName, vPersonalDOB, vPersonalSex, vPersonalRace, vPersonalBodyType, vPersonalBodyMark, vPersonalHairType, vPersonalOccupation, vPersonalLastAddress, vPersonalStatus, vPersonalDesc, vPersonalCaseDetails, vPersonalRewardBounty, vInfoRegion, authName)
                    } else {
                        val vInfoRegion = "Global"
                        showRedListGallery(vPersonalName, vPersonalDOB, vPersonalSex, vPersonalRace, vPersonalBodyType, vPersonalBodyMark, vPersonalHairType, vPersonalOccupation, vPersonalLastAddress, vPersonalStatus, vPersonalDesc, vPersonalCaseDetails, vPersonalRewardBounty, vInfoRegion, authName)
                    }
                }
            } else {
                // Show the gallery to select person or victim photograph
                val currentUser = auth.currentUser
                val authName = currentUser!!.displayName.toString()
                val uid = currentUser!!.uid
                if (uid != null && authName != null) {
                    if (::mState.isInitialized){
                        val vInfoRegion = mState
                        showRedListGallery(getString(R.string.vPName), getString(R.string.vPDOB), getString(R.string.vPGender), getString(R.string.vPRace), getString(R.string.vPBodyType), getString(R.string.vPBodyMark), getString(R.string.vPHairType), getString(R.string.vPOccupation), getString(R.string.vPLastAddress), getString(R.string.vPStatus), getString(R.string.vPDesc), getString(R.string.vPDetails), getString(R.string.vPReward), vInfoRegion, authName)
                    } else if (::mCountry.isInitialized){
                        val vInfoRegion = mCountry
                        showRedListGallery(getString(R.string.vPName), getString(R.string.vPDOB), getString(R.string.vPGender), getString(R.string.vPRace), getString(R.string.vPBodyType), getString(R.string.vPBodyMark), getString(R.string.vPHairType), getString(R.string.vPOccupation), getString(R.string.vPLastAddress), getString(R.string.vPStatus), getString(R.string.vPDesc), getString(R.string.vPDetails), getString(R.string.vPReward), vInfoRegion, authName)
                    } else {
                        val vInfoRegion = "Global"
                        showRedListGallery(getString(R.string.vPName), getString(R.string.vPDOB), getString(R.string.vPGender), getString(R.string.vPRace), getString(R.string.vPBodyType), getString(R.string.vPBodyMark), getString(R.string.vPHairType), getString(R.string.vPOccupation), getString(R.string.vPLastAddress), getString(R.string.vPStatus), getString(R.string.vPDesc), getString(R.string.vPDetails), getString(R.string.vPReward), vInfoRegion, authName)
                    }
                }
            }

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
            db.collection("agency")
                    .whereEqualTo("uid", uid)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {

                            val mName = document.getString("name").toString()
                            val mStorageUsed = document.getString("storageUsed")!!.toLong()
                            val mStorageCap = document.getString("storageCap")!!.toLong()

                            txt_title_dash.setText(mName)
                            txtStorageUsed.setText(mStorageUsed.toString() + " MB: Used")
                            txtStorageCap.setText(mStorageCap.toString() + " MB: Total")
                        }

                    }
        }


        // Format the buttons
        btnState.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
        btnState.setTextColor(Color.BLACK)
        btnGlobal.setBackgroundColor(Color.parseColor("#2196F3"))
        btnGlobal.setTextColor(Color.WHITE)
        btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
        btnCountry.setTextColor(Color.BLACK)


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

                            //mState = state // Save state for anonymous inner class
                            //mCountry = country

                            // Feed User Dashboard with Data
                            if (knownName != null && state != null && country != null) {
                                //showReportDialogHome("","",knownName + "" + ", " + city + "" + ", " + state + "" + ", " + country, location.latitude, location.longitude)
                                //Toast.makeText(this@DashUserActivity, knownName.toString() +""+", "+ city, Toast.LENGTH_LONG).show()

                                // Format the action buttons
                                btnState.text = state
                                btnCountry.text = country



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


    // Duplicate method for instant incident reporting
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showRedListGallery(pName: String, pDob: String, pSex: String, pRace: String, pBodyType: String, pBodyMark: String, pHairType: String, pOccupation: String, pLastAddress: String, pStatus: String, pDesc: String, pCaseDetails: String, pReward: String, pSeekInfoRegion: String, mAuthName: String) {
        val user = auth.currentUser!!
        val mAuthID = user.uid
        if (mAuthID != null) {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTO)

            val crimetype = TOC_REDLIST
            var current = LocalDateTime.now()
            var fullLocaleFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
            var fullLocaleTime = current.format(fullLocaleFormat) // Retrieve only date

            //showUploadDialog(title, desc, address, crimetype, fullLocaleTime, lat, lon)

            val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
            val mPhotoURL = mStamp
            // Add data to firebase
            addDataToFirebase(pName, pDob, pSex, pRace, pBodyType, pBodyMark, pHairType, pOccupation, pLastAddress, pStatus, pDesc, pCaseDetails, pReward, pSeekInfoRegion, mAuthID, mAuthName, mPhotoURL, fullLocaleTime)
        } else {
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Sorry, something went wrong. Login again.", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }

    }


    private fun addDataToFirebase(pName: String, pDob: String, pSex: String, pRace: String, pBodyType: String, pBodyMark: String, pHairType: String, pOccupation: String, pLastAddress: String, pStatus: String, pDesc: String, pCaseDetails: String, pReward: String, pSeekInfoRegion: String, mAuthID: String, mAuthName: String, mPhotoURL: String, mMetaTime: String) {
        mIncident.setIncidentEvidenceURL(mPhotoURL)
        val photoURL = mIncident.getIncidentEvidenceURL()
        val redlist = hashMapOf(
                "id" to mAuthID,
                "person_name" to pName,
                "person_dob" to pDob,
                "person_sex" to pSex,
                "person_race" to pRace,
                "person_bodytype" to pBodyType,
                "person_bodymark" to pBodyMark,
                "person_hairtype" to pHairType,
                "person_occupation" to pOccupation,
                "person_last_address" to pLastAddress,
                "documentID" to "",
                "person_status" to pStatus,
                "person_desc" to pDesc,
                "person_case_details" to pCaseDetails,
                "person_reward" to pReward,
                "person_photograph" to photoURL,
                "metatime" to mMetaTime,
                "person_seek_info_region" to pSeekInfoRegion,
                "notifyStatus" to "unread",
                "auth_name" to mAuthName,
                "alertInfo" to mAuthName+" is requesting for information from the public concerning the following person named "+pName+". If you have any credible information about the person in the picture below, kindly call us immediately. You will always remain anonymous."
        )

        // Add a new document with a generated ID
        db.collection("redlist")
                .add(redlist)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    // Update the collection and add docuemnt ID
                    val mDocumentID = documentReference.id
                    val modifyRef = db.collection("redlist").document(documentReference.id)
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //val uriTxt = findViewById<View>(R.id.uriTxt) as TextView
        if (resultCode == RESULT_OK) {
            if (requestCode == PHOTO) {
                filePath = data!!.data!!
                uploadPhoto()
            }
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
                subPath.mkdirs()
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



  private fun uploadPhoto() {
                if (filePath != null) {
                    val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())
                    mRefPhoto = mStorageRef.child("eyewitness/evidence/image/" + UUID.randomUUID().toString())
                    val mPath: AssetFileDescriptor = applicationContext.contentResolver.openAssetFileDescriptor(filePath, "r")!!
                    // Get length of file in Bytes
                    val mByteSize: Long = mPath.length
                    // Convert the bytes to Kilobytes
                    val mKBSize: Long = mByteSize / 1024
                    // Convert the kilobytes to Megabytes
                    val mMBSize: Long = mKBSize / 1024
                    // Convert the megabytes to Gigabytes
                    val mGBSize: Long = mMBSize / 1024

                    val currentUser = auth.currentUser
                    val uid = currentUser!!.uid
                    if (uid != null) {
                        db.collection("agency")
                                .whereEqualTo("uid", uid)
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

                                                mRefPhoto.putFile(filePath).addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot?> {
                                                    override fun onSuccess(p0: UploadTask.TaskSnapshot?) {
                                                        progress.dismiss()
                                                        val parentLayout: View = findViewById(android.R.id.content)
                                                        Snackbar.make(parentLayout, "Uploaded successfully", Snackbar.LENGTH_LONG)
                                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                                .show()

                                                        // Modigy Used Storage
                                                        val modify = db.collection("agency").document(documentID.toString())
                                                        val mTotalStorageUsed = mMBSize + sUsedSize
                                                        val mUpdatedStorageUsed = mTotalStorageUsed.toString()
                                                        modify.update("storageUsed", mUpdatedStorageUsed)
                                                                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                                        mRefPhoto.downloadUrl.addOnSuccessListener { evidURL ->
                                                            if (evidURL != null) {
                                                                // Update the collection and add evidenceURL

                                                                // Start an asynchronous task for the weather api
                                                                val mEvid = mIncident.getIncidentEvidenceURL()
                                                                db.collection("redlist")
                                                                        .whereEqualTo("person_photograph", mEvid)
                                                                        .get()
                                                                        .addOnSuccessListener { result ->
                                                                            for (document in result) {
                                                                                //Log.d(TAG, "${document.id} => ${document.data}")
                                                                                val documentID = document.getString("documentID")
                                                                                val mEvidURL = evidURL.toString() + "-image"
                                                                                val modifyx = db.collection("redlist").document(document.id)
                                                                                modifyx.update("person_photograph", mEvidURL)
                                                                                        .addOnSuccessListener {
                                                                                            Log.d(TAG, "DocumentSnapshot successfully written!")
                                                                                            // Init metadata for the following file incident

                                                                                            // Setting up a HTTP Get Request to obtain the weather api json response
                                                                                        }
                                                                                        .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                                                            }
                                                                        }
                                                                        .addOnFailureListener { exception ->
                                                                            //Log.d(TAG, "Error getting documents: ", exception)
                                                                        }
                                                            }
                                                        }.addOnFailureListener { err ->
                                                            Toast.makeText(this@RedListAddActivity, err.toString(), Toast.LENGTH_LONG).show()
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

/*
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

 */

}