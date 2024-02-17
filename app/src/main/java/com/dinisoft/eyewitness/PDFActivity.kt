package com.dinisoft.eyewitness

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import java.io.File

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class PDFActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_pdf)

        /*
        val mPDF = findViewById<View>(R.id.evid_pdf) as PDFView
        //Get string extra and initialize exo player
        val mItemSelected = intent.getStringExtra("Tag")
        if (mItemSelected.endsWith(".pdf")) {
            val localFile = File(mItemSelected)
            //val localUriPath = Uri.fromFile(localFile)
            mPDF.fromFile(localFile).load()
        } else {
            val localFile = File(mItemSelected)
            //val localUriPath = Uri.fromFile(localFile)
            mPDF.fromFile(localFile).load()
        }
*/

    }


    override fun onStart() {
        super.onStart()
        // Get string extra and initialize exo player
        val mPDF = findViewById<View>(R.id.evid_pdf) as PDFView
        //Get string extra and initialize exo player
        val mItemSelected = intent.getStringExtra("Tag")
        if (mItemSelected!!.endsWith(".pdf")) {
            val localFile = File(mItemSelected)
            //val localUriPath = Uri.fromFile(localFile)
            mPDF.fromFile(localFile).load()
        } else {
            val localFile = File(mItemSelected)
            //val localUriPath = Uri.fromFile(localFile)
            mPDF.fromFile(localFile).load()
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
        if (mItemSelected!!.endsWith(".pdf")) {
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

