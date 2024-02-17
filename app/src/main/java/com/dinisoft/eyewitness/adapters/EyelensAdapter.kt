package com.dinisoft.eyewitness.adapters

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.dinisoft.eyewitness.*
import com.dinisoft.eyewitness.model.Incident
import kotlinx.android.synthetic.main.eyelens_card_layout.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class EyelensAdapter (private val incidentList: List<Incident>, val mContext: Context) : RecyclerView.Adapter<EyelensAdapter.IncidentViewHolder>(){

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bindItems(incidentList[position])
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

        holder.mBtnChat.setOnClickListener {
            //mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java))
            if (holder.incidentID != null){
                mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", holder.incidentID))
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
            val evidURL = holder.xIncident.getIncidentEvidenceURL()
            if (evidURL.toString().contains("-image", true)) {
                val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                holder.downloadImageToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-video", true)) {
                val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                holder.downloadVideoToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-audio", true)) {
                val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                holder.downloadAudioToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-docx", true)) {
                val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                holder.downloadDocToLocalFile(cleanEvid, mContext)
            } else if (evidURL.toString().contains("-pdf", true)) {
                val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                holder.downloadPdfToLocalFile(cleanEvid, mContext)
            }
        }

    }

    //this method is giving the size of the list
    override fun getItemCount(): Int {
        return incidentList.size
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }

    fun deleteItem(cardList: RecyclerView){
        cardList.removeView(cardList)
        cardList.adapter!!.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): IncidentViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.eyelens_card_layout, viewGroup, false)
        return IncidentViewHolder(v)
    }


    //the class is holding the list view
    class IncidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val TAG: String = "--EyelensAdapter"

        lateinit var mBtnLike: AppCompatButton
        lateinit var mBtnChat: AppCompatButton
        lateinit var mBtnDislike: AppCompatButton
        lateinit var mCardReviewStat: CardView
        lateinit var mCardTitleItem: CardView
        lateinit var auth: FirebaseAuth
        lateinit var db: FirebaseFirestore
        lateinit var xIncident: Incident

        lateinit var incidentID: String

        lateinit var mStorage: FirebaseStorage
        lateinit var mStorageRef: StorageReference
        lateinit var mRefVideo: StorageReference
        lateinit var mRefAudio: StorageReference
        lateinit var mRefPhoto: StorageReference
        lateinit var mRefPDF: StorageReference
        lateinit var mRefDOC: StorageReference

        fun bindItems(mIncident: Incident) {
            mBtnLike = itemView.btn_like as AppCompatButton
            mBtnChat = itemView.btn_chat as AppCompatButton
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

            xIncident = mIncident
            //mMapActivity = MapActivity()

            val code = Math.random() / 2  // Generate numbers to hide users name
            val user = auth.currentUser!!.displayName
            val currentUID =  auth.currentUser!!.uid
            if (user != null){
                itemView.txt_title_review_date.text = mIncident.getIncidentDate()
                itemView.txt_title_review_status.text = code.toString() // Hide users name
                itemView.txt_title_review_info.text = mIncident.getIncidentReviewInfo()
                incidentID = mIncident.getDocumentID().toString()

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
                val evidURL = mIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_eyelens_content)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
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
                    subPath.mkdirs();
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