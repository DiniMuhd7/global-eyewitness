package com.dinisoft.eyewitness

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
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
import android.os.Handler
import android.util.Log
import android.view.*
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.internal.ViewUtils
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.dinisoft.eyewitness.setting.WebviewGPSTrack
import com.dinisoft.eyewitness.ui.home.HomeActivity
import com.google.android.material.internal.ViewUtils.getContentView
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_home.root_container
import kotlinx.android.synthetic.main.layout_prof_user.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class UserProfActivity :  ThemeBaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2200
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private const val LOCATION_PERMISSION_CODE = 103
        private const val PHONE_PERMISSION_CODE = 104
        private const val COAST_LOCATION_PERMISSION_CODE = 105
        private const val READ_STORAGE_PERMISSION_CODE = 106
    }

    private val TAG: String = "---ProfActivity"

    private var mPermissionRequest: PermissionRequest? = null
    private var mJsRequestCount = 0

    private lateinit var mAboutUsPopup: PopupWindow
    private lateinit var mSocialNetworkPopup: PopupWindow
    private lateinit var mStorageDialogPopUp: PopupWindow

    val PDF: Int = 0
    val DOCX: Int = 1
    val AUDIO: Int = 2
    val VIDEO: Int = 3
    val PHOTO: Int = 4

    val TOC_POLICE = "Police"
    val TOC_ACCIDENT = "Accident"
    val TOC_RAPE = "Rape"
    val TOC_ROBBERY = "Robbery"
    val TOC_MURDER = "Murder"
    val TOC_TERRORIST = "Terrorism"

    val ref = ""

    lateinit var filePath: Uri
    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth

    lateinit var mStorage: FirebaseStorage
    lateinit var mStorageRef: StorageReference
    lateinit var mRefVideo: StorageReference
    lateinit var mRefAudio: StorageReference
    lateinit var mRefPhoto: StorageReference
    lateinit var mRefPDF: StorageReference
    lateinit var mRefDOC: StorageReference


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_prof_user)
        mContext = this
        //requestPermission()

        initSliderMenu()
        initClickEvent()
        geocoder = Geocoder(this@UserProfActivity, Locale.getDefault())

        // Initialize Firebase Auth
        auth = Firebase.auth
        db = Firebase.firestore


        mStorage = FirebaseStorage.getInstance()
        mStorageRef = mStorage.getReference()

        val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())
        mRefPhoto = mStorageRef.child("eyewitness/evidence/image/" + UUID.randomUUID().toString())
        mRefVideo = mStorageRef.child("eyewitness/evidence/video/" + UUID.randomUUID().toString())
        mRefAudio = mStorageRef.child("eyewitness/evidence/audio/" + UUID.randomUUID().toString())
        mRefDOC = mStorageRef.child("eyewitness/evidence/docx/" + UUID.randomUUID().toString())
        mRefPDF = mStorageRef.child("eyewitness/evidence/pdf/" + UUID.randomUUID().toString())


        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        if (currentUser != null){
            db.collection("users")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val mUID = document.getString("uid").toString()
                        val mDocID = document.getString("documentID").toString()
                        val mName = document.getString("name").toString()
                        val mEmail = document.getString("email").toString()
                        val mPhone = document.getString("phone").toString()
                        val mStat = document.getString("status").toString()
                        val mState = document.getString("state").toString()
                        val mCountry = document.getString("country").toString()
                        val mAcode = document.getString("accesscode").toString()
                        val mStorageUsed = document.getString("storageUsed")!!.toLong()
                        val mStorageCap = document.getString("storageCap")!!.toLong()
                        val mPhotoURL = document.getString("photoURL").toString()

                        val navigationHeader = navigation_view.getHeaderView(0)
                        val navigationLogo = navigationHeader.findViewById<View>(R.id.img_logo) as AppCompatImageView
                        val navigationTitle = navigationHeader.findViewById<View>(R.id.txt_navigation_title) as TextView
                        navigationTitle.text = mName
                        val navigationDetails =  navigationHeader.findViewById<View>(R.id.txt_navigation_detail) as TextView
                        navigationDetails.text =  mState+", "+mCountry

                        val photoURL = auth.currentUser?.photoUrl.toString()
                        Glide.with(this).load(photoURL).into(navigationLogo)
                    }

                }
        }

        val imgUserDp = findViewById<View>(R.id.userDp) as ImageView
        val txtTitleDash = findViewById<View>(R.id.txt_marker_title_dash) as TextView
        val txtStorageUsed = findViewById<View>(R.id.txt_storage_used) as TextView
        val txtStorageCap = findViewById<View>(R.id.txt_storage_cap) as TextView
        val txtTitlePolice = findViewById<View>(R.id.txt_value_police) as TextView
        val txtTitleAccident = findViewById<View>(R.id.txt_value_accident) as TextView
        val txtTitleMurder = findViewById<View>(R.id.txt_value_murder) as TextView
        val txtTitleTerrorism = findViewById<View>(R.id.txt_value_terrorism) as TextView
        val txtTitleRobbery = findViewById<View>(R.id.txt_value_robbery) as TextView
        val txtTitleSexAssault = findViewById<View>(R.id.txt_value_rape) as TextView
        val txtUpdateTitle = findViewById<View>(R.id.txt_update_title) as TextView
        val txtTitleReport = findViewById<View>(R.id.txt_title_report) as TextView

        val txtTitleFullName = findViewById<View>(R.id.txtFullName) as EditText
        val txtTitleEmail = findViewById<View>(R.id.txtEmail) as EditText
        val txtPhone = findViewById<View>(R.id.txtPhone) as EditText
        val txtState = findViewById<View>(R.id.txtState) as EditText
        val txtCountry = findViewById<View>(R.id.txtCountry) as EditText

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext as UserProfActivity);
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

                                val currentUser = auth.currentUser
                                val uid = currentUser?.uid
                                if (state != null && country != null) {
                                    // Get user credentials

                                    if (currentUser != null) {
                                        updateUI(currentUser)
                                        db.collection("users")
                                        .whereEqualTo("uid", uid)
                                        .get()
                                        .addOnSuccessListener { result ->
                                        for (document in result) {
                                            val mUID = document.getString("uid").toString()
                                            val mDocID = document.getString("documentID").toString()
                                            val mName = document.getString("name").toString()
                                            val mEmail =  document.getString("email").toString()
                                            val mPhone = document.getString("phone").toString()
                                            val mStat = document.getString("status").toString()
                                            val mAcode = document.getString("accesscode").toString()
                                            val mStorageUsed = document.getString("storageUsed")!!.toLong()
                                            val mStorageCap = document.getString("storageCap")!!.toLong()
                                            val mPhotoURL = document.getString("photoURL").toString()

                                            // Update UI
                                            txtTitleDash.setText(mName)
                                            txtTitleFullName.setText(mName)
                                            txtTitleEmail.setText(mEmail)
                                            txtUpdateTitle.setText("Update Your Details")
                                            txtTitleReport.setText("My Complaint")
                                            if (mPhone.equals("null", true)) {
                                                txtPhone.hint = "Enter your Mobile Number"
                                            } else {
                                                txtPhone.setText(mPhone)
                                            }
                                            txtState.setText(state)
                                            txtCountry.setText(country)
                                            txtStorageUsed.setText(mStorageUsed.toString()+" MB: Used")
                                            txtStorageCap.setText(mStorageCap.toString()+" MB: Total")
                                    }

                                }

                            }

                                    val txtValueOfficer = findViewById<View>(R.id.txt_value_police) as TextView
                                    val txtValueAccident = findViewById<View>(R.id.txt_value_accident) as TextView
                                    val txtValueTheft = findViewById<View>(R.id.txt_value_robbery) as TextView
                                    val txtValueHomicide = findViewById<View>(R.id.txt_value_murder) as TextView
                                    val txtValueFire = findViewById<View>(R.id.txt_value_fire) as TextView
                                    val txtValueFlood = findViewById<View>(R.id.txt_value_flood) as TextView
                                    val txtValueSexualAss = findViewById<View>(R.id.txt_value_rape) as TextView
                                    val txtValueDrugs = findViewById<View>(R.id.txt_value_drug) as TextView
                                    val txtValueTerrorism = findViewById<View>(R.id.txt_value_terrorism) as TextView
                                    val txtTitleStateName = findViewById<View>(R.id.txt_marker_title_dash) as TextView


                                    db.collection("officer")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueOfficer.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueOfficer.setText(count.toString())
                                                }

                                            }
                                    db.collection("accident")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueAccident.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueAccident.setText(count.toString())
                                                }

                                            }
                                    db.collection("theft")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueTheft.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueTheft.setText(count.toString())
                                                }

                                            }
                                    db.collection("murder")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueHomicide.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueHomicide.setText(count.toString())
                                                }

                                            }
                                    db.collection("firehazard")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueFire.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueFire.setText(count.toString())
                                                }

                                            }
                                    db.collection("flooding")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueFlood.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueFlood.setText(count.toString())
                                                }

                                            }
                                    db.collection("sexual-assault")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueSexualAss.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueSexualAss.setText(count.toString())
                                                }

                                            }
                                    db.collection("illegal-drugs")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueDrugs.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueDrugs.setText(count.toString())
                                                }

                                            }
                                    db.collection("terrorism")
                                            .whereEqualTo("id", uid)
                                            .get()
                                            .addOnSuccessListener { result ->
                                                // Get the total number of approved incident report from a particular state
                                                val count = result.size()
                                                if (count != null) {
                                                    txtValueTerrorism.setText(count.toString())
                                                } else {
                                                    val count = 0
                                                    txtValueTerrorism.setText(count.toString())
                                                }

                                            }

                                } else {
                                    val parentLayout: View = findViewById(android.R.id.content)
                                    Snackbar.make(parentLayout, "Network error ♥, your current address is unknown.", Snackbar.LENGTH_LONG)
                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                            .show()
                                }

                                //mMap.addMarker(MarkerOptions().position(location).title(knownName.toString() +""+", "+ city))

                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }


                        val userDp = findViewById<View>(R.id.userDp) as ImageView
                        val photoURL = auth.currentUser?.photoUrl.toString()
                        Glide.with(this).load(photoURL).into(userDp)

        // Sow buy storage bundle dialog
        val mCardStorage = findViewById<View>(R.id.card_item_dash_report) as CardView
        mCardStorage.setOnClickListener { showStorageDialog() }

                        val btnOfficer = findViewById<View>(R.id.img_police) as ImageView
                        val btnAccident = findViewById<View>(R.id.img_accident) as ImageView
                        val btnTheft = findViewById<View>(R.id.img_robbery) as ImageView
                        val btnSexAssault = findViewById<View>(R.id.img_rape) as ImageView
                        val btnTerrorism = findViewById<View>(R.id.img_terrorist) as ImageView

                        btnOfficer.setOnClickListener {
                            val user = auth.currentUser
                            if (user != null){
                                db.collection("officer")
                                        .whereEqualTo("status", "Approved")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            if (result.size() != 0){
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
                            if (user != null){
                                db.collection("accident")
                                        .whereEqualTo("status", "Approved")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            if (result.size() != 0){
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
                            if (user != null){
                                db.collection("theft")
                                        .whereEqualTo("status", "Approved")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            if (result.size() != 0){
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
                            if (user != null){
                                db.collection("sexual-assault")
                                        .whereEqualTo("status", "Approved")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            if (result.size() != 0){
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
                            if (user != null){
                                db.collection("terrorism")
                                        .whereEqualTo("status", "Approved")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            if (result.size() != 0){
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


                        val mIdentitySwitch = findViewById<View>(R.id.identitySwitch) as SwitchCompat
                        mIdentitySwitch.setOnCheckedChangeListener (object: CompoundButton.OnCheckedChangeListener {
                            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                                val userID = auth.currentUser!!.uid
                                if (userID != null) {
                                    if (isChecked) {
                                        //if (holder.itemView.txt_title_review_type.text.equals("Officer Incident Report")) {
                                        //holder.itemView.txt_title_review_status.text="Loading..."
                                        db.collection("users")
                                                .whereEqualTo("uid", userID)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    for (document in result) {
                                                        val documentID = document.getString("documentID")
                                                        val modify = db.collection("users").document(documentID.toString())
                                                        modify.update("identity", "private")
                                                                .addOnSuccessListener {
                                                                    // Hide the user caller id *67
                                                                    val callSettingsIntent = Intent("android.intent.action.MAIN")
                                                                    val distanceActivity = ComponentName("com.android.phone", "com.android.phone.GsmUmtsAdditionalCallOptions")
                                                                    callSettingsIntent.setComponent(distanceActivity)
                                                                    startActivity(callSettingsIntent)
                                                                    Toast.makeText(mContext, "Hide My Identity has been enabled. Please know that based on your preference, your identity is now private and you remain anonymous.", Toast.LENGTH_LONG).show()
                                                                }
                                                                .addOnFailureListener { e -> }
                                                    }

                                                }
                                                .addOnFailureListener { e ->


                                                }

                                    } else {
                                        val userID = auth.currentUser!!.uid
                                        //if (holder.itemView.txt_title_review_type.text.equals("Officer Incident Report")) {
                                        //holder.itemView.txt_title_review_status.text="Loading..."
                                        db.collection("users")
                                                .whereEqualTo("uid", userID)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    for (document in result) {
                                                        val documentID = document.getString("documentID")
                                                        val modify = db.collection("users").document(documentID.toString())
                                                        modify.update("identity", "public")
                                                                .addOnSuccessListener {
                                                                    // Show the user caller id *67
                                                                    val callSettingsIntent = Intent("android.intent.action.MAIN")
                                                                    val distanceActivity = ComponentName("com.android.phone", "com.android.phone.GsmUmtsAdditionalCallOptions")
                                                                    callSettingsIntent.setComponent(distanceActivity)
                                                                    startActivity(callSettingsIntent)
                                                                    Toast.makeText(mContext, "Hide My Identity has been disabled. Please know that based on your preference, your identity is now publicly available.", Toast.LENGTH_LONG).show()
                                                                }
                                                                .addOnFailureListener { e -> }
                                                    }

                                                }
                                                .addOnFailureListener { e ->


                                                }

                                    }
                                } else {
                                    Toast.makeText(mContext, "Sorry! Something went wrong. Try again.", Toast.LENGTH_LONG).show()
                                }
                            }

                        })


                        val currentUser = auth.currentUser
                        val uid = currentUser?.uid.toString()
                        if (currentUser != null) {
                            // Update user credentials
                            db.collection("users")
                                    .whereEqualTo("uid", uid)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        for (document in result) {
                                            //Log.d(TAG, "${document.id} => ${document.data}")
                                            val identity = document.getString("identity")
                                            if (identity != null && identity.equals("private")) {
                                                mIdentitySwitch.isChecked = true
                                            } else if (identity != null && identity.equals("public")) {
                                                mIdentitySwitch.isChecked = false
                                            }

                                        }
                                    }
                        }


                        val mExit = findViewById<View>(R.id.img_right_menu_exit) as AppCompatImageView
        mExit.setOnClickListener { exitHomeScreen() }

                        val mAbout = findViewById<View>(R.id.img_right_menu) as AppCompatImageView
                        mAbout.setOnClickListener { showAboutUs() }

        val mFab = findViewById<View>(R.id.fabButton) as FloatingActionButton
        mFab.setOnClickListener {
            val mTextTitle = txtUpdateTitle.text.toString()
            if (!mTextTitle.equals("Update Your Details", true)) {
                val parentLayout: View = findViewById(android.R.id.content)
                Snackbar.make(parentLayout, "Sorry ♥, cannot perform this operation at this time.", Snackbar.LENGTH_LONG)
                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                        .show()
            } else {
                val fullName =  txtTitleFullName.text.toString()
                val mEmail =  txtTitleEmail.text.toString()
                val mPhone =  txtPhone.text.toString()
                val mState = txtState.text.toString()
                val mCountry = txtCountry.text.toString()
                if (!fullName.isEmpty() && !mEmail.isEmpty() && !mPhone.isEmpty() && !mState.isEmpty() && !mCountry.isEmpty()) {
                    val currentUser = auth.currentUser
                    val uid = currentUser?.uid.toString()
                    // Update user credentials
                    db.collection("users")
                            .whereEqualTo("uid", uid)
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")
                                    val documentID = document.getString("documentID")
                                    val modify = db.collection("users").document(document.id)
                                    modify.update("name", fullName)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "DocumentSnapshot successfully written!")
                                                // Init metadata for the following file incident
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Updated Successfully.", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()
                                                // Setting up a HTTP Get Request to obtain the weather api json response
                                            }
                                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                    modify.update("email", mEmail)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "DocumentSnapshot successfully written!")
                                                // Init metadata for the following file incident
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Updated Successfully.", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()
                                                // Setting up a HTTP Get Request to obtain the weather api json response
                                            }
                                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                    modify.update("phone", mPhone)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "DocumentSnapshot successfully written!")
                                                // Init metadata for the following file incident
                                                val parentLayout: View = findViewById(android.R.id.content)
                                                Snackbar.make(parentLayout, "Updated Successfully.", Snackbar.LENGTH_LONG)
                                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                        .show()
                                                //startActivity(Intent(mContext, ProfActivity::class.java))
                                                // Setting up a HTTP Get Request to obtain the weather api json response
                                            }
                                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                    modify.update("state", mState)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "DocumentSnapshot successfully written!")
                                                // Init metadata for the following file incident

                                                // Setting up a HTTP Get Request to obtain the weather api json response
                                            }
                                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                    modify.update("country", mCountry)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "DocumentSnapshot successfully written!")
                                                // Init metadata for the following file incident

                                                // Setting up a HTTP Get Request to obtain the weather api json response
                                            }
                                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                    }

                                    }

                            .addOnFailureListener {
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else {
                    val parentLayout: View = findViewById(android.R.id.content)
                    Snackbar.make(parentLayout, "Sorry ♥, Text field should not be left blank.", Snackbar.LENGTH_LONG)
                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                            .show()
                }
            }

        }




    }) }



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
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)

    }
    // [END on_start_check_user]


    private fun updateUI(user: FirebaseUser?) {

    }


    private fun setActiveFullScreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }


    // Social Network dialog
    private fun showSocialNetwork(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_social, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mSocialNetworkPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mSocialNetworkPopup.setBackgroundDrawable(colorDrawable)
        mSocialNetworkPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mSocialNetworkPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSocialNetworkPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mSocialNetworkPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnFacebook = popUpView.findViewById<View>(R.id.img_facebook) as ImageView
        val btnTwitter = popUpView.findViewById<View>(R.id.img_twitter) as ImageView
        val btnTelegram = popUpView.findViewById<View>(R.id.img_telegram) as ImageView
        val btnYoutube = popUpView.findViewById<View>(R.id.img_youtube) as ImageView
        val btnQuora = popUpView.findViewById<View>(R.id.img_quora) as ImageView
        val btnSoundcloud = popUpView.findViewById<View>(R.id.img_soundcloud) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        //txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        //txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")
        btnConfirm.setOnClickListener { mSocialNetworkPopup.dismiss() }
        btnFacebook.setOnClickListener {
            startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://mobile.facebook.com/"))
            mSocialNetworkPopup.dismiss()
        }

        btnTwitter.setOnClickListener {
            startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://mobile.twitter.com/"))
            mSocialNetworkPopup.dismiss()
        }

        btnTelegram.setOnClickListener {
            startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://web.telegram.org/"))
            mSocialNetworkPopup.dismiss()
        }

        btnYoutube.setOnClickListener {
            startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://m.youtube.com/"))
            mSocialNetworkPopup.dismiss()
        }

        btnQuora.setOnClickListener {
            startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://www.quora.com/"))
            mSocialNetworkPopup.dismiss()
        }

        btnSoundcloud.setOnClickListener {
            startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://html5games.com/"))
            mSocialNetworkPopup.dismiss()
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



    // About us dialog
    private fun showAboutUs(){
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_about_us, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mAboutUsPopup = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)
        mAboutUsPopup.setBackgroundDrawable(colorDrawable)
        mAboutUsPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mAboutUsPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
        val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
        val btnEmail = popUpView.findViewById<View>(R.id.img_email) as ImageView
        val btnWebsite = popUpView.findViewById<View>(R.id.img_website) as ImageView

        btnConfirm.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        txtTitle.text = getString(R.string.title_user_version)
        txtDetail.text = getString(R.string.title_about_us)
        btnConfirm.setOnClickListener { mAboutUsPopup.dismiss() }
        btnCall.setOnClickListener {
            // Request permission and Show emergency dialog
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), PHONE_PERMISSION_CODE)
            Toast.makeText(mContext, "Initiating a call to " + getString(R.string.app_name)+" Team", Toast.LENGTH_LONG).show()
            mAboutUsPopup.dismiss()
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:" + getString(R.string.contact_phone))
            startActivity(callIntent)
        }

        btnWebsite.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).putExtra("Tag", getString(R.string.web_address)))
        }

        btnEmail.setOnClickListener {  UtilMethods.sandMailTo(mContext, "Contact with email!",
                getString(R.string.title_user_email),
                "Contact with via "+ R.string.app_name +" app", "") }
    }


    private fun initClickEvent(){
        // menu click toggle left
        img_left_menu.setOnClickListener{
            // slier menu open from left
            val params = DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.WRAP_CONTENT, DrawerLayout.LayoutParams.MATCH_PARENT)
            val gravityCompat: Int

            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)){
                params.gravity = Gravity.END
                gravityCompat = GravityCompat.END
                navigation_view.layoutParams = params
            }else{
                params.gravity = Gravity.START
                gravityCompat = GravityCompat.START
                navigation_view.layoutParams = params
            }

            val navigationLeftMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_LEFT_MENU_STYLE, Constants.LEFT_MENU_SLIDER)
            if(navigationLeftMenu == Constants.LEFT_MENU_SLIDER){
                Handler().postDelayed({
                    if (drawer_layout.isDrawerOpen(gravityCompat)){
                        drawer_layout.closeDrawer(gravityCompat)
                    }else{
                        drawer_layout.openDrawer(gravityCompat)
                    }
                }, 100)
            }else if(navigationLeftMenu == Constants.LEFT_MENU_RELOAD){
                // request for reload again website
                //webviewReload()
            }else if(navigationLeftMenu == Constants.LEFT_MENU_SHARE){
                UtilMethods.shareTheApp(mContext,
                        "Download "+ getString(R.string.app_name)+"" +
                                " app from play store. Click here: "+"" +
                                "https://play.google.com/store/apps/details?id="+packageName+"/")
            }else if(navigationLeftMenu == Constants.LEFT_MENU_HOME){
                //isViewLoaded = false
                //loadBaseWebView()
            }else if(navigationLeftMenu == Constants.LEFT_MENU_EXIT){
                exitHomeScreen()
            }else if(navigationLeftMenu == Constants.LEFT_MENU_HIDE){
                // menu is hidden
            }
        }

        // menu click toggle right
        img_right_menu.setOnClickListener{
            // slier menu open from left
            val params = DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.WRAP_CONTENT, DrawerLayout.LayoutParams.MATCH_PARENT)
            val gravityCompat: Int

            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)){
                params.gravity = Gravity.START
                gravityCompat = GravityCompat.START
                navigation_view.layoutParams = params
            }else{
                params.gravity = Gravity.END
                gravityCompat = GravityCompat.END
                navigation_view.layoutParams = params
            }

            val navigationRightMenu = PreferenceUtils.getInstance().getStringValue(Constants.KEY_RIGHT_MENU_STYLE, Constants.RIGHT_MENU_SLIDER)
            if(navigationRightMenu == Constants.RIGHT_MENU_SLIDER){
                Handler().postDelayed({
                    if (drawer_layout.isDrawerOpen(gravityCompat)) {
                        drawer_layout.closeDrawer(gravityCompat)
                    } else {
                        drawer_layout.openDrawer(gravityCompat)
                    }
                }, 100)
            }else if(navigationRightMenu == Constants.RIGHT_MENU_RELOAD){
                // request for reload again website
                //webviewReload()
            }else if(navigationRightMenu == Constants.RIGHT_MENU_SHARE){
                UtilMethods.shareTheApp(mContext,
                        "Download "+ getString(R.string.app_name)+"" +
                                " app from play store. Click here: "+"" +
                                "https://play.google.com/store/apps/details?id="+packageName+"/")
            }else if(navigationRightMenu == Constants.RIGHT_MENU_HOME){
                //isViewLoaded = false
                //loadWebView("https://www.eyewitness.global/welcome")
            }else if(navigationRightMenu == Constants.RIGHT_MENU_EXIT){
                exitHomeScreen()
            }else if(navigationRightMenu == Constants.RIGHT_MENU_HIDE){
                // menu is hidden
            }
        }
    }


    private fun initSliderMenu() {
        if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)) {
            navigation_view.layoutDirection = View.LAYOUT_DIRECTION_RTL
            navigation_view.textDirection = View.TEXT_DIRECTION_RTL
        }

        navigation_view.itemIconTintList = null
        val navigationMenu = navigation_view.menu
        navigationMenu.clear()

        /**
         * If you need to add menu with icon
         * menu.add(0, R.string.menu_home, Menu.NONE, R.string.menu_home).setIcon(R.drawable.ic_home)**/

        var subMenu: SubMenu
        subMenu = navigationMenu.addSubMenu("Menu")
        //subMenu.add(0, R.string.menu_home, Menu.NONE, getString(R.string.menu_home)).setIcon(R.drawable.ic_home)

        //var i = 1
        //for(menu in AppDataInstance.navigationMenus){
        //    when(menu.url) {
        subMenu.add(0, R.string.menu_home, Menu.NONE, getString(R.string.menu_home)).setIcon(R.drawable.ic_home)
        //subMenu.add(1, R.string.menu_map, Menu.NONE, getString(R.string.menu_map)).setIcon(R.drawable.ic_label)
        subMenu.add(2, R.string.menu_bot, Menu.NONE, getString(R.string.menu_bot)).setIcon(R.drawable.ic_label)
        subMenu.add(3, R.string.menu_profile, Menu.NONE, getString(R.string.menu_profile)).setIcon(R.drawable.ic_label)
        subMenu.add(4, R.string.menu_social, Menu.NONE, getString(R.string.menu_social)).setIcon(R.drawable.ic_label)
        //subMenu.add(5, R.string.menu_game, Menu.NONE, getString(R.string.menu_game)).setIcon(R.drawable.ic_label)
        subMenu.add(6, R.string.menu_about, Menu.NONE, getString(R.string.menu_about)).setIcon(R.drawable.ic_info)
        subMenu.add(7, R.string.menu_rate, Menu.NONE, getString(R.string.menu_rate)).setIcon(R.drawable.ic_rate)
        subMenu.add(8, R.string.menu_share, Menu.NONE, getString(R.string.menu_share)).setIcon(R.drawable.ic_share)
        subMenu.add(9, R.string.menu_exit, Menu.NONE, getString(R.string.menu_exit)).setIcon(R.drawable.ic_exit)


        /*
        // For using menu gorup
        "http://infixsoft.com/" -> {
            subMenu = navigationMenu.addSubMenu("Website")
            subMenu.add(i++, i-2, Menu.NONE, menu.name).setIcon(R.drawable.ic_label)
        }
        */

        // In cage you need to user custom icon
        // "http://infixsoft.com/" ->  subMenu.add(i++, i-2, Menu.NONE, menu.name).setIcon(R.drawable.ic_label)
        //else -> subMenu.add(i++, i-2, Menu.NONE, menu.name).setIcon(R.drawable.ic_label)
        //}
        //}

        navigation_view.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.string.menu_home -> { startActivity(Intent(mContext, DashUserActivity::class.java)); true}
                //R.string.menu_map -> {startActivity(Intent(mContext, MapActivity::class.java).putExtra("Tag", "agency")); true}
                R.string.menu_bot -> { startActivity(Intent(mContext, ChatActivity::class.java)); true}
                R.string.menu_profile -> { startActivity(Intent(mContext, UserProfActivity::class.java)); true}
                R.string.menu_social -> { showSocialNetwork(); true}
                //R.string.menu_game -> { startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://html5games.com/")); true}
                R.string.menu_about -> { showAboutUs(); true}
                R.string.menu_rate -> { UtilMethods.rateTheApp(mContext); true}
                R.string.menu_share -> { UtilMethods.shareTheApp(mContext,
                        "Download "+ getString(R.string.app_name)+"" +
                                " app from play store. Click here: "+"" +
                                "https://play.google.com/store/apps/details?id="+packageName+"/"); true}
                R.string.menu_exit -> { exitHomeScreen(); true}


                else -> {""; false}

            }



        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.string.menu_home -> startActivity(Intent(mContext, DashUserActivity::class.java))
            //R.string.menu_map ->  startActivity(Intent(mContext, MapActivity::class.java).putExtra("Tag", "agency"))
            R.string.menu_bot ->  startActivity(Intent(mContext, ChatActivity::class.java))
            R.string.menu_profile -> startActivity(Intent(mContext, UserProfActivity::class.java))
            R.string.menu_social -> showSocialNetwork()
            //R.string.menu_game -> startActivity(Intent(mContext, HomeActivity::class.java).putExtra("Tag", "https://html5games.com/"))
            R.string.menu_about -> showAboutUs()
            R.string.menu_rate -> UtilMethods.rateTheApp(mContext)
            R.string.menu_share -> UtilMethods.shareTheApp(mContext,
                    "Download "+ getString(R.string.app_name)+"" +
                            " app from play store. Click here: "+"" +
                            "https://play.google.com/store/apps/details?id="+packageName+"/")
            R.string.menu_exit -> exitHomeScreen()
            else ->
                try {
                    //loadWebView("https://www.eyewitness.global/welcome")
                }catch (ex: Exception){
                    ex.printStackTrace()
                }
        }

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) run {
            drawer_layout.closeDrawer(GravityCompat.START)
        }else if (drawer_layout.isDrawerOpen(GravityCompat.END)) run {
            drawer_layout.closeDrawer(GravityCompat.END)
        }
        return true
    }


    private fun exitHomeScreen() {
        auth.signOut()
        startActivity(Intent(this, AuthUIActivity::class.java))
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) run {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else if (drawer_layout.isDrawerOpen(GravityCompat.END)) run {
            drawer_layout.closeDrawer(GravityCompat.END)

            UtilMethods.showSnackbar(root_container, getString(R.string.massage_exit))

            Handler().postDelayed({
                run {
                }
            }, 2000)

        }

    }


    private fun requestPermission(){
        var mIndex: Int = -1
        val requestList: Array<String> = Array(10, { "" } )

        // Access photos Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        // Location Permission
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.ACCESS_FINE_LOCATION
        }else{
            getLocation()
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.CAMERA
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mIndex ++
            requestList[mIndex] = Manifest.permission.RECORD_AUDIO
        }

        if(mIndex != -1){
            ActivityCompat.requestPermissions(this, requestList, PERMISSIONS_REQUEST_ALL)
        }
    }

    private fun jsPermissionAccepted(){
        mJsRequestCount --
        if (mPermissionRequest != null && mJsRequestCount == 0){
            mPermissionRequest!!.grant(mPermissionRequest!!.resources)
        }
    }
    private fun askForPermission(permissionCode: Int, request: Boolean): Boolean{
        when(permissionCode){
            PERMISSIONS_REQUEST_LOCATION ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@UserProfActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION)){
                            UtilMethods.showSnackbar(root_container, "Location permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@UserProfActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_CAMERA ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@UserProfActivity,
                                        Manifest.permission.CAMERA)){
                            UtilMethods.showSnackbar(root_container, "Camera permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@UserProfActivity, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@UserProfActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                            UtilMethods.showSnackbar(root_container, "Write permission is required, Please allow from permission manager!!")
                        }else {
                            ActivityCompat.requestPermissions(this@UserProfActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                        }
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_MICROPHONE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    if(request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this@UserProfActivity,
                                        Manifest.permission.RECORD_AUDIO)) {
                            UtilMethods.showSnackbar(root_container, "Audio permission is required, Please allow from permission manager!!")
                        } else
                            ActivityCompat.requestPermissions(this@UserProfActivity, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSIONS_REQUEST_MICROPHONE)
                    }
                    return false
                }else{
                    jsPermissionAccepted()
                    return true
                }
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ALL -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission accept location
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        UtilMethods.printLog(TAG, "External permission accept.")
                    }

                    // permission accept location
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        UtilMethods.printLog(TAG, "Location permission accept.")
                        getLocation()
                    }

                } else {
                    //UtilMethods.showSnackbar(root_container, "Permission Failed!")
                }
                return
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Write permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Write Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Camera permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Camera Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Location permission accept.")
                    //getLocation()
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(root_container, "Location Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_MICROPHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Microphone Permission Accept.")
                    jsPermissionAccepted()
                }
                else {
                    UtilMethods.showSnackbar(root_container, "Microphone Permission Failed!")
                }
            }
        }
    }

    // get user location for
    private fun getLocation(): String {
        var newloc = "0,0"
        //Checking for location permissions
        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false)) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val gps = WebviewGPSTrack(mContext)
            val latitude = gps.getLatitude()
            val longitude = gps.getLongitude()
            if (gps.canGetLocation()) {
                if (latitude != 0.0 || longitude != 0.0) {
                    cookieManager.setCookie(mDefaultURL, "lat=$latitude")
                    cookieManager.setCookie(mDefaultURL, "long=$longitude")
                    newloc = "$latitude,$longitude"
                } else {
                    UtilMethods.printLog(TAG, "Location null.")
                }
            } else {
                UtilMethods.printLog(TAG, "Location read failed.")
            }
        }
        return newloc
    }

    private fun locationSettingsRequest() {
        val locationManager = mContext
                .getSystemService(Service.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false) && isGPSEnabled == false) {
            val mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000)

            val settingsBuilder = LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest)
            settingsBuilder.setAlwaysShow(true)

            val result = LocationServices.getSettingsClient(mContext)
                    .checkLocationSettings(settingsBuilder.build())
            result.addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                } catch (ex: ApiException) {

                    when (ex.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            Toast.makeText(mContext, "GPS IS OFF", Toast.LENGTH_SHORT).show()
                            val resolvableApiException = ex as ResolvableApiException
                            resolvableApiException.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {
                            Toast.makeText(mContext, "PendingIntent unable to execute request.", Toast.LENGTH_SHORT).show()

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            Toast.makeText(
                                    mContext,
                                    "Something is wrong in your GPS",
                                    Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }



}