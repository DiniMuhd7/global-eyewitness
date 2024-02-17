package com.dinisoft.eyewitness

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dinisoft.eyewitness.adapters.RedListAdapter
import com.dinisoft.eyewitness.model.RedList
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import com.google.firebase.storage.StorageReference
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.redlist_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class RedListFeedActivity : AppCompatActivity() {

    private val TAG: String = "--RedListFeedActivity"

    // firebase auth, database and incident object
    lateinit var auth: FirebaseAuth
    lateinit var db: FirebaseFirestore
    lateinit var mRedList: RedList

    private val mRedListMute = mutableListOf<RedList>()
    lateinit var mState: String // For security check
    lateinit var mCountry: String
    lateinit var mGlobal: String

    private lateinit var mRedListAdapter: RedListAdapter
    private lateinit var mContext: Context

    lateinit var mStorage: FirebaseStorage
    lateinit var mStorageRef: StorageReference
    lateinit var mRefVideo: StorageReference
    lateinit var mRefAudio: StorageReference
    lateinit var mRefPhoto: StorageReference
    lateinit var mRefPDF: StorageReference
    lateinit var mRefDOC: StorageReference

    // The entry point to the Fused Location Provider.
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    // [END maps_current_place_state_keys]
    lateinit var geocoder: Geocoder

    private var currentItemPosition = -1
    private lateinit var mStorageDialogPopUp: PopupWindow

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.redlist_main)
        mContext = this
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

        val btnState = findViewById<View>(R.id.btn_state) as AppCompatButton
        val btnCountry = findViewById<View>(R.id.btn_country) as AppCompatButton
        val btnGlobal = findViewById<View>(R.id.btn_global) as AppCompatButton

        geocoder = Geocoder(mContext, Locale.getDefault())
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext as RedListFeedActivity)

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
                            if (knownName != null && state != null) {
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

                            //mMap.addMarker(MarkerOptions().position(location).title(knownName.toString() +""+", "+ city))

                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                })

        val userDp = findViewById<View>(R.id.userDp) as ImageView
        val txtStorageUsed = findViewById<View>(R.id.txt_storage_used) as TextView
        val txtStorageCap = findViewById<View>(R.id.txt_storage_cap) as TextView


        // Show buy storage bundle dialog
        val mCardStorage = findViewById<View>(R.id.card_item_userDp) as CardView
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


        btnState.setOnClickListener {
            val user = auth.currentUser
            val uid = user?.uid
            if (uid != null) {
                // Retrieve Officer collection
                btnState.setBackgroundColor(Color.parseColor("#2196F3"))
                btnState.setTextColor(Color.WHITE)
                btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnCountry.setTextColor(Color.BLACK)
                btnGlobal.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnGlobal.setTextColor(Color.BLACK)

                mRedListMute.clear() // Clear all contents from list objrct
                mRedListAdapter.deleteItem(cardList) // Delete all items in recycler view

                if (::mState.isInitialized) {
                    db.collection("redlist")
                            .whereEqualTo("person_seek_info_region", mState)
                            //.orderBy("", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    val docID = document.getString("documentID")
                                    val alertInfo = document.getString("alertInfo")
                                    val authorName = document.getString("auth_name")
                                    val personStatus = document.getString("person_status")
                                    val personMetatime = document.getString("metatime")
                                    val personReward = document.getString("person_reward")
                                    val personPhoto = document.getString("person_photograph")
                                    if (docID != null && alertInfo != null && authorName != null && personMetatime != null && personReward != null) {
                                        // Incident model object
                                        mRedList = RedList()
                                        mRedList.setDocumentID(docID)
                                        mRedList.setAuthName(authorName.toString())
                                        mRedList.setAlertInfo(alertInfo.toString())
                                        mRedList.setPersonStatus(personStatus)
                                        mRedList.setPersonMetatime(personMetatime)
                                        mRedList.setPersonReward(personReward)
                                        mRedList.setPersonPhotograph(personPhoto)
                                        mRedListMute.add(mRedList)

                                        mRedListAdapter = RedListAdapter(mRedListMute, mContext)
                                        cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                        cardList.adapter = mRedListAdapter
                                        defaultMessageTextView.visibility = View.GONE
                                    }

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                                Toast.makeText(this@RedListFeedActivity, "Sorry, something went wrong.", Toast.LENGTH_LONG).show()
                            }


                }


            }
        }


        btnCountry.setOnClickListener {
            val user = auth.currentUser
            val uid = user?.uid
            if (uid != null) {
                // Retrieve Officer collection
                btnState.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnState.setTextColor(Color.BLACK)
                btnGlobal.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnGlobal.setTextColor(Color.BLACK)
                btnCountry.setBackgroundColor(Color.parseColor("#2196F3"))
                btnCountry.setTextColor(Color.WHITE)

                mRedListMute.clear() // Clear all contents from list objrct
                mRedListAdapter.deleteItem(cardList) // Delete all items in recycler view

                if (::mCountry.isInitialized){
                    db.collection("redlist")
                            .whereEqualTo("person_seek_info_region", mCountry)
                            //.orderBy("", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    val docID = document.getString("documentID")
                                    val alertInfo = document.getString("alertInfo")
                                    val authorName = document.getString("auth_name")
                                    val personStatus = document.getString("person_status")
                                    val personMetatime = document.getString("metatime")
                                    val personReward = document.getString("person_reward")
                                    val personPhoto = document.getString("person_photograph")
                                    if (docID != null && alertInfo != null && authorName != null && personMetatime != null && personReward != null) {
                                        // Incident model object
                                        mRedList = RedList()
                                        mRedList.setDocumentID(docID)
                                        mRedList.setAuthName(authorName.toString())
                                        mRedList.setAlertInfo(alertInfo.toString())
                                        mRedList.setPersonStatus(personStatus)
                                        mRedList.setPersonMetatime(personMetatime)
                                        mRedList.setPersonReward(personReward)
                                        mRedList.setPersonPhotograph(personPhoto)
                                        mRedListMute.add(mRedList)

                                        mRedListAdapter = RedListAdapter(mRedListMute, mContext)
                                        cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                        cardList.adapter = mRedListAdapter
                                        defaultMessageTextView.visibility = View.GONE
                                    }

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                                Toast.makeText(this@RedListFeedActivity, "Sorry, something went wrong.", Toast.LENGTH_LONG).show()
                            }
                }

            }
        }

        btnGlobal.setOnClickListener {
            val user = auth.currentUser
            val uid = user?.uid
            if (uid != null) {
                // Retrieve Officer collection
                btnCountry.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnCountry.setTextColor(Color.BLACK)
                btnState.setBackgroundColor(Color.parseColor("#FFF5F5F5"))
                btnState.setTextColor(Color.BLACK)
                btnGlobal.setBackgroundColor(Color.parseColor("#2196F3"))
                btnGlobal.setTextColor(Color.WHITE)

                mRedListMute.clear() // Clear all contents from list objrct
                mRedListAdapter.deleteItem(cardList) // Delete all items in recycler view

                db.collection("redlist")
                        //.whereEqualTo("person_seek_info_region", mCountry)
                        //.orderBy("", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val docID = document.getString("documentID")
                                val alertInfo = document.getString("alertInfo")
                                val authorName = document.getString("auth_name")
                                val personStatus = document.getString("person_status")
                                val personMetatime = document.getString("metatime")
                                val personReward = document.getString("person_reward")
                                val personPhoto = document.getString("person_photograph")
                                if (docID != null && alertInfo != null && authorName != null && personMetatime != null && personReward != null) {
                                    // Incident model object
                                    mRedList = RedList()
                                    mRedList.setDocumentID(docID)
                                    mRedList.setAuthName(authorName.toString())
                                    mRedList.setAlertInfo(alertInfo.toString())
                                    mRedList.setPersonStatus(personStatus)
                                    mRedList.setPersonMetatime(personMetatime)
                                    mRedList.setPersonReward(personReward)
                                    mRedList.setPersonPhotograph(personPhoto)
                                    mRedListMute.add(mRedList)

                                    mRedListAdapter = RedListAdapter(mRedListMute, mContext)
                                    cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                    cardList.adapter = mRedListAdapter
                                    defaultMessageTextView.visibility = View.GONE
                                }

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                            Toast.makeText(this@RedListFeedActivity, "Sorry, something went wrong.", Toast.LENGTH_LONG).show()
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

                            txtStorageUsed.setText(mStorageUsed.toString()+" MB: Used")
                            txtStorageCap.setText(mStorageCap.toString()+" MB: Total")
                        }

                    }
        }

        val user = auth.currentUser
        val uid = user?.uid
        if (uid != null) {
            // Retrieve Officer collection
                 db.collection("redlist")
                    //.orderBy("", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val docID = document.getString("documentID")
                            val alertInfo = document.getString("alertInfo")
                            val authorName = document.getString("auth_name")
                            val personStatus = document.getString("person_status")
                            val personMetatime = document.getString("metatime")
                            val personReward = document.getString("person_reward")
                            val personPhoto = document.getString("person_photograph")
                            if (docID != null && alertInfo != null && authorName != null /*&& personMetatime != null*/ && personReward != null) {
                                // Incident model object
                                mRedList = RedList()
                                mRedList.setDocumentID(docID)
                                mRedList.setAuthName(authorName.toString())
                                mRedList.setAlertInfo(alertInfo.toString())
                                mRedList.setPersonStatus(personStatus)
                                mRedList.setPersonMetatime(personMetatime)
                                mRedList.setPersonReward(personReward)
                                mRedList.setPersonPhotograph(personPhoto)
                                mRedListMute.add(mRedList)

                                mRedListAdapter = RedListAdapter(mRedListMute, mContext)
                                cardList.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
                                cardList.adapter = mRedListAdapter
                                defaultMessageTextView.visibility = View.GONE
                            }

                        }
                    }
                    .addOnFailureListener { exception ->
                        //Log.d(TAG, "Error getting documents: ", exception)
                        Toast.makeText(this@RedListFeedActivity, "Sorry, something went wrong.", Toast.LENGTH_LONG).show()
                    }

        } else {
            Toast.makeText(this@RedListFeedActivity, "Sorry, something went wrong.", Toast.LENGTH_LONG).show()
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
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)

    }
    // [END on_start_check_user]


    private fun updateUI(user: FirebaseUser?) {

    }

    @SuppressLint("RestrictedApi", "ServiceCast")
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



    override fun onStop() {
        super.onStop()
    }

}