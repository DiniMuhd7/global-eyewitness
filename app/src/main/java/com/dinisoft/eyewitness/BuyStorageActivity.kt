package com.dinisoft.eyewitness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class BuyStorageActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buy_storage_layout)

        val mBundleStorage =  intent.getStringArrayExtra("Tag")
        val mBundleName = mBundleStorage!![0]
        val mBundleDesc = mBundleStorage!![1]
        val mBundleSize = mBundleStorage!![2]
        val mBundlePrice = mBundleStorage!![3]

        val mTxtName: TextView = findViewById(R.id.iv_bundle_name)
        mTxtName.text = mBundleName+" Storage"
        val mTxtDesc: TextView = findViewById(R.id.iv_bundle_description)
        mTxtDesc.text = mBundleDesc
        val mTxtSize: TextView = findViewById(R.id.iv_bundle_size)
        mTxtSize.text = mBundleSize+" Bundle"



        val mCheckout: Button = findViewById(R.id.btn_checkout)
        mCheckout.text = getString(R.string.bundle_price)+mBundlePrice
        mCheckout.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putExtra("Tag", mBundleStorage)
            }
            startActivity(intent)
        }
    }

    override fun onStop() {
        super.onStop()
    }


}