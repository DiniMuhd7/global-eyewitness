package com.dinisoft.eyewitness

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.paystack.android.Paystack
import co.paystack.android.PaystackSdk
import co.paystack.android.Transaction
import co.paystack.android.model.Card
import co.paystack.android.model.Charge
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class CheckoutActivity: AppCompatActivity() {

    private lateinit var mCardNumber: TextInputLayout
    private lateinit var mCardExpiry: TextInputLayout
    private lateinit var mCardCVV: TextInputLayout
    lateinit var auth: FirebaseAuth
    lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.checkout_layout)
        // Initialize Firebase Auth
        auth = Firebase.auth
        db = Firebase.firestore

        // Initialize the paystack payment gateway
        initializePaystack()
        initializeFormVariables()


    }


    private fun initializePaystack() {
        PaystackSdk.initialize(this)
        PaystackSdk.setPublicKey(getString(R.string.paystack_pubkey))
    }

    private fun initializeFormVariables() {
        mCardNumber = findViewById(R.id.til_card_number)
        mCardExpiry = findViewById(R.id.til_card_expiry)
        mCardCVV = findViewById(R.id.til_card_cvv)

        // this is used to add a forward slash (/) between the cards expiry month
        // and year (11/21). After the month is entered, a forward slash is added
        // before the year
        mCardExpiry.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?,
                                           start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().length == 2 && !s.toString().contains("/")) {
                    s!!.append("/")
                }
            }
        })

        val mBundleStorage =  intent.getStringArrayExtra("Tag")
        val mBundlePrice = mBundleStorage!![3]
        val button = findViewById<Button>(R.id.btn_make_payment)
        button.text = getString(R.string.bundle_price)+mBundlePrice
        button.setOnClickListener { v: View? -> performCharge() }

    }

    private fun performCharge() {
        val userEmail = auth.currentUser?.email.toString()
        val cardNumber = mCardNumber.editText!!.text.toString()
        val cardExpiry = mCardExpiry.editText!!.text.toString()
        val ccv = mCardCVV.editText!!.text.toString()

        val mBundleStorage =  intent.getStringArrayExtra("Tag")
        val mBundlePrice = mBundleStorage!![3]
        var amount = mBundlePrice.toInt()
        amount *= 100

        if (!cardNumber.isEmpty() && !cardExpiry.isEmpty() && !ccv.isEmpty() && userEmail != null) {
            val cardExpiryArray = cardExpiry.split("/").toTypedArray()
            val expiryMonth = cardExpiryArray[0].toInt()
            val expiryYear = cardExpiryArray[1].toInt()

            val card = Card(cardNumber, expiryMonth, expiryYear, ccv)
            val charge = Charge()
            charge.amount = amount
            charge.email = userEmail
            charge.card = card

            PaystackSdk.chargeCard(this, charge, object : Paystack.TransactionCallback {
                override fun onSuccess(transaction: Transaction) {
                    parseResponse(transaction.reference)
                }
                override fun beforeValidate(transaction: Transaction) {
                    Log.d("Main Activity", "beforeValidate: " + transaction.reference)
                    //Toast.makeText(this@CheckoutActivity, "Processing transaction...", Toast.LENGTH_SHORT).show()
                }
                override fun onError(error: Throwable, transaction: Transaction) {
                    Log.d("Main Activity", "onError: " + error.localizedMessage)
                    Log.d("Main Activity", "onError: $error")
                    Toast.makeText(this@CheckoutActivity, error.localizedMessage, Toast.LENGTH_LONG).show()
                }
            })
        } else {
            val parentLayout: View = findViewById(android.R.id.content)
            Snackbar.make(parentLayout, "Card details field should not be left blank", Snackbar.LENGTH_LONG)
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                    .show()
        }

    }

    private fun parseResponse(transactionReference: String) {
        val message = "Payment Successful: - $transactionReference"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Use intent to go back to the dashboard using user status
        val mBundleStorage = intent.getStringArrayExtra("Tag")
        val mStorageSize = mBundleStorage!![2]
        val xStorageSize = mStorageSize.toString().substring(0, mStorageSize.lastIndexOf("GB")) // Remove the 'GB' prefix
        val xCleanStorageSize = xStorageSize.toInt()

        val mUserStatus = mBundleStorage!![4]
        if (mUserStatus.equals("user", true)) {
            val userID = auth.currentUser?.uid.toString()
            db.collection("users")
                    .whereEqualTo("uid", userID)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val documentID = document.getString("documentID")
                            val storageCap = document.getString("storageCap")
                            val mGBSize: Long = storageCap!!.toLong() / 1024
                            val totalGBSize: Long = mGBSize + xCleanStorageSize.toLong()
                            val totalStorageCap: Long = totalGBSize * 1024
                            val modify = db.collection("users").document(document.id)
                            modify.update("storageCap", totalStorageCap.toString())
                                    .addOnSuccessListener { }
                                    .addOnFailureListener { e ->  }
                        }
                    }
                    .addOnFailureListener { e ->


                    }
            startActivity(Intent(this, DashUserActivity::class.java))
        } else if (mUserStatus.equals("agency")) {
            val userID = auth.currentUser?.uid.toString()
            db.collection("agency")
                    .whereEqualTo("uid", userID)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val documentID = document.getString("documentID")
                            val storageCap = document.getString("storageCap")
                            val mGBSize: Long = storageCap!!.toLong() / 1024
                            val totalGBSize: Long = mGBSize + xCleanStorageSize.toLong()
                            val totalStorageCap: Long = totalGBSize * 1024
                            val modify = db.collection("agency").document(document.id)
                            modify.update("storageCap", totalStorageCap.toString())
                                    .addOnSuccessListener { }
                                    .addOnFailureListener { e ->  }
                        }
                    }
                    .addOnFailureListener { e ->


                    }
            startActivity(Intent(this, DashAgencyActivity::class.java))
        }

    }


}