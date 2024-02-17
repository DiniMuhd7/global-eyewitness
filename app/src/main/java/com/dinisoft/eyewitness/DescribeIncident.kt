package com.dinisoft.eyewitness

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class DescribeIncident: AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    private val TAG: String = "---DescActivity"
    lateinit var selectedSpinItemCtype: String
    lateinit var selectedSpinItemCMis: String

    lateinit var mStorage: FirebaseStorage
    lateinit var mStorageRef: StorageReference
    lateinit var mRefVideo: StorageReference
    lateinit var mRefAudio: StorageReference
    lateinit var mRefPhoto: StorageReference
    lateinit var mRefPDF: StorageReference
    lateinit var mRefDOC: StorageReference

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desc)

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

        val txtHeadline = findViewById<View>(R.id.txtHeadline) as EditText
        val txtDesc = findViewById<View>(R.id.txtDesc) as EditText

        val userDp = findViewById<View>(R.id.userDp) as ImageView
        val txtTitleName = findViewById<View>(R.id.txt_title_dash) as TextView
        val txtStorageUsed = findViewById<View>(R.id.txt_storage_used) as TextView
        val txtStorageCap = findViewById<View>(R.id.txt_storage_cap) as TextView

        val photoURL = auth.currentUser?.photoUrl.toString()
        Glide.with(this).load(photoURL).into(userDp)

        val mSelectedItem = intent.getStringArrayExtra("Tag")
        val type = mSelectedItem!![0]
        val evidURL = mSelectedItem!![1]
        val documentID = mSelectedItem!![2]
        val status = mSelectedItem!![3]

        val currentUser = auth.currentUser
                            val uid = currentUser?.uid
                                // Get user credentials
                                if (currentUser != null) {
                                    updateUI(currentUser)
                                    if (status.equals("user", true)){
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
                                                        val mAcode = document.getString("accesscode").toString()
                                                        val mStorageUsed = document.getString("storageUsed")!!.toLong()
                                                        val mStorageCap = document.getString("storageCap")!!.toLong()
                                                        val mPhotoURL = document.getString("photoURL").toString()

                                                        txtStorageUsed.setText(mStorageUsed.toString() + " MB: Used")
                                                        txtStorageCap.setText(mStorageCap.toString() + " MB: Total")
                                                    }

                                                }
                                    } else if (status.equals("agency", true)) {
                                        db.collection("agency")
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
                                                    val mAcode = document.getString("accesscode").toString()
                                                    val mStorageUsed = document.getString("storageUsed")!!.toLong()
                                                    val mStorageCap = document.getString("storageCap")!!.toLong()
                                                    val mPhotoURL = document.getString("photoURL").toString()

                                                    txtStorageUsed.setText(mStorageUsed.toString() + " MB: Used")
                                                    txtStorageCap.setText(mStorageCap.toString() + " MB: Total")
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


        // Assign list of incident type to the spinner
        val misconduct = arrayOf(getString(R.string.false_confession), getString(R.string.intimidation), getString(R.string.false_arrest), getString(R.string.false_imprison),
                getString(R.string.false_evid), getString(R.string.spoil_evid), getString(R.string.perjury), getString(R.string.witness_temp),
                getString(R.string.brutality), getString(R.string.corruption), getString(R.string.racial_prof), getString(R.string.u_surveillance), getString(R.string.u_searches),
                getString(R.string.u_seizure), getString(R.string.sexual_assault), getString(R.string.treason),
                getString(R.string.impersonate), getString(R.string.v_protest), getString(R.string.malpractice))

        val accident = arrayOf(getString(R.string.roll_over), getString(R.string.head_on_col), getString(R.string.side_impact),
                getString(R.string.rear_end), getString(R.string.single_car), getString(R.string.hitnrun))
        val murder = arrayOf(getString(R.string.homicide), getString(R.string.suicide), getString(R.string.genocide))
        val firehazard = arrayOf(getString(R.string.residential_fire), getString(R.string.bush_fire), getString(R.string.automotive_fire), getString(R.string.school_fire), getString(R.string.market_fire),
                getString(R.string.organization_fire), getString(R.string.arson), getString(R.string.volcano))
        val flooding = arrayOf(getString(R.string.domestic_flood), getString(R.string.domestic_village), getString(R.string.river_flood), getString(R.string.heavy_rain))
        val sexual_assault = arrayOf(getString(R.string.sexual_harass), getString(R.string.child_abuse), getString(R.string.rape), getString(R.string.incest),
                getString(R.string.molestation), getString(R.string.intimate_part))
        val illegal_drugs = arrayOf(getString(R.string.taken_drugs), getString(R.string.dealing_drugs), getString(R.string.trafficking_drugs), getString(R.string.manufacturing_drugs),
                getString(R.string.possession_drugs), getString(R.string.supply_drugs))
        val assault = arrayOf(getString(R.string.aggravated_ass), getString(R.string.verbal_ass), getString(R.string.domestic_violence))
        val terrorism = arrayOf(getString(R.string.insurgency), getString(R.string.banditry), getString(R.string.kidnapping))
        val theft = arrayOf(getString(R.string.robbery), getString(R.string.burglary), getString(R.string.auto_theft), getString(R.string.larceny), getString(R.string.embezzle),
                getString(R.string.fraud), getString(R.string.shoplifting), getString(R.string.livestock), getString(R.string.g_theft))

        if (type.equals("Officer", true)) {
            val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
            txtTitleName.setText("Officer Incident Report")

            getContentView(findViewById(R.id.view_id))?.let {
                ArrayAdapter<String>(this@DescribeIncident,
                        android.R.layout.simple_spinner_item, misconduct)
                        .also { adapter ->
                            // Specify the layout to use when the list of choices appears
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            // Apply the adapter to the spinner
                            spinCtype.adapter = adapter

                            spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                        parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long
                                ) {
                                    // Pick the selected item
                                    val selectedItem = parent?.getItemAtPosition(position).toString()
                                    selectedSpinItemCtype = selectedItem
                                    Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {

                                }
                            }
                        }
            }


        } else if (type.equals("Accident", true)) {
            val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
            txtTitleName.setText("Accident Incident Report")

            getContentView(findViewById(R.id.view_id))?.let {
                ArrayAdapter<String>(this@DescribeIncident,
                        android.R.layout.simple_spinner_item, accident)
                        .also { adapter ->
                            // Specify the layout to use when the list of choices appears
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            // Apply the adapter to the spinner
                            spinCtype.adapter = adapter

                            spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                        parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long
                                ) {
                                    // Pick the selected item
                                    val selectedItem = parent?.getItemAtPosition(position).toString()
                                    selectedSpinItemCtype = selectedItem
                                    Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {

                                }
                            }
                        }
            }


        } else if (type.equals("Murder", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Murder Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, murder)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Theft", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Theft Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, theft)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Sexual-Assault", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Sexual-Assault Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, sexual_assault)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Illegal-Drugs", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Illegal-Drugs Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, illegal_drugs)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Firehazard", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Firehazard Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, firehazard)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Flooding", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Flooding Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, flooding)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Assault", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Assault Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, assault)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        } else if (type.equals("Terrorism", true)) {
                val spinCtype = findViewById<View>(R.id.spin_ctype) as Spinner
                txtTitleName.setText("Terrorism Incident Report")

                getContentView(findViewById(R.id.view_id))?.let {
                    ArrayAdapter<String>(this@DescribeIncident,
                            android.R.layout.simple_spinner_item, terrorism)
                            .also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spinCtype.adapter = adapter

                                spinCtype.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long
                                    ) {
                                        // Pick the selected item
                                        val selectedItem = parent?.getItemAtPosition(position).toString()
                                        selectedSpinItemCtype = selectedItem
                                        Toast.makeText(this@DescribeIncident, selectedSpinItemCtype, Toast.LENGTH_LONG).show()
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {

                                    }
                                }
                            }
                }


        }

        val mFab = findViewById<View>(R.id.fabButton) as FloatingActionButton
        mFab.setOnClickListener {
            if (::selectedSpinItemCtype.isInitialized){
                val headline = txtHeadline.text.toString()
                val desc = txtDesc.text.toString()
                if (!headline.isEmpty() && !desc.isEmpty()) {
                    db.collection(type)
                            .whereEqualTo("documentID", documentID)
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")
                                    val modify = db.collection(type).document(document.id)
                                    if (headline != null && desc != null) {
                                        modify.update("title", headline)
                                                .addOnSuccessListener {
                                                    Log.d(TAG, "DocumentSnapshot successfully written!")
                                                    // Init metadata for the following file incident
                                                    val parentLayout: View = findViewById(android.R.id.content)
                                                    Snackbar.make(parentLayout, "Updated Successfully.", Snackbar.LENGTH_LONG)
                                                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                            .show()
                                                    //startActivity(Intent(this, MapActivity::class.java).putExtra("Tag", status))

                                                    // Setting up a HTTP Get Request to obtain the weather api json response
                                                }
                                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                                        modify.update("description", desc)
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

                                        modify.update("subType", selectedSpinItemCtype)
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

                                    } else {
                                        val parentLayout: View = findViewById(android.R.id.content)
                                        Snackbar.make(parentLayout, "Default settings would be applied to describe the incident.", Snackbar.LENGTH_LONG)
                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                .show()
                                        //startActivity(Intent(this, MapActivity::class.java).putExtra("Tag", status))
                                    }

                                }
                            }
                            .addOnFailureListener {
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }

                    //Toast.makeText(this, type+" \n"+ evidURL +" \n"+ documentID, Toast.LENGTH_LONG).show()
                } else {
                    //startActivity(Intent(this, MapActivity::class.java).putExtra("Tag", status))
                    val parentLayout: View = findViewById(android.R.id.content)
                    Snackbar.make(parentLayout, "Default settings would be applied to describe the incident.", Snackbar.LENGTH_LONG)
                            .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                            .show()
                }
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
            val rootPath = File(this.filesDir, "EyeWitness")
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
            val rootPath = File(this.filesDir, "EyeWitness")
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

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun onBackPressed() {
        super.onBackPressed()

    }

}