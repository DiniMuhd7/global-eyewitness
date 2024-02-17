package com.dinisoft.eyewitness.adapters

/*
 *  Copyright (c) 2021. Shamsudeen A. Muhammed, Dinisoft Technology Ltd
 *
 *  This file is part of Eyewitness-Android a client for Eyewitness Core.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
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
import com.google.android.material.snackbar.Snackbar
import infix.imrankst1221.rocket.library.utility.UtilMethods
import kotlinx.android.synthetic.main.compaint_card_layout.view.*
import kotlinx.android.synthetic.main.compaint_card_layout.view.btn_dislike
import kotlinx.android.synthetic.main.compaint_card_layout.view.btn_like
import kotlinx.android.synthetic.main.compaint_card_layout.view.img_remove
import kotlinx.android.synthetic.main.review_card_layout.view.*
import kotlinx.android.synthetic.main.review_card_layout.view.card_item_review
import kotlinx.android.synthetic.main.review_card_layout.view.txt_title_review_date
import kotlinx.android.synthetic.main.review_card_layout.view.txt_title_review_info
import kotlinx.android.synthetic.main.review_card_layout.view.txt_title_review_status
import kotlinx.android.synthetic.main.review_card_layout.view.txt_title_review_type
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */
class ComplaintAdapter(val incidentList: List<Incident>, val mContext: Context) : RecyclerView.Adapter<ComplaintAdapter.IncidentViewHolder>() {

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bindItems(incidentList[position])
        holder.mBtnLike.setOnClickListener {
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
        }


        holder.mBtnView.setOnClickListener {
            if (holder.itemView.txt_title_review_type.text.equals("Officer Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Accident Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Murder Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Sexual-Assault Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Theft Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Terrorism Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType() + " Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Firehazard Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Flooding Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Illegal-Drugs Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                }
            } else if (holder.itemView.txt_title_review_type.text.equals("Assault Incident Report")) {
                val evidURL = holder.xIncident.getIncidentEvidenceURL()
                if (evidURL.toString().contains("-image", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-image"))
                    holder.showMarkerDialogImage(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-video", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                    holder.showMarkerDialogVideo(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-audio", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                    holder.showMarkerDialogAudio(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-docx", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                    holder.showMarkerDialogDocx(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                } else if (evidURL.toString().contains("-pdf", true)) {
                    val cleanEvid = holder.xIncident.getIncidentEvidenceURL().toString().substring(0, holder.xIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                    holder.showMarkerDialogPDF(cleanEvid, holder.xIncident.getIncidentType()+" Incident Report", mContext, holder.db)
                }
            }
        }

        holder.mBtnDislike.setOnClickListener {
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

        holder.mCardItemEvid.setOnClickListener {
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


        holder.mBtnRemove.setOnClickListener{
            Toast.makeText(mContext, "Deleting content, please wait...", Toast.LENGTH_SHORT).show()
            val evidURL = holder.xIncident.getIncidentEvidenceURL()
            if (holder.itemView.txt_title_review_type.text.equals("Officer Incident Report")) {
                //holder.itemView.txt_title_review_status.text="Loading..."
                holder.db.collection("officer")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("officer").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Accident Incident Report")) {
                //holder.itemView.txt_title_review_status.text="Loading..."
                holder.db.collection("accident")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("accident").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Murder Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("murder")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("murder").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Theft Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("theft")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("theft").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Sexual-Assault Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("sexual-assault")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("sexual-assault").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Terrorism Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("terrorism")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("terrorism").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Firehazard Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("firehazard")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("firehazard").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Flooding Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("flooding")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("flooding").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }

            } else if (holder.itemView.txt_title_review_type.text.equals("Illegal-Drugs Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("illegal-drugs")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("illegal-drugs").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }

            } else if (holder.itemView.txt_title_review_type.text.equals("Assault Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("assault")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Delete document from collection
                                    holder.db.collection("assault").document(document.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->  }

                                } else {
                                    Toast.makeText(mContext, "Unable to delete content. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->

                        }


            }
        }


        holder.mBtnCopy.setOnClickListener{
            Toast.makeText(mContext, "Copying complaint code, please wait...", Toast.LENGTH_SHORT).show()
            val evidURL = holder.xIncident.getIncidentEvidenceURL()
            if (holder.itemView.txt_title_review_type.text.equals("Officer Incident Report")) {
                //holder.itemView.txt_title_review_status.text="Loading..."
                holder.db.collection("officer")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Accident Incident Report")) {
                //holder.itemView.txt_title_review_status.text="Loading..."
                holder.db.collection("accident")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Murder Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("murder")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Theft Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("theft")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Sexual-Assault Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("sexual-assault")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Terrorism Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("terrorism")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Firehazard Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("firehazard")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }
            } else if (holder.itemView.txt_title_review_type.text.equals("Flooding Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("flooding")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }

            } else if (holder.itemView.txt_title_review_type.text.equals("Illegal-Drugs Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("illegal-drugs")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->


                        }

            } else if (holder.itemView.txt_title_review_type.text.equals("Assault Incident Report")) {
                //holder.itemView.txt_title_review_status.text = "Loading..."
                holder.db.collection("assault")
                        .whereEqualTo("evidenceURL", evidURL)
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val documentID = document.getString("documentID").toString()
                                val status = document.getString("status").toString()
                                if (!documentID.isEmpty()) {
                                    // Copy document from collection to clipboard
                                    val clipboardManager = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val data = ClipData.newPlainText("text", document.id)
                                    clipboardManager.setPrimaryClip(data)
                                    Toast.makeText(mContext, "Copied to clipboard.", Toast.LENGTH_LONG).show()

                                } else {
                                    Toast.makeText(mContext, "Unable to copy complaint code. Try again.", Toast.LENGTH_LONG).show()
                                }

                            }

                        }
                        .addOnFailureListener { e ->

                        }


            }
        }


    }

        //this method is giving the size of the list
        override fun getItemCount(): Int {
            return incidentList.size
        }

        fun deleteItem(cardList: RecyclerView){
            cardList.removeView(cardList)
            cardList.adapter!!.notifyDataSetChanged()
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): IncidentViewHolder {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.compaint_card_layout, viewGroup, false)
            return IncidentViewHolder(v)
        }


        //the class is holding the list view
        class IncidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val TAG: String = "--ComplaintAdapter"

            lateinit var mBtnLike: AppCompatButton
            lateinit var mBtnView: AppCompatButton
            lateinit var mBtnDislike: AppCompatButton
            lateinit var mBtnRemove: ImageView
            lateinit var mBtnCopy: ImageView
            lateinit var mCardReviewStat: CardView
            lateinit var mCardItemEvid: CardView
            lateinit var auth: FirebaseAuth
            lateinit var db: FirebaseFirestore
            lateinit var xIncident: Incident
            lateinit var mMapActivity: MapActivity
            lateinit var incidentID: String

            private lateinit var mMarkerDialogImagePopUp: PopupWindow
            private lateinit var mMarkerDialogVideoPopUp: PopupWindow
            private lateinit var mMarkerDialogAudioPopUp: PopupWindow
            private lateinit var mMarkerDialogDocxPopUp: PopupWindow
            private lateinit var mMarkerDialogPdfPopUp: PopupWindow

            private lateinit var mWeatherDialogVideoPopUp: PopupWindow
            private lateinit var mWeatherDialogAudioPopUp: PopupWindow
            private lateinit var mWeatherDialogImagePopUp: PopupWindow
            private lateinit var mWeatherDialogPdfPopUp: PopupWindow
            private lateinit var mWeatherDialogDocxPopUp: PopupWindow

            lateinit var mStorage: FirebaseStorage
            lateinit var mStorageRef: StorageReference
            lateinit var mRefVideo: StorageReference
            lateinit var mRefAudio: StorageReference
            lateinit var mRefPhoto: StorageReference
            lateinit var mRefPDF: StorageReference
            lateinit var mRefDOC: StorageReference

            fun bindItems(mIncident: Incident) {
                mBtnLike = itemView.btn_like as AppCompatButton
                mBtnView = itemView.btn_view as AppCompatButton
                mBtnDislike = itemView.btn_dislike as AppCompatButton
                mBtnRemove = itemView.img_remove as ImageView
                mBtnCopy = itemView.img_copyy as ImageView
                mCardReviewStat = itemView.card_item_review as CardView
                mCardItemEvid = itemView.card_item_complaint as CardView

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
                mMapActivity = MapActivity()

                itemView.txt_title_review_date.text = mIncident.getIncidentDate()
                itemView.txt_title_review_status.text = mIncident.getIncidentReviewStatus()
                itemView.txt_title_review_info.text = mIncident.getIncidentReviewInfo()
                itemView.txt_title_review_type.text = mIncident.getIncidentType() + " Incident Report"

                if ((itemView.txt_title_review_status.text as String?).equals("Approved", true)) {
                    mCardReviewStat.setCardBackgroundColor(Color.GREEN)
                } else if ((itemView.txt_title_review_status.text as String?).equals("Rejected", true)) {
                    mCardReviewStat.setCardBackgroundColor(Color.RED)
                } else if ((itemView.txt_title_review_status.text as String?).equals("Under Review", true)) {
                    mCardReviewStat.setCardBackgroundColor(Color.parseColor("#FF9800"))
                }

                val user = auth.currentUser!!.displayName
                val currentUID =  auth.currentUser!!.uid
                if (user != null){
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
                        Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_complaint)
                    } else if (evidURL.toString().contains("-video", true)) {
                        val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-video"))
                        Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_complaint)
                    } else if (evidURL.toString().contains("-audio", true)) {
                        val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-audio"))
                        Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_complaint)
                    } else if (evidURL.toString().contains("-docx", true)) {
                        val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-docx"))
                        Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_complaint)
                    } else if (evidURL.toString().contains("-pdf", true)) {
                        val cleanEvid = mIncident.getIncidentEvidenceURL().toString().substring(0, mIncident.getIncidentEvidenceURL().toString().lastIndexOf("-pdf"))
                        Glide.with(itemView.context).asBitmap().load(cleanEvid).into(itemView.img_complaint)
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



            fun showMarkerDialogVideo(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_marker, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mMarkerDialogVideoPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mMarkerDialogVideoPopUp.setBackgroundDrawable(colorDrawable)
                mMarkerDialogVideoPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mMarkerDialogVideoPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mMarkerDialogVideoPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mMarkerDialogVideoPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
                //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
                val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
                val btnView = popUpView.findViewById<View>(R.id.img_open) as ImageView
                val btnShare = popUpView.findViewById<View>(R.id.img_share) as ImageView
                val btnMessage = popUpView.findViewById<View>(R.id.img_message) as ImageView

                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView
                val txtMarkerDesc = popUpView.findViewById<View>(R.id.txt_marker_desc) as TextView
                val txtMarkerSubType = popUpView.findViewById<View>(R.id.txt_marker_ctype) as TextView


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title)
                                    mMarkerImage.setImageResource(R.drawable.police)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title)
                                    mMarkerImage.setImageResource(R.drawable.accident)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.rape)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.robbery)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.murder)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.terrorist)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.flood)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.fire)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.drug)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }

                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))


                btnConfirm.setOnClickListener {
                    mMarkerDialogVideoPopUp.dismiss()
                }


                btnCall.setOnClickListener {
                    // Show Weather Dialog
                    showWeatherDialogVideo(evid, title, mContext, dbRead)
                    mMarkerDialogVideoPopUp.dismiss()
                }

                btnView.setOnClickListener { view: View? ->
                    // Download video to local file
                    downloadVideoToLocalFile(evid, mContext)
                    mMarkerDialogVideoPopUp.dismiss()
                }


                btnMessage.setOnClickListener { view: View? ->
                    if (::incidentID.isInitialized){
                        mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", incidentID))
                    } else {
                        Toast.makeText(mContext, "Sorry! You cannot perform this operation at this time", Toast.LENGTH_LONG).show()
                    }
                    mMarkerDialogVideoPopUp.dismiss()
                }

                btnShare.setOnClickListener {
                    UtilMethods.shareTheApp(mContext, title+" \n"+" EvidenceURL: "+evid+"\n"+" To learn more about the incident, " +
                            "Download eyewitness app from play store. Click here:"+" https://play.google.com/store/apps/details?id="+mContext.packageName+"/")
                    mMarkerDialogVideoPopUp.dismiss()
                }


            }


            fun showMarkerDialogAudio(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_marker, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mMarkerDialogAudioPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mMarkerDialogAudioPopUp.setBackgroundDrawable(colorDrawable)
                mMarkerDialogAudioPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mMarkerDialogAudioPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mMarkerDialogAudioPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mMarkerDialogAudioPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
                //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
                val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
                val btnView = popUpView.findViewById<View>(R.id.img_open) as ImageView
                val btnShare = popUpView.findViewById<View>(R.id.img_share) as ImageView
                val btnMessage = popUpView.findViewById<View>(R.id.img_message) as ImageView


                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView
                val txtMarkerDesc = popUpView.findViewById<View>(R.id.txt_marker_desc) as TextView
                val txtMarkerSubType = popUpView.findViewById<View>(R.id.txt_marker_ctype) as TextView


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.police)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title)
                                    mMarkerImage.setImageResource(R.drawable.accident)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.rape)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.robbery)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.murder)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.terrorist)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.fire)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.flood)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.drug)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }



                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))


                btnConfirm.setOnClickListener {
                    mMarkerDialogAudioPopUp.dismiss()

                }


                btnCall.setOnClickListener {
                    // Show Weather Dialog
                    showWeatherDialogAudio(evid, title, mContext, dbRead)
                    mMarkerDialogAudioPopUp.dismiss()


                }

                btnView.setOnClickListener { view: View? ->
                    // Download audio to local file
                    downloadAudioToLocalFile(evid, mContext)
                    mMarkerDialogAudioPopUp.dismiss()
                }


                btnMessage.setOnClickListener { view: View? ->
                    if (::incidentID.isInitialized){
                        mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", incidentID))
                    } else {
                        Toast.makeText(mContext, "Sorry! You cannot perform this operation at this time", Toast.LENGTH_LONG).show()
                    }
                    mMarkerDialogAudioPopUp.dismiss()
                }

                btnShare.setOnClickListener {
                    UtilMethods.shareTheApp(mContext, title+" \n"+" EvidenceURL: "+evid+"\n"+" To learn more about the incident, " +
                            "Download eyewitness app from play store. Click here:"+" https://play.google.com/store/apps/details?id="+mContext.packageName+"/")
                    mMarkerDialogAudioPopUp.dismiss()
                }


            }


            fun showMarkerDialogImage(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_marker, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mMarkerDialogImagePopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mMarkerDialogImagePopUp.setBackgroundDrawable(colorDrawable)
                mMarkerDialogImagePopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mMarkerDialogImagePopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mMarkerDialogImagePopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mMarkerDialogImagePopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
                //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
                val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
                val btnView = popUpView.findViewById<View>(R.id.img_open) as ImageView
                val btnShare = popUpView.findViewById<View>(R.id.img_share) as ImageView
                val btnMessage = popUpView.findViewById<View>(R.id.img_message) as ImageView


                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView
                val txtMarkerDesc = popUpView.findViewById<View>(R.id.txt_marker_desc) as TextView
                val txtMarkerSubType = popUpView.findViewById<View>(R.id.txt_marker_ctype) as TextView

                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.police)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title)
                                    mMarkerImage.setImageResource(R.drawable.accident)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.rape)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.robbery)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.murder)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.terrorist)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.fire)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.flood)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.drug)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }


                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))


                btnConfirm.setOnClickListener {
                    mMarkerDialogImagePopUp.dismiss()

                }


                btnCall.setOnClickListener {
                    // Show Weather Dialog
                    showWeatherDialogImage(evid, title, mContext, dbRead)
                    mMarkerDialogImagePopUp.dismiss()


                }

                btnView.setOnClickListener { view: View? ->
                    // Download video to local file
                    downloadImageToLocalFile(evid, mContext)
                    mMarkerDialogImagePopUp.dismiss()
                }


                btnMessage.setOnClickListener { view: View? ->
                    if (::incidentID.isInitialized){
                        mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", incidentID))
                    } else {
                        Toast.makeText(mContext, "Sorry! You cannot perform this operation at this time", Toast.LENGTH_LONG).show()
                    }
                    mMarkerDialogImagePopUp.dismiss()
                }

                btnShare.setOnClickListener {
                    UtilMethods.shareTheApp(mContext, title+" \n"+" EvidenceURL: "+evid+"\n"+" To learn more about the incident, " +
                            "Download eyewitness app from play store. Click here:"+" https://play.google.com/store/apps/details?id="+mContext.packageName+"/")
                    mMarkerDialogImagePopUp.dismiss()
                }


            }


            fun showMarkerDialogDocx(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_marker, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mMarkerDialogDocxPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mMarkerDialogDocxPopUp.setBackgroundDrawable(colorDrawable)
                mMarkerDialogDocxPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mMarkerDialogDocxPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mMarkerDialogDocxPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mMarkerDialogDocxPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
                //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
                val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
                val btnView = popUpView.findViewById<View>(R.id.img_open) as ImageView
                val btnShare = popUpView.findViewById<View>(R.id.img_share) as ImageView
                val btnMessage = popUpView.findViewById<View>(R.id.img_message) as ImageView


                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView
                val txtMarkerDesc = popUpView.findViewById<View>(R.id.txt_marker_desc) as TextView
                val txtMarkerSubType = popUpView.findViewById<View>(R.id.txt_marker_ctype) as TextView


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.police)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title)
                                    mMarkerImage.setImageResource(R.drawable.accident)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.rape)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.robbery)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.murder)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.terrorist)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.fire)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.flood)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.drug)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-docx")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }


                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))


                btnConfirm.setOnClickListener {
                    mMarkerDialogDocxPopUp.dismiss()

                }


                btnCall.setOnClickListener {
                    // Show Weather Dialog
                    showWeatherDialogDocx(evid, title, mContext, dbRead)
                    mMarkerDialogDocxPopUp.dismiss()
                }

                btnView.setOnClickListener { view: View? ->
                    // Download video to local file
                    downloadDocToLocalFile(evid, mContext)
                    mMarkerDialogDocxPopUp.dismiss()
                }


                btnMessage.setOnClickListener { view: View? ->
                    if (::incidentID.isInitialized){
                        mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", incidentID))
                    } else {
                        Toast.makeText(mContext, "Sorry! You cannot perform this operation at this time", Toast.LENGTH_LONG).show()
                    }
                    mMarkerDialogDocxPopUp.dismiss()
                }

                btnShare.setOnClickListener {
                    UtilMethods.shareTheApp(mContext, title+" \n"+" EvidenceURL: "+evid+"\n"+" To learn more about the incident, " +
                            "Download eyewitness app from play store. Click here:"+" https://play.google.com/store/apps/details?id="+mContext.packageName+"/")
                    mMarkerDialogDocxPopUp.dismiss()
                }


            }


            fun showMarkerDialogPDF(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_marker, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mMarkerDialogPdfPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mMarkerDialogPdfPopUp.setBackgroundDrawable(colorDrawable)
                mMarkerDialogPdfPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mMarkerDialogPdfPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mMarkerDialogPdfPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mMarkerDialogPdfPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                //val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
                //val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton
                val btnCall = popUpView.findViewById<View>(R.id.img_call) as ImageView
                val btnView = popUpView.findViewById<View>(R.id.img_open) as ImageView
                val btnShare = popUpView.findViewById<View>(R.id.img_share) as ImageView
                val btnMessage = popUpView.findViewById<View>(R.id.img_message) as ImageView


                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView
                val txtMarkerDesc = popUpView.findViewById<View>(R.id.txt_marker_desc) as TextView
                val txtMarkerSubType = popUpView.findViewById<View>(R.id.txt_marker_ctype) as TextView


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.police)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title)
                                    mMarkerImage.setImageResource(R.drawable.accident)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.rape)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.robbery)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.murder)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.terrorist)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.fire)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.flood)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.drug)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-pdf")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    txtMarkerTitle.setText(title.toString())
                                    mMarkerImage.setImageResource(R.drawable.general)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())
                                    txtMarkerDesc.setText(desc.toString())
                                    txtMarkerSubType.setText(subType.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }


                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))


                btnConfirm.setOnClickListener {
                    mMarkerDialogPdfPopUp.dismiss()

                }


                btnCall.setOnClickListener {
                    // Show Weather Dialog
                    showWeatherDialogPdf(evid, title, mContext, dbRead)
                    mMarkerDialogPdfPopUp.dismiss()


                }

                btnView.setOnClickListener { view: View? ->
                    // Download video to local file
                    downloadPdfToLocalFile(evid, mContext)
                    mMarkerDialogPdfPopUp.dismiss()
                }


                btnMessage.setOnClickListener { view: View? ->
                    if (::incidentID.isInitialized){
                        mContext.startActivity(Intent(mContext, IncidentChatActivity::class.java).putExtra("Tag", incidentID))
                    } else {
                        Toast.makeText(mContext, "Sorry! You cannot perform this operation at this time", Toast.LENGTH_LONG).show()
                    }
                    mMarkerDialogPdfPopUp.dismiss()
                }

                btnShare.setOnClickListener {
                    UtilMethods.shareTheApp(mContext, title+" \n"+" EvidenceURL: "+evid+"\n"+" To learn more about the incident, " +
                            "Download eyewitness app from play store. Click here:"+" https://play.google.com/store/apps/details?id="+mContext.packageName+"/")
                    mMarkerDialogPdfPopUp.dismiss()
                }

            }


            fun showWeatherDialogVideo(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_weather, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mWeatherDialogVideoPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mWeatherDialogVideoPopUp.setBackgroundDrawable(colorDrawable)
                mWeatherDialogVideoPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mWeatherDialogVideoPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWeatherDialogVideoPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mWeatherDialogVideoPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerCondition = popUpView.findViewById<View>(R.id.txt_marker_condition) as TextView
                val txtMarkerTemp = popUpView.findViewById<View>(R.id.txt_marker_temp_value) as TextView
                val txtMarkerWindspeed = popUpView.findViewById<View>(R.id.txt_marker_windspeed_value) as TextView
                val txtMarkerWindangle = popUpView.findViewById<View>(R.id.txt_marker_windangle_value) as TextView
                val txtMarkerCloud = popUpView.findViewById<View>(R.id.txt_marker_clouds_value) as TextView
                val txtMarkerPressure = popUpView.findViewById<View>(R.id.txt_marker_pressure_value) as TextView
                val txtMarkerHumidity = popUpView.findViewById<View>(R.id.txt_marker_humidity_value) as TextView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView

                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-video")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }

                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

                btnConfirm.setOnClickListener {
                    mWeatherDialogVideoPopUp.dismiss()
                }

            }


            fun showWeatherDialogAudio(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_weather, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mWeatherDialogAudioPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mWeatherDialogAudioPopUp.setBackgroundDrawable(colorDrawable)
                mWeatherDialogAudioPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mWeatherDialogAudioPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWeatherDialogAudioPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mWeatherDialogAudioPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerCondition = popUpView.findViewById<View>(R.id.txt_marker_condition) as TextView
                val txtMarkerTemp = popUpView.findViewById<View>(R.id.txt_marker_temp_value) as TextView
                val txtMarkerWindspeed = popUpView.findViewById<View>(R.id.txt_marker_windspeed_value) as TextView
                val txtMarkerWindangle = popUpView.findViewById<View>(R.id.txt_marker_windangle_value) as TextView
                val txtMarkerCloud = popUpView.findViewById<View>(R.id.txt_marker_clouds_value) as TextView
                val txtMarkerPressure = popUpView.findViewById<View>(R.id.txt_marker_pressure_value) as TextView
                val txtMarkerHumidity = popUpView.findViewById<View>(R.id.txt_marker_humidity_value) as TextView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView

                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-audio")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }

                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

                btnConfirm.setOnClickListener {
                    mWeatherDialogAudioPopUp.dismiss()
                }


            }


            fun showWeatherDialogImage(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_weather, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mWeatherDialogImagePopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mWeatherDialogImagePopUp.setBackgroundDrawable(colorDrawable)
                mWeatherDialogImagePopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mWeatherDialogImagePopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWeatherDialogImagePopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mWeatherDialogImagePopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

                val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
                val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
                val txtMarkerCondition = popUpView.findViewById<View>(R.id.txt_marker_condition) as TextView
                val txtMarkerTemp = popUpView.findViewById<View>(R.id.txt_marker_temp_value) as TextView
                val txtMarkerWindspeed = popUpView.findViewById<View>(R.id.txt_marker_windspeed_value) as TextView
                val txtMarkerWindangle = popUpView.findViewById<View>(R.id.txt_marker_windangle_value) as TextView
                val txtMarkerCloud = popUpView.findViewById<View>(R.id.txt_marker_clouds_value) as TextView
                val txtMarkerPressure = popUpView.findViewById<View>(R.id.txt_marker_pressure_value) as TextView
                val txtMarkerHumidity = popUpView.findViewById<View>(R.id.txt_marker_humidity_value) as TextView
                val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
                val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView

                val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton


                if (title.equals("Officer Incident Report", true)) {
                    dbRead.collection("officer")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Accident Incident Report", true)) {
                    dbRead.collection("accident")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Sexual-Assault Incident Report", true)) {
                    dbRead.collection("sexual-assault")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Theft Incident Report", true)) {
                    dbRead.collection("theft")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Murder Incident Report", true)) {
                    dbRead.collection("murder")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Terrorism Incident Report", true)) {
                    dbRead.collection("terrorism")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Flooding Incident Report", true)) {
                    dbRead.collection("flooding")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Firehazard Incident Report", true)) {
                    dbRead.collection("firehazard")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                    dbRead.collection("illegal-drugs")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("Assault Incident Report", true)) {
                    dbRead.collection("assault")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                } else if (title.equals("General Incident Report", true)) {
                    dbRead.collection("general")
                            .whereEqualTo("evidenceURL", evid + "-image")
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    //Log.d(TAG, "${document.id} => ${document.data}")

                                    val id = document.getString("id")
                                    val documentID = document.getString("documentID")
                                    val subType = document.getString("subType")
                                    val xtitle = document.getString("title")
                                    val desc = document.getString("description")
                                    val address = document.getString("address")
                                    val date = document.getString("date")
                                    val evid = document.getString("evidenceURL")
                                    val latitude = document.getDouble("latitude")
                                    val longitude = document.getDouble("longitude")

                                    val wtemp = document.getString("weatherTemp")
                                    val wcondition = document.getString("weatherDesc")
                                    val wclouds = document.getString("weatherClouds")
                                    val wwindspeed = document.getString("weatherWindSpeed")
                                    val wwindangle = document.getString("weatherWindAngle")
                                    val whumidity = document.getString("weatherHumidity")
                                    val wpressure = document.getString("weatherPressure")

                                    if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                        if (wcondition.contains("clear sky", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                        } else if (wcondition.contains("clouds", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                        } else if (wcondition.contains("rain", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("drizzle", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.rain)
                                        } else if (wcondition.contains("thunderstorm", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                        } else if (wcondition.contains("snow", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.snow)
                                        } else if (wcondition.contains("atmosphere", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else if (wcondition.contains("mist", true)){
                                            mMarkerImage.setBackgroundResource(R.drawable.mist)
                                        } else {
                                            mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                        }

                                        txtMarkerTemp.setText(wtemp)
                                        txtMarkerCondition.setText(wcondition)
                                        txtMarkerCloud.setText(wclouds)
                                        txtMarkerHumidity.setText(whumidity)
                                        txtMarkerWindspeed.setText(wwindspeed)
                                        txtMarkerWindangle.setText(wwindangle)
                                        txtMarkerPressure.setText(wpressure)
                                    } else {

                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                        txtMarkerTemp.setText(":::")
                                        txtMarkerCondition.setText(":::")
                                        txtMarkerCloud.setText(":::")
                                        txtMarkerHumidity.setText(":::")
                                        txtMarkerWindspeed.setText(":::")
                                        txtMarkerWindangle.setText(":::")
                                        txtMarkerPressure.setText(":::")
                                    }

                                    txtMarkerTitle.setText(title)
                                    txtMarkerAddress.setText(address.toString())
                                    txtMarkerDate.setText(date.toString())

                                    incidentID = documentID.toString()
                                    //val location = LatLng(latitude!!, longitude!!)

                                }
                            }
                            .addOnFailureListener { exception ->
                                //Log.d(TAG, "Error getting documents: ", exception)
                            }
                }

                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

                btnConfirm.setOnClickListener {
                    mWeatherDialogImagePopUp.dismiss()
                }


            }


             fun showWeatherDialogPdf(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_weather, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mWeatherDialogPdfPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mWeatherDialogPdfPopUp.setBackgroundDrawable(colorDrawable)
                mWeatherDialogPdfPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                        mWeatherDialogPdfPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        mWeatherDialogPdfPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                        mWeatherDialogPdfPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

            val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
            val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
            val txtMarkerCondition = popUpView.findViewById<View>(R.id.txt_marker_condition) as TextView
            val txtMarkerTemp = popUpView.findViewById<View>(R.id.txt_marker_temp_value) as TextView
            val txtMarkerWindspeed = popUpView.findViewById<View>(R.id.txt_marker_windspeed_value) as TextView
            val txtMarkerWindangle = popUpView.findViewById<View>(R.id.txt_marker_windangle_value) as TextView
            val txtMarkerCloud = popUpView.findViewById<View>(R.id.txt_marker_clouds_value) as TextView
            val txtMarkerPressure = popUpView.findViewById<View>(R.id.txt_marker_pressure_value) as TextView
            val txtMarkerHumidity = popUpView.findViewById<View>(R.id.txt_marker_humidity_value) as TextView
            val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
            val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView

            val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton


            if (title.equals("Officer Incident Report", true)) {
                dbRead.collection("officer")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Accident Incident Report", true)) {
                dbRead.collection("accident")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Sexual-Assault Incident Report", true)) {
                dbRead.collection("sexual-assault")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Theft Incident Report", true)) {
                dbRead.collection("theft")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Murder Incident Report", true)) {
                dbRead.collection("murder")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Terrorism Incident Report", true)) {
                dbRead.collection("terrorism")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Flooding Incident Report", true)) {
                dbRead.collection("flooding")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Firehazard Incident Report", true)) {
                dbRead.collection("firehazard")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                dbRead.collection("illegal-drugs")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Assault Incident Report", true)) {
                dbRead.collection("assault")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("General Incident Report", true)) {
                dbRead.collection("general")
                        .whereEqualTo("evidenceURL", evid + "-pdf")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            }

                btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

                btnConfirm.setOnClickListener {
                        mWeatherDialogPdfPopUp.dismiss()
                }


        }


            fun showWeatherDialogDocx(evid: String, title: String, mContext: Context, dbRead: FirebaseFirestore) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val popUpView = inflater.inflate(R.layout.layout_weather, null)

                val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
                colorDrawable.alpha = 70

                mWeatherDialogDocxPopUp = PopupWindow(popUpView, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT)
                mWeatherDialogDocxPopUp.setBackgroundDrawable(colorDrawable)
                mWeatherDialogDocxPopUp.isOutsideTouchable = true

                val user = auth.currentUser

                if (Build.VERSION.SDK_INT >= 21) {
                    mWeatherDialogDocxPopUp.setElevation(5.0f)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWeatherDialogDocxPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
                } else {
                    mWeatherDialogDocxPopUp.showAsDropDown(popUpView, Gravity.CENTER, 0)
                }

            val txtMarkerTitle = popUpView.findViewById<View>(R.id.txt_marker_title) as TextView
            val mMarkerImage = popUpView.findViewById<View>(R.id.img_mark) as ImageView
            val txtMarkerCondition = popUpView.findViewById<View>(R.id.txt_marker_condition) as TextView
            val txtMarkerTemp = popUpView.findViewById<View>(R.id.txt_marker_temp_value) as TextView
            val txtMarkerWindspeed = popUpView.findViewById<View>(R.id.txt_marker_windspeed_value) as TextView
            val txtMarkerWindangle = popUpView.findViewById<View>(R.id.txt_marker_windangle_value) as TextView
            val txtMarkerCloud = popUpView.findViewById<View>(R.id.txt_marker_clouds_value) as TextView
            val txtMarkerPressure = popUpView.findViewById<View>(R.id.txt_marker_pressure_value) as TextView
            val txtMarkerHumidity = popUpView.findViewById<View>(R.id.txt_marker_humidity_value) as TextView
            val txtMarkerAddress = popUpView.findViewById<View>(R.id.txt_marker_address) as TextView
            val txtMarkerDate = popUpView.findViewById<View>(R.id.txt_marker_date) as TextView

            val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as AppCompatButton


            if (title.equals("Officer Incident Report", true)) {
                dbRead.collection("officer")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Accident Incident Report", true)) {
                dbRead.collection("accident")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Sexual-Assault Incident Report", true)) {
                dbRead.collection("sexual-assault")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Theft Incident Report", true)) {
                dbRead.collection("theft")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Murder Incident Report", true)) {
                dbRead.collection("murder")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Terrorism Incident Report", true)) {
                dbRead.collection("terrorism")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Flooding Incident Report", true)) {
                dbRead.collection("flooding")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Firehazard Incident Report", true)) {
                dbRead.collection("firehazard")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Illegal-Drugs Incident Report", true)) {
                dbRead.collection("illegal-drugs")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("Assault Incident Report", true)) {
                dbRead.collection("assault")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            } else if (title.equals("General Incident Report", true)) {
                dbRead.collection("general")
                        .whereEqualTo("evidenceURL", evid + "-docx")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                //Log.d(TAG, "${document.id} => ${document.data}")

                                val id = document.getString("id")
                                val documentID = document.getString("documentID")
                                val subType = document.getString("subType")
                                val xtitle = document.getString("title")
                                val desc = document.getString("description")
                                val address = document.getString("address")
                                val date = document.getString("date")
                                val evid = document.getString("evidenceURL")
                                val latitude = document.getDouble("latitude")
                                val longitude = document.getDouble("longitude")

                                val wtemp = document.getString("weatherTemp")
                                val wcondition = document.getString("weatherDesc")
                                val wclouds = document.getString("weatherClouds")
                                val wwindspeed = document.getString("weatherWindSpeed")
                                val wwindangle = document.getString("weatherWindAngle")
                                val whumidity = document.getString("weatherHumidity")
                                val wpressure = document.getString("weatherPressure")

                                if (wtemp != null && wcondition != null && wclouds != null && wwindspeed != null && wwindangle != null && whumidity != null){
                                    if (wcondition.contains("clear sky", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.clearsky)
                                    } else if (wcondition.contains("clouds", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.fewclouds)
                                    } else if (wcondition.contains("rain", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("drizzle", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.rain)
                                    } else if (wcondition.contains("thunderstorm", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.thunder)
                                    } else if (wcondition.contains("snow", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.snow)
                                    } else if (wcondition.contains("atmosphere", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else if (wcondition.contains("mist", true)){
                                        mMarkerImage.setBackgroundResource(R.drawable.mist)
                                    } else {
                                        mMarkerImage.setBackgroundResource(R.drawable.brokencloud)
                                    }

                                    txtMarkerTemp.setText(wtemp)
                                    txtMarkerCondition.setText(wcondition)
                                    txtMarkerCloud.setText(wclouds)
                                    txtMarkerHumidity.setText(whumidity)
                                    txtMarkerWindspeed.setText(wwindspeed)
                                    txtMarkerWindangle.setText(wwindangle)
                                    txtMarkerPressure.setText(wpressure)
                                } else {

                                    mMarkerImage.setBackgroundResource(R.drawable.brokencloud)

                                    txtMarkerTemp.setText(":::")
                                    txtMarkerCondition.setText(":::")
                                    txtMarkerCloud.setText(":::")
                                    txtMarkerHumidity.setText(":::")
                                    txtMarkerWindspeed.setText(":::")
                                    txtMarkerWindangle.setText(":::")
                                    txtMarkerPressure.setText(":::")
                                }

                                txtMarkerTitle.setText(title)
                                txtMarkerAddress.setText(address.toString())
                                txtMarkerDate.setText(date.toString())

                                incidentID = documentID.toString()
                                //val location = LatLng(latitude!!, longitude!!)

                            }
                        }
                        .addOnFailureListener { exception ->
                            //Log.d(TAG, "Error getting documents: ", exception)
                        }
            }


            btnConfirm.background = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

                btnConfirm.setOnClickListener {
                        mWeatherDialogDocxPopUp.dismiss()
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