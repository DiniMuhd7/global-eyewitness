package com.dinisoft.eyewitness

import android.accounts.NetworkErrorException
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.dinisoft.eyewitness.Constants.EyewitnessMobileConstants.VERSION_CODE_PREFERENCE_KEY
import com.dinisoft.eyewitness.Constants.EyewitnessMobileConstants.VERSION_NAME_PREFERENCE_KEY
import com.dinisoft.eyewitness.adapters.EyeAdapter
import com.dinisoft.eyewitness.receivers.NetworkChangeReceiver
import com.dinisoft.eyewitness.utils.NetworkUtil
import kotlinx.android.synthetic.main.content_main.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class ChatActivity : AppCompatActivity() {

    private val logTag = "EyewitnessAI"
    private val utterances = mutableListOf<Utterance>()
    private val reqCodeSpeechInput = 100
    private var maximumRetries = 1
    private var currentItemPosition = -1

    private var isNetworkChangeReceiverRegistered = false
    private var isWearBroadcastRevieverRegistered = false
    private var launchedFromWidget = false
    private var autoPromptForSpeech = false

    private lateinit var ttsManager: TTSManager
    private lateinit var mEyeAdapter: EyeAdapter
    private lateinit var wsip: String
    private lateinit var sharedPref: SharedPreferences
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var wearBroadcastReceiver: BroadcastReceiver

    private lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    var counter by Delegates.notNull<Int>()

    lateinit var mStorage: FirebaseStorage
    lateinit var mStorageRef: StorageReference
    lateinit var mRefVideo: StorageReference
    lateinit var mRefAudio: StorageReference
    lateinit var mRefPhoto: StorageReference
    lateinit var mRefPDF: StorageReference
    lateinit var mRefDOC: StorageReference

    var webSocketClient: WebSocketClient? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_chat_main)
        //setSupportActionBar(toolbar as androidx.appcompat.widget.Toolbar)

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

        wishMe()
        loadPreferences()

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

        ttsManager = TTSManager(this)
        mEyeAdapter = EyeAdapter(utterances, applicationContext, menuInflater)
        mEyeAdapter.setOnLongItemClickListener(object : EyeAdapter.OnLongItemClickListener {
            override fun itemLongClicked(v: View, position: Int) {
                currentItemPosition = position
                v.showContextMenu()
            }
        })


        kbMicSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("kbMicSwitch", isChecked)
            editor.apply()

            if (isChecked) {
                // Switch to mic
                micButton.visibility = View.VISIBLE
                utteranceInput.visibility = View.INVISIBLE
                sendUtterance.visibility = View.INVISIBLE
            } else {
                // Switch to keyboard
                micButton.visibility = View.INVISIBLE
                utteranceInput.visibility = View.VISIBLE
                sendUtterance.visibility = View.VISIBLE
            }
        }

        utteranceInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendUtterance()
                true
            } else {
                false
            }
        })
        micButton.setOnClickListener { promptSpeechInput() }
        sendUtterance.setOnClickListener { sendUtterance() }

        registerForContextMenu(cardList)

        //attach a listener to check for changes in state
        voxswitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("appReaderSwitch", isChecked)
            editor.apply()

            // stop tts from speaking if app reader disabled
            if (!isChecked) ttsManager.initQueue("")
        }

        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        llm.orientation = LinearLayoutManager.VERTICAL
        with(cardList) {
            setHasFixedSize(true)
            layoutManager = llm
            adapter = mEyeAdapter
        }

        //registerReceivers()

        // start the discovery activity (testing only)
        // startActivity(new Intent(this, DiscoveryActivity.class));
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_setup, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        var consumed = false
        when (item.itemId) {
            R.id.action_settings -> {
                //startActivity(Intent(this, MapActivity::class.java))
                consumed = true
            }
            R.id.action_home_mycroft_ai -> {
                //val intent = Intent(Intent.ACTION_VIEW,
                        //Uri.parse(getString(R.string.mycroft_website_url)))
                //startActivity(intent)
            }
        }

        return consumed && super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onContextItemSelected(item: MenuItem): Boolean {
        super.onContextItemSelected(item)
        if (item.itemId == R.id.user_resend) {
            // Resend user utterance
            sendMessage(utterances[currentItemPosition].utterance)
        } else if (item.itemId == R.id.user_copy || item.itemId == R.id.mycroft_copy) {
            // Copy utterance to clipboard
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = ClipData.newPlainText("text", utterances[currentItemPosition].utterance)
            clipboardManager.setPrimaryClip(data)
            showToast("Copied to clipboard")
        } else if (item.itemId == R.id.mycroft_share) {
            // Share utterance
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, utterances[currentItemPosition].utterance)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.action_share)))
        } else {
            return super.onContextItemSelected(item)
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendUtterance() {
        val utterance = utteranceInput.text.toString()
        if (utterance != "") {
            sendMessage(utterance)
            utteranceInput.text.clear()
        }
    }

    fun connectWebSocket() {
        val uri = deriveURI()

        if (uri != null) {
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(serverHandshake: ServerHandshake) {
                    Log.i("Websocket", "Opened")
                }

                override fun onMessage(s: String) {
                    // Log.i(TAG, s);
                    runOnUiThread(MessageParser(s, object : SafeCallback<Utterance> {
                        override fun call(param: Utterance) {
                            addData(param)
                        }
                    }))
                }

                override fun onClose(i: Int, s: String, b: Boolean) {
                    Log.i("Websocket", "Closed $s")

                }

                override fun onError(e: Exception) {
                    Log.i("Websocket", "Error " + e.message)
                }
            }
            webSocketClient!!.connect()
        }
    }

    private fun wishMe() {
        val rightNow: Calendar = Calendar.getInstance()
        rightNow.setTimeZone(TimeZone.getTimeZone("UTC+1"));
        val cHour12Format = rightNow[Calendar.HOUR_OF_DAY]

        if (cHour12Format >= 0 && cHour12Format < 12) {
            defaultMessageTextView.setText(R.string.morning)
        } else if (cHour12Format >= 12 && cHour12Format < 18) {
            defaultMessageTextView.setText(R.string.noon)
        } else {
            defaultMessageTextView.setText(R.string.evening)
        }

    }

    private fun addData(mycroftUtterance: Utterance) {
        utterances.add(mycroftUtterance)
        defaultMessageTextView.visibility = View.GONE
        mEyeAdapter.notifyItemInserted(utterances.size - 1)
        if (voxswitch.isChecked) {
            if (mycroftUtterance.from.id.equals(1)) {
                ttsManager.addQueue(mycroftUtterance.utterance)

            } else {
                //Do Nothing
            }

        }
        cardList.smoothScrollToPosition(mEyeAdapter.itemCount - 1)
    }



    private fun registerReceivers() {
        registerNetworkReceiver()
        registerWearBroadcastReceiver()
    }

    private fun registerNetworkReceiver() {
        if (!isNetworkChangeReceiverRegistered) {
            // set up the dynamic broadcast receiver for maintaining the socket
            networkChangeReceiver = NetworkChangeReceiver()
            //networkChangeReceiver.setMainActivityHandler(this)

            // set up the intent filters
            val connChange = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            val wifiChange = IntentFilter("android.net.wifi.WIFI_STATE_CHANGED")
            registerReceiver(networkChangeReceiver, connChange)
            registerReceiver(networkChangeReceiver, wifiChange)

            isNetworkChangeReceiverRegistered = true
        }
    }

    private fun registerWearBroadcastReceiver() {
        if (!isWearBroadcastRevieverRegistered) {
            wearBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    /*
                    val message = intent.getStringExtra(MYCROFT_WEAR_REQUEST_MESSAGE)
                    // send to mycroft
                    if (message != null) {
                        Log.d(logTag, "Wear message received: [$message] sending to Aboki")
                        sendMessage(message)
                    }
                    */

                }
            }

            //LocalBroadcastManager.getInstance(this).registerReceiver(wearBroadcastReceiver, IntentFilter(MYCROFT_WEAR_REQUEST))
            isWearBroadcastRevieverRegistered = true
        }
    }

    private fun unregisterReceivers() {
        unregisterBroadcastReceiver(networkChangeReceiver)
        unregisterBroadcastReceiver(wearBroadcastReceiver)

        isNetworkChangeReceiverRegistered = false
        isWearBroadcastRevieverRegistered = false
    }

    private fun unregisterBroadcastReceiver(broadcastReceiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    /**
     * This method will attach the correct path to the
     * [.wsip] hostname to allow for communication
     * with a Mycroft instance at that address.
     *
     *
     * If [.wsip] cannot be used as a hostname
     * in a [URI] (e.g. because it's null), then
     * this method will return null.
     *
     *
     * @return a valid uri, or null
     */
    private fun deriveURI(): URI? {
        return if (wsip.isNotEmpty()) {
            try {
                URI("ws://$wsip:8181/core")
            } catch (e: URISyntaxException) {
                Log.e(logTag, "Unable to build URI for websocket", e)
                null
            }
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendMessage(msg: String) {
        // let's keep it simple eh?
        //final String json = "{\"message_type\":\"recognizer_loop:utterance\", \"context\": null, \"metadata\": {\"utterances\": [\"" + msg + "\"]}}";
        val json = "{\"data\": {\"utterances\": [\"$msg\"]}, \"type\": \"recognizer_loop:utterance\", \"context\": null}"

        //val mItemSelected = intent.getStringExtra("Tag")

        try {
            //if (webSocketClient == null || webSocketClient!!.connection.isClosed) {
            // try and reconnect
            if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.NETWORK_STATUS_MOBILE || NetworkUtil.getConnectivityStatus(this) == NetworkUtil.NETWORK_STATUS_WIFI) { //TODO: add config to specify wifi only.
                //connectWebSocket(
                //showToast(resources.getString(R.string.websocket_connected))
            }
            //}

            // Add data to the recyler view adapter
            addData(Utterance(msg, UtteranceFrom.USER, "", ""))

            val currentUserID = auth.currentUser?.uid
            if (currentUserID != null) {

                db.collection("questions")
                        .get()
                        .addOnSuccessListener { result ->
                            // Get the total number of approved incident report from a particular state
                            val count = result.size()
                            for (document in result) {
                                val documentID = document.getString("documentID")
                                val message = document.getString("message")
                                val uid = document.getString("uid")
                                val incidentID = document.getString("incidentID")

                                var questions = message!!.split("?")
                                for (question in questions){
                                    if (question.contains(msg.toLowerCase().removeSuffix("?").removeSuffix(".").removeSuffix(" ").
                                            removeSuffix(",").removeSuffix("'").removeSuffix("%"))){
                                        //Toast.makeText(this@ChatActivity, msg, Toast.LENGTH_LONG).show()

                                        db.collection("answers")
                                                .whereEqualTo("questionID", documentID)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    // Get the total number of approved incident report from a particular state
                                                    if (!result.isEmpty()){
                                                        val count = result.size()
                                                        for (document in result) {
                                                            val documentID = document.getString("documentID")
                                                            val message = document.getString("message")
                                                            val uid = document.getString("uid")
                                                            val qid = document.getString("questionID")

                                                            var answers = message!!.split(".")
                                                            val random = Random()
                                                            val index = random.nextInt(answers.size)

                                                            // Add data to the recyler view adapter
                                                            addData(Utterance(answers[index], UtteranceFrom.ABOKI, "", ""))

                                                        }

                                                    } else {
                                                        // Add data to the recyler view adapter
                                                        addData(Utterance("Sorry! Something went wrong. Review your network connection or check back later.", UtteranceFrom.ABOKI, "", ""))
                                                    }


                                                }
                                                .addOnFailureListener { e ->
                                                    // Add data to the recyler view adapter
                                                    addData(Utterance("Sorry! Something went wrong. Review your network connection or check back later.", UtteranceFrom.ABOKI, "", ""))
                                                }
                                    }
                                }

                            }
                        }
                    }



            val handler = Handler()
            handler.postDelayed({
                // Actions to do after 1 seconds
                try {
                    //webSocketClient!!.send(json)

                    /*
                    if (msg.contains("Aboki", ignoreCase = true) || msg.contains("Hey", ignoreCase = true) || msg.contains("Hello", ignoreCase = true) || msg.contains("Hi", ignoreCase = true)
                            || msg.contains("Morning", ignoreCase = true) || msg.contains("Afternoon", ignoreCase = true) || msg.contains("Evening", ignoreCase = true)) {
                        val rightNow: Calendar = Calendar.getInstance()
                        rightNow.setTimeZone(TimeZone.getTimeZone("UTC+1"));
                        val cHour12Format = rightNow[Calendar.HOUR_OF_DAY]

                        // Morning
                        var mgreet1 = "Good Morning. How can i help you?."
                        var mgreet2 = "Good Morning. What can i do for you?."
                        var mgreet3 = "Barka Da Safiya. That's Good Morning in Hausa. What can i do for you?."
                        var mgreet4 = "E Kaaro. That's Good Morning in Yoruba. How can i help you?."
                        var mgreet5 = "Utụtụ ọma. That's Good Morning in Igbo. What can i do for you?."
                        val mgreet = arrayOf(mgreet1, mgreet2, mgreet3, mgreet4, mgreet5)
                        val mrandom = Random()
                        val mindex = mrandom.nextInt(mgreet.size)
                        //println(mgreet[index])

                        // Afternoon
                        var afgreet1 = "Good Afternoon. How can i help you?."
                        var afgreet2 = "Barka Da Rana. That's Good Afternoon in Hausa. What can i do for you?."
                        var afgreet3 = "E Kaasan. That's Good Afternoon in Yoruba. How can i help you?."
                        var afgreet4 = "Good Afternoon. I hope you are having a good day? What can i do for you?."
                        var afgreet5 = "Ehihie ọma. That's Good Afternoon in Igbo. How can i help you?."

                        val afgreet = arrayOf(afgreet1, afgreet2, afgreet3, afgreet4, afgreet5)
                        val afrandom = Random()
                        val afindex = afrandom.nextInt(afgreet.size)

                        // Evening
                        var evgreet1 = "Mgbede ọma. That's Good Evening in Igbo. How can i help you?."
                        var evgreet2 = "Good Evening. It's really a good time to talk to you. What can i do for you?."
                        var evgreet3 = "Good Evening. It's not too much to ask me anything. How can i help you?."
                        var evgreet4 = "Barka Da Yamma. That's Good Evening in Hausa. What can i do for you?."
                        var evgreet5 = "E Kaaale. That's Good Evening in Yoruba. How can i help you?."

                        val evgreet = arrayOf(evgreet1, evgreet2, evgreet3, evgreet4, evgreet5)
                        val evrandom = Random()
                        val evindex = evrandom.nextInt(evgreet.size)

                        if (cHour12Format >= 0 && cHour12Format < 12) {
                            addData(Utterance(mgreet[mindex], UtteranceFrom.ABOKI, "", ""))
                        } else if (cHour12Format >= 12 && cHour12Format < 18) {
                            addData(Utterance(afgreet[afindex], UtteranceFrom.ABOKI, "", ""))
                        } else {
                            addData(Utterance(evgreet[evindex], UtteranceFrom.ABOKI, "", ""))
                        }

                    } else if (msg.contains("How are you", ignoreCase = true) || msg.contains("How are you doing", ignoreCase = true) || msg.contains("What are you doing", ignoreCase = true) ||
                            msg.contains("What's up", ignoreCase = true) || msg.contains("Whats up", ignoreCase = true) || msg.contains("Tell me something", ignoreCase = true) || msg.contains("What can you say", ignoreCase = true) ||
                            msg.contains("What can you do", ignoreCase = true) || msg.contains("How can you help me", ignoreCase = true)) {

                        var mval1 = "I'm feeling good and I'm getting better. I can do a lot of things. Let me prove it. Would you like to ask me a question?."
                        var mval2 = "I'm having a great time! I was just looking up some fun things to do. You know, I like to try new things. How about you?"
                        var mval3 = "For a Computer software, I'd say I'm feeling pretty good. If you need any help, I'm ready to go."
                        var mval4 = "I'm feeling like a lean, mean, assisting machine! I can respond to your queries. How about you?"
                        var mval5 = "Nothing to look bad at all. Today has been great. I've been learning about a lot of different things. I can do many task that will surprise you. Please keep this between us."
                        val vquestion = arrayOf(mval1, mval2, mval3, mval4, mval5)
                        val vrandom = Random()
                        val vindex = vrandom.nextInt(vquestion.size)

                        addData(Utterance(vquestion[vindex], UtteranceFrom.ABOKI, "", ""))
                    } else {
                        addData(Utterance("Sorry! I forgot about what we were discussing. Would you like to remind me?.", UtteranceFrom.ABOKI, "", ""))
                    }

                     */
                } catch (exception: NetworkErrorException) {
                    showToast(resources.getString(R.string.websocket_closed))
                }
            }, 1000)

        } catch (exception: WebsocketNotConnectedException) {
            showToast(resources.getString(R.string.websocket_closed))
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



    /**
     * Showing google speech input dialog
     */
    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt))
        try {
            startActivityForResult(intent, reqCodeSpeechInput)
        } catch (a: ActivityNotFoundException) {
            showToast(getString(R.string.speech_not_supported))
        }

    }

    /**
     * Receiving speech input
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            reqCodeSpeechInput -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                    sendMessage(result!![0])
                }
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutDown()
        isNetworkChangeReceiverRegistered = false
        isWearBroadcastRevieverRegistered = false
    }

    public override fun onStart() {
        super.onStart()
        //recordVersionInfo()
        //registerReceivers()
        checkIfLaunchedFromWidget(intent)
    }

    public override fun onStop() {
        super.onStop()

        //unregisterReceivers()

        if (launchedFromWidget) {
            autoPromptForSpeech = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun loadPreferences() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // get mycroft-core ip address
        wsip = sharedPref.getString("ip", "")!!
        if (wsip!!.isEmpty()) {
            // eep, show the settings intent!
            startActivity(Intent(this, ChatActivity::class.java))
        } else if (webSocketClient == null || webSocketClient!!.connection.isClosed) {
            //connectWebSocket()
        }

        kbMicSwitch.isChecked = sharedPref.getBoolean("kbMicSwitch", true)
        if (kbMicSwitch.isChecked) {
            // Switch to mic
            micButton.visibility = View.VISIBLE
            utteranceInput.visibility = View.INVISIBLE
            sendUtterance.visibility = View.INVISIBLE
        } else {
            // Switch to keyboard
            micButton.visibility = View.INVISIBLE
            utteranceInput.visibility = View.VISIBLE
            sendUtterance.visibility = View.VISIBLE
        }

        // set app reader setting
        voxswitch.isChecked = sharedPref.getBoolean("appReaderSwitch", true)

        maximumRetries = Integer.parseInt(sharedPref.getString("maximumRetries", "1")!!)
    }

    private fun checkIfLaunchedFromWidget(intent: Intent) {
        val extras = getIntent().extras
        if (extras != null) {
            if (extras.containsKey("launchedFromWidget")) {
                launchedFromWidget = extras.getBoolean("launchedFromWidget")
                autoPromptForSpeech = extras.getBoolean("autoPromptForSpeech")
            }

            /*
            if (extras.containsKey(MYCROFT_WEAR_REQUEST_KEY_NAME)) {
                Log.d(logTag, "checkIfLaunchedFromWidget - extras contain key:$MYCROFT_WEAR_REQUEST_KEY_NAME")
                extras.getString(MYCROFT_WEAR_REQUEST_KEY_NAME)?.let { sendMessage(it) }
                getIntent().removeExtra(MYCROFT_WEAR_REQUEST_KEY_NAME)
            }
            */

        }

        if (autoPromptForSpeech) {
            promptSpeechInput()
            intent.putExtra("autoPromptForSpeech", false)
        }
    }

    private fun recordVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val editor = sharedPref.edit()
            editor.putInt(VERSION_CODE_PREFERENCE_KEY, packageInfo.versionCode)
            editor.putString(VERSION_NAME_PREFERENCE_KEY, packageInfo.versionName)
            editor.apply()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(logTag, "Couldn't find package info", e)
        }
    }

    private fun showToast(message: String) {
        //GuiUtilities.showToast(applicationContext, message)
    }
}