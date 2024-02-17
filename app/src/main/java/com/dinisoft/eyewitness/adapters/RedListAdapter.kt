package com.dinisoft.eyewitness.adapters

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dinisoft.eyewitness.*
import com.dinisoft.eyewitness.model.Incident
import com.dinisoft.eyewitness.model.RedList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.eyelens_card_layout.view.*
import kotlinx.android.synthetic.main.eyelens_card_layout.view.btn_dislike
import kotlinx.android.synthetic.main.eyelens_card_layout.view.btn_like
import kotlinx.android.synthetic.main.eyelens_card_layout.view.card_item_review
import kotlinx.android.synthetic.main.eyelens_card_layout.view.card_item_title
import kotlinx.android.synthetic.main.eyelens_card_layout.view.img_eyelens_content
import kotlinx.android.synthetic.main.eyelens_card_layout.view.txt_title_review_date
import kotlinx.android.synthetic.main.eyelens_card_layout.view.txt_title_review_info
import kotlinx.android.synthetic.main.eyelens_card_layout.view.txt_title_review_status
import kotlinx.android.synthetic.main.redlist_card_layout.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class RedListAdapter (private val redList: List<RedList>, val mContext: Context) : RecyclerView.Adapter<RedListAdapter.IncidentViewHolder>(){

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bindItems(redList[position])
        holder.mBtnLike.setOnClickListener {
            //if (holder.mBtnLike.text.equals("Like") && holder.mBtnDislike.text.equals("Dislike")) {
            holder.mBtnLike.text = "Liked"
            holder.mBtnDislike.isClickable = false
            val userID = holder.auth.currentUser!!.uid
            holder.db.collection("engagement")
                    .whereEqualTo("incidentID", holder.incidentID)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            for (document in result) {
                                val mlike = document.getString("likes")!!.toInt()
                                val uid = document.getString("uid").toString()
                                val documentID = document.getString("documentID").toString()
                                val mrate = mlike + 1

                                if (!userID.equals(uid, true)) {
                                    val modify = holder.db.collection("engagement").document(document.id)
                                    modify.update("likes", mrate.toString())
                                            .addOnSuccessListener {
                                                Log.d(holder.TAG, "DocumentSnapshot successfully written!")

                                            }
                                            .addOnFailureListener { e -> Log.w(holder.TAG, "Error writing document", e) }
                                } else {
                                    Toast.makeText(mContext, "You have already liked this complaint", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            holder.addEngagementToFb(userID, holder.incidentID, "1", "0")
                        }

                    }
            //}
        }

        holder.mBtnCall.setOnClickListener {
            ActivityCompat.requestPermissions(mContext as Activity, arrayOf(Manifest.permission.CALL_PHONE), holder.PHONE_PERMISSION_CODE)
            if (holder.incidentID != null){
                //mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", holder.incidentID))
                val currentUser = holder.auth.currentUser
                if (currentUser != null) {
                    holder.db.collection("agency")
                            .whereEqualTo("name", holder.xRedList.getAuthName())
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    val phone = document.getString("phone")
                                    val access = document.getString("accesscode")
                                    if (phone != null && access != null) {
                                        // Emergency call intent
                                        Toast.makeText(mContext, "Initiating a call to "+holder.xRedList.getAuthName(), Toast.LENGTH_LONG).show()
                                        val callIntent = Intent(Intent.ACTION_CALL)
                                        callIntent.data = Uri.parse("tel:" + phone)
                                        mContext.startActivity(callIntent)
                                    } else {
                                        Toast.makeText(mContext, holder.xRedList.getAuthName()+" have not been verified. Try another agency.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                }
            } else {
                Toast.makeText(mContext, "Sorry! You cannot perform this operation at this time", Toast.LENGTH_LONG).show()
            }
        }

        holder.mBtnDislike.setOnClickListener {
            //if (holder.mBtnDislike.text.equals("Dislike") && holder.mBtnLike.text.equals("Like")) {
            holder.mBtnDislike.text = "Disliked"
            holder.mBtnLike.isClickable = false
            val userID = holder.auth.currentUser!!.uid
            holder.db.collection("engagement")
                    .whereEqualTo("incidentID", holder.incidentID)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            for (document in result) {
                                val mdislike = document.getString("dislikes")!!.toInt()
                                val uid = document.getString("uid").toString()
                                val documentID = document.getString("documentID").toString()
                                val mrate = mdislike + 1

                                if (!userID.equals(uid, true)) {
                                    val modify = holder.db.collection("engagement").document(documentID)
                                    modify.update("dislikes", mrate.toString())
                                            .addOnSuccessListener {
                                                Log.d(holder.TAG, "DocumentSnapshot successfully written!")
                                            }
                                            .addOnFailureListener { e -> Log.w(holder.TAG, "Error writing document", e) }
                                } else {
                                    Toast.makeText(mContext, "You have already disliked this complaint", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            holder.addEngagementToFb(userID, holder.incidentID, "0", "1")
                        }

                    }
        }

        holder.mCardTitleItem.setOnClickListener {
            // Start Exo activity to view the evidence file
            val evidURL = holder.xRedList.getPersonPhotograph()
            if (evidURL.toString().contains("-image", true)) {
                val cleanEvid = holder.xRedList.getPersonPhotograph().toString().substring(0, holder.xRedList.getPersonPhotograph().toString().lastIndexOf("-image"))
                holder.downloadImageToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-video", true)) {
                val cleanEvid = holder.xRedList.getPersonPhotograph().toString().substring(0, holder.xRedList.getPersonPhotograph().toString().lastIndexOf("-video"))
                holder.downloadVideoToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-audio", true)) {
                val cleanEvid = holder.xRedList.getPersonPhotograph().toString().substring(0, holder.xRedList.getPersonPhotograph().toString().lastIndexOf("-audio"))
                holder.downloadAudioToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-docx", true)) {
                val cleanEvid = holder.xRedList.getPersonPhotograph().toString().substring(0, holder.xRedList.getPersonPhotograph().toString().lastIndexOf("-docx"))
                holder.downloadDocToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-pdf", true)) {
                val cleanEvid = holder.xRedList.getPersonPhotograph().toString().substring(0, holder.xRedList.getPersonPhotograph().toString().lastIndexOf("-pdf"))
                holder.downloadPdfToLocalFile(cleanEvid, mContext)
            }
        }

    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return redList.size
    }

    fun deleteItem(cardList: RecyclerView){
        cardList.removeView(cardList)
        cardList.adapter!!.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): IncidentViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.redlist_card_layout, viewGroup, false)
        return IncidentViewHolder(v)
    }


    //the class is holding the list view
    class IncidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val TAG: String = "--RedListAdapter"

        lateinit var mBtnLike: AppCompatButton
        lateinit var mBtnCall: AppCompatButton
        lateinit var mBtnDislike: AppCompatButton
        lateinit var mCardReviewStat: CardView
        lateinit var mCardTitleItem: CardView
        lateinit var auth: FirebaseAuth
        lateinit var db: FirebaseFirestore
        lateinit var xRedList: RedList

        lateinit var incidentID: String

        lateinit var mStorage: FirebaseStorage
        lateinit var mStorageRef: StorageReference
        lateinit var mRefVideo: StorageReference
        lateinit var mRefAudio: StorageReference
        lateinit var mRefPhoto: StorageReference
        lateinit var mRefPDF: StorageReference
        lateinit var mRefDOC: StorageReference

        val PHONE_PERMISSION_CODE = 104

        fun bindItems(mRedList: RedList) {
            mBtnLike = itemView.btn_like as AppCompatButton
            mBtnCall = itemView.btn_call as AppCompatButton
            mBtnDislike = itemView.btn_dislike as AppCompatButton
            mCardReviewStat = itemView.card_item_review as CardView
            mCardTitleItem = itemView.card_item_title as CardView

            auth = Firebase.auth
            db = Firebase.firestore

            mStorage = FirebaseStorage.getInstance()
            mStorageRef = mStorage.getReference()
            mRefPhoto = mStorageRef.child("eyewitness/evidence/image/" + UUID.randomUUID().toString())
            mRefVideo = mStorageRef.child("eyewitness/evidence/video/" + UUID.randomUUID().toString())
            mRefAudio = mStorageRef.child("eyewitness/evidence/audio/" + UUID.randomUUID().toString())
            mRefDOC = mStorageRef.child("eyewitness/evidence/docx/" + UUID.randomUUID().toString())
            mRefPDF = mStorageRef.child("eyewitness/evidence/pdf/" + UUID.randomUUID().toString())

            xRedList = mRedList
            //mMapActivity = MapActivity()

            val user = auth.currentUser!!.displayName
            val currentUID =  auth.currentUser!!.uid
            if (user != null){
                itemView.txt_title_review_date.text = mRedList.getPersonMetatime()
                itemView.txt_title_review_status.text = mRedList.getPersonStatus()
                itemView.txt_title_review_info.text = mRedList.getAlertInfo()
                itemView.txt_title_reward_status.text = "Reward: "+mRedList.getPersonReward()
                incidentID = mRedList.getDocumentID().toString()

                db.collection("engagement")
                        .whereEqualTo("incidentID", incidentID)
                        .get()
                        .addOnSuccessListener { result ->
                            if (!result.isEmpty) {
                                for (document in result) {
                                    val uid = document.getString("uid")
                                    val incidentID = document.getString("incidentID")
                                    val mlike = document.getString("likes")
                                    val mdislike = document.getString("dislikes")

                                    // Format the eyelence action buttons
                                    if (!currentUID.equals(uid, true)) {
                                        mBtnLike.text = "Like" //Do Nothing
                                        mBtnDislike.text = "Dislike" // Do Nothing
                                    } else {
                                        itemView.btn_like.text = mlike + " LK"
                                        mBtnLike.isClickable = false
                                        itemView.btn_dislike.text = mdislike + " DLK"
                                        mBtnDislike.isClickable = false
                                    }
                                }

                            } else {
                                mBtnLike.text = "Like" //Do Nothing
                                mBtnDislike.text = "Dislike" // Do Nothing
                            }
                        }

                // Glide to load image thumbnail
                val evidURL = mRedList.getPersonPhotograph()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = mRedList.getPersonPhotograph().toString().substring(0, mRedList.getPersonPhotograph().toString().lastIndexOf("-image"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = mRedList.getPersonPhotograph().toString().substring(0, mRedList.getPersonPhotograph().toString().lastIndexOf("-video"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = mRedList.getPersonPhotograph().toString().substring(0, mRedList.getPersonPhotograph().toString().lastIndexOf("-audio"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = mRedList.getPersonPhotograph().toString().substring(0, mRedList.getPersonPhotograph().toString().lastIndexOf("-docx"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = mRedList.getPersonPhotograph().toString().substring(0, mRedList.getPersonPhotograph().toString().lastIndexOf("-pdf"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                }

            }

        }


        fun addEngagementToFb(uid: String, incidentID: String, likes: String, dislikes: String){

            val engage = hashMapOf(
                    "uid" to uid,
                    "documentID" to "",
                    "incidentID" to incidentID,
                    "likes" to likes,
                    "dislikes" to dislikes

            )

            //if (status.equals("user", true)){

            db.collection("engagement")
                    .add(engage)
                    .addOnSuccessListener { documentReference ->
                        //Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                        // Update the collection and add docuemnt ID
                        val mDocumentID = documentReference.id
                        val modify = db.collection("engagement").document(documentReference.id)
                        modify.update("documentID", mDocumentID)
                                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }


        }



        fun downloadVideoToLocalFile(evidURL: String, mContext: Context) {
            if (evidURL != null) {

                mRefVideo = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(mContext)
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
                                        mContext.startActivity(Intent(mContext, ExoActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))
                                    }
                                }
                                .addOnFailureListener { exception ->

                                    Toast.makeText(mContext, exception.message, Toast.LENGTH_LONG).show()
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
                    mContext.startActivity(Intent(mContext, ExoActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))
                }


            } else {
                Toast.makeText(mContext, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }

        }


        fun downloadAudioToLocalFile(evidURL: String, mContext: Context) {
            if (evidURL != null) {

                mRefAudio = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(mContext)
                progress.setTitle("Loading....")
                progress.show()

                val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
                val rootPath = File(mContext.filesDir, "EyeWitness")
                val subPath = File(rootPath, "Audio")
                if (!subPath.exists()) {
                    subPath.mkdirs();
                }
                val localFile = File(subPath, mStamp.toString() + ".mp3")

                try {
                    mRefAudio.getFile(localFile)
                            .addOnSuccessListener {
                                if (localFile.canRead()) {
                                    progress.dismiss()
                                    mContext.startActivity(Intent(mContext, ExoActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))

                                }
                            }
                            .addOnFailureListener { exception ->

                                Toast.makeText(mContext, exception.message, Toast.LENGTH_LONG).show()
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
                Toast.makeText(mContext, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }


        }


        fun downloadImageToLocalFile(evidURL: String, mContext: Context) {
            if (evidURL != null) {

                mRefPhoto = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(mContext)
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
                                    mContext.startActivity(Intent(mContext, ImageActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))

                                }
                            }
                            .addOnFailureListener { exception ->

                                Toast.makeText(mContext, exception.message, Toast.LENGTH_LONG).show()
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
                Toast.makeText(mContext, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }


        }

        fun downloadDocToLocalFile(evidURL: String, mContext: Context) {
            if (evidURL != null) {

                mRefDOC = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(mContext)
                progress.setTitle("Loading....")
                progress.show()

                val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
                val rootPath = File(mContext.filesDir, "EyeWitness")
                val subPath = File(rootPath, "Docx")
                if (!subPath.exists()) {
                    subPath.mkdirs();
                }
                val localFile = File(subPath, mStamp.toString() + ".docx")

                try {
                    mRefDOC.getFile(localFile)
                            .addOnSuccessListener {
                                if (localFile.canRead()) {
                                    progress.dismiss()
                                    // Load Pdf from local file
                                    mContext.startActivity(Intent(mContext, PDFActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))

                                }
                            }
                            .addOnFailureListener { exception ->

                                Toast.makeText(mContext, exception.message, Toast.LENGTH_LONG).show()
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
                Toast.makeText(mContext, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }


        }


        fun downloadPdfToLocalFile(evidURL: String, mContext: Context) {
            if (evidURL != null) {

                mRefPDF = mStorage.getReferenceFromUrl(evidURL)
                val progress = ProgressDialog(mContext)
                progress.setTitle("Loading....")
                progress.show()

                val mStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(Date())
                val rootPath = File(mContext.filesDir, "EyeWitness")
                val subPath = File(rootPath, "Pdf")
                if (!subPath.exists()) {
                    subPath.mkdirs();
                }
                val localFile = File(subPath, mStamp.toString() + ".pdf")

                try {
                    mRefPDF.getFile(localFile)
                            .addOnSuccessListener {
                                if (localFile.canRead()) {
                                    progress.dismiss()
                                    // Load Pdf from local file
                                    mContext.startActivity(Intent(mContext, PDFActivity::class.java).putExtra("Tag", localFile.absoluteFile.toString()))
                                }
                            }
                            .addOnFailureListener { exception ->

                                Toast.makeText(mContext, exception.message, Toast.LENGTH_LONG).show()
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
                Toast.makeText(mContext, "Upload file before downloading", Toast.LENGTH_LONG).show()
            }


        }

    }
}