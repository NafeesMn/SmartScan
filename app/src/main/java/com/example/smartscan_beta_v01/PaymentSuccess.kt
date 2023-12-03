package com.example.smartscan_beta_v01

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class PaymentSuccess : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val productList: MutableList<CartProductModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_success)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid.toString()
        database = FirebaseDatabase.getInstance().getReference("user").child(currentUserId).child("cart")

        // Attach a ValueEventListener to retrieve the data
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                productList.clear()
                for (snapshot in dataSnapshot.children) {
                    val product = snapshot.getValue(CartProductModel::class.java)
                    product?.let {
                        productList.add(it)
                    }
                }
                // Update the ScrollView with the new data
                updateScrollView()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("YourActivity", "Error fetching data from Firebase: ${databaseError.message}")
            }
        })

        val contBtn: Button = findViewById(R.id.btn_continue)
        contBtn.setOnClickListener {

            val cartReference = database
            val databaseRecentlyPurchased: DatabaseReference = FirebaseDatabase.getInstance().getReference("user").child(currentUserId).child("recentlyPurchased")
            var purchase = databaseRecentlyPurchased.push().key.toString()
            for (product in productList) {
                val productName = product.productName.toString()
                databaseRecentlyPurchased.child("$purchase").child(productName).setValue(product)
                cartReference.child("${product.productId}").removeValue()
                    .addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Item removed from cart in Firebase")
                    }
                    .addOnFailureListener { exception ->
                        Log.d(ContentValues.TAG, "Failed to remove item from cart in Firebase: ${exception.message}")
                    }
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateScrollView() {
        val scrollView: ScrollView = findViewById(R.id.sv_payment)
        val linearLayout: LinearLayout = findViewById(R.id.ly_payment)

        // Clear the previous views
        linearLayout.removeAllViews()

        // Populate the ScrollView with product information
        for (product in productList) {
            val customView = layoutInflater.inflate(R.layout.receipt_layout, null)

            val productNameTextView: TextView = customView.findViewById(R.id.tv_prodName)
            val productQuantity: TextView = customView.findViewById(R.id.tv_quantity)
            val prodcutImage: ImageView = customView.findViewById(R.id.iv_itemImage)
            val productQtyBrand: TextView = customView.findViewById(R.id.tv_prodQty_brand)

            productNameTextView.text = product.productName
            productQuantity.text = "x${product.quantity}"
            val prodcutImageUrl = product.imageUrl
            Picasso.get().load(prodcutImageUrl).into(prodcutImage)
            productQtyBrand.text = "${product.productQty} | ${product.brand}"

            linearLayout.addView(customView)
        }
    }
}