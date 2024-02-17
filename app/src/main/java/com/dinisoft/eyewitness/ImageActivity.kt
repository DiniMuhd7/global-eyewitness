package com.dinisoft.eyewitness

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_image)

        /*
        val mImage = findViewById<View>(R.id.evid_img) as ImageView
        //Get string extra and initialize exo player
        val mItemSelected = intent.getStringExtra("Tag")
        if (mItemSelected.endsWith(".jpg")) {
            val localFile = File(mItemSelected)
            val localUriPath = Uri.fromFile(localFile)
            mImage.setImageURI(localUriPath)
        } else {
            val localFile = File(mItemSelected)
            val localUriPath = Uri.fromFile(localFile)
            mImage.setImageURI(localUriPath)
        }
*/

    }


    override fun onStart() {
        super.onStart()
        // Get string extra and initialize exo player
        val mItemSelected = intent.getStringExtra("Tag")
        if (mItemSelected!!.endsWith(".jpg")) {
            val mImage = findViewById<View>(R.id.evid_img) as ImageView
            val localFile = File(mItemSelected)
            val localUriPath = Uri.fromFile(localFile)
            mImage.setImageURI(localUriPath)
        } else {
            val mImage = findViewById<View>(R.id.evid_img) as ImageView
            val localFile = File(mItemSelected)
            val localUriPath = Uri.fromFile(localFile)
            mImage.setImageURI(localUriPath)
        }

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        //Get string extra and initialize exo player
        val mItemSelected = intent.getStringExtra("Tag")
        val localFile = File(mItemSelected)
        intent.removeExtra("Tag")
        MapActivity().deleteFileForce(localFile)
        if (mItemSelected!!.endsWith(".jpg")) {
            val localFile = File(mItemSelected)
            intent.removeExtra("Tag")
            MapActivity().deleteFileForce(localFile)
        } else {
            val localFile = File(mItemSelected)
            intent.removeExtra("Tag")
            MapActivity().deleteFileForce(localFile)
        }
    }
}

