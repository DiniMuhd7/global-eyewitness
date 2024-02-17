package com.dinisoft.eyewitness

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dinisoft.eyewitness.setting.WebviewGPSTrack
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.activity_home.*
import java.util.*
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult


/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class AuthUIActivity : ThemeBaseActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var db: FirebaseFirestore
    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mPopUpVerification: PopupWindow
    private lateinit var googleSignInClient: GoogleSignInClient

    private var mPermissionRequest: PermissionRequest? = null
    private var mJsRequestCount = 0

    private lateinit var status: String
    private lateinit var accesscode: String

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2200
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
        private const val LOCATION_PERMISSION_CODE = 103
        private const val PHONE_PERMISSION_CODE = 104
        private const val COAST_LOCATION_PERMISSION_CODE = 105
        private const val READ_STORAGE_PERMISSION_CODE = 106
        private const val PERMISSIONS_REQ_ALL = 107

        private const val RC_SIGN_IN = 9001
        private val TAG: String = "--AuthActivity"
    }

    // [START auth_fui_create_launcher]
    // See: https://developer.android.com/training/basics/intents/result
    private val signInLauncherEyewitness = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }
    // [END auth_fui_create_launcher]


    // [START auth_fui_create_launcher]
    // See: https://developer.android.com/training/basics/intents/result
    private val signInLauncherAgency = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }
    // [END auth_fui_create_launcher]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_auth)
        mContext = this
        // Request for permission
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_LOCATION)

        // Initialize Firebase Auth
        auth = Firebase.auth
        db = Firebase.firestore

        val mBtnEyewitness = findViewById<View>(R.id.btn_eyewit) as AppCompatButton
        val mBtnAgency = findViewById<View>(R.id.btn_agency) as AppCompatButton

        mBtnEyewitness.setOnClickListener {
            status = "user"
            accesscode = "free"
            createSignInIntentEyewitness()
        }

        mBtnAgency.setOnClickListener {
            status = "agency"
            accesscode = ""
            createSignInIntentAgency()
        }


    }

    private fun addUserToFirebase(uid: String, name: String, email: String, phone: String, status: String, accesscode: String, storageUsed: String, storageCap: String, photoURL: String) {

            val user = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "status" to status,
                    "accesscode" to accesscode,
                    "identity" to "public",
                    "eyelensView" to "disable",
                    "storageUsed" to storageUsed,
                    "storageCap" to storageCap,
                    "photoURL" to photoURL

            )

            if (status.equals("user")) {
                db.collection("users")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener { result ->
                            //for (document in result) {
                            //val mUID = document.getString("uid").toString()
                            if (result.isEmpty) {
                                db.collection("users")
                                        .add(user)
                                        .addOnSuccessListener { documentReference ->
                                            //Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                                            // Update the collection and add docuemnt ID
                                            val mDocumentID = documentReference.id
                                            val modifyRef = db.collection("users").document(documentReference.id)
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
                            } else {
                                val parentLayout: View = findViewById(android.R.id.content)
                                Snackbar.make(parentLayout, "You are signing in as " + status, Snackbar.LENGTH_LONG)
                                        .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                        .show()
                                // Create an array and pass user credentials via intent put extra
                                val array = arrayOf(uid, name, email, phone, status, accesscode, photoURL) // Pass to an array of string
                                startActivity(Intent(this, DashUserActivity::class.java))
                            }
                        }

                // }
            } else if (status.equals("agency")) {
                db.collection("agency")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener { result ->
                            //for (document in result) {
                            //val mUID = document.getString("uid").toString()
                            if (result.isEmpty) {
                                db.collection("agency")
                                        .add(user)
                                        .addOnSuccessListener { documentReference ->
                                            //Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                                            // Update the collection and add docuemnt ID
                                            val mDocumentID = documentReference.id
                                            val modifyRef = db.collection("agency").document(documentReference.id)
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
                            } else {
                                for (document in result){
                                    val agencyCode = document.getString("accesscode").toString()
                                    if (!agencyCode.isEmpty()){
                                        val parentLayout: View = findViewById(android.R.id.content)
                                        Snackbar.make(parentLayout, "You are signing in as " + status, Snackbar.LENGTH_LONG)
                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                .show()
                                        // Create an array and pass user credentials via intent put extra
                                        val array = arrayOf(uid, name, email, phone, status, accesscode, photoURL) // Pass to an array of string
                                        startActivity(Intent(this, DashAgencyActivity::class.java))
                                    } else {
                                        Toast.makeText(mContext, "No valid access code. To apply for an access code, contact deenysoft@gmail.com or call +234 90 3888 00 35", Toast.LENGTH_LONG).show()
                                        val parentLayout: View = findViewById(android.R.id.content)
                                        Snackbar.make(parentLayout, "Contact Eyewitness Team", Snackbar.LENGTH_LONG)
                                                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                                                .show()
                                    }
                                }

                            }
                        }

            } else {
                startActivity(Intent(this, AuthUIActivity::class.java))
            }


        }



    private fun createSignInIntentEyewitness() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = Arrays.asList(
                //AuthUI.IdpConfig.EmailBuilder().build(),
                //AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build())
                //AuthUI.IdpConfig.TwitterBuilder().build(),
                //AuthUI.IdpConfig.YahooBuilder().build())

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.chatbg) // Set logo drawable
                .setTheme(R.style.AppThemePrimary) // Set theme
                .setTosAndPrivacyPolicyUrls(
                        "https://mywaec-8cff0.web.app/",
                        "https://mywaec-8cff0.web.app/")
                .build()
        val array = arrayOf(status, accesscode) // Pass to an array of string
        signInLauncherEyewitness.launch(signInIntent.putExtra("Tag", array))
        // [END auth_fui_create_intent]

        //Toast.makeText(mContext, status, Toast.LENGTH_LONG).show()
    }


    private fun createSignInIntentAgency() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = Arrays.asList(
                //AuthUI.IdpConfig.EmailBuilder().build(),
                //AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build())
                //AuthUI.IdpConfig.TwitterBuilder().build(),
                //AuthUI.IdpConfig.YahooBuilder().build())

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.chatbg) // Set logo drawable
                .setTheme(R.style.AppThemePrimary) // Set theme
                .setTosAndPrivacyPolicyUrls(
                        "https://mywaec-8cff0.web.app/",
                        "https://mywaec-8cff0.web.app/")
                .build()
        val array = arrayOf(status, accesscode) // Pass to an array of string
        signInLauncherAgency.launch(signInIntent.putExtra("Tag", array))
        val mData = signInIntent.getStringArrayExtra("Tag")
        val mStatus = mData!![0].toString()
        val mAccessCode = mData!![1].toString()
        // Initialize lateinit property
        status = mStatus
        accesscode = mAccessCode
        // [END auth_fui_create_intent]

        //Toast.makeText(mContext, status, Toast.LENGTH_LONG).show()
    }

    // [START auth_fui_result]
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val uid = user?.uid.toString()
                val name = user?.displayName.toString()
                val email = user?.email.toString()
                val phone = user?.phoneNumber.toString()
                val photoURL = user?.photoUrl.toString()
                addUserToFirebase(uid, name, email, phone, status, accesscode, "0", "100", photoURL) // Add data to Firebase if doesn't exist or skip and sign in
            }

            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Network error, check your network settings", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }
    }
    // [END auth_fui_result]
}



