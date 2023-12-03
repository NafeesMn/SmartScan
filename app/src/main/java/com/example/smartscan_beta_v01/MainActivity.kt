@file:Suppress("DEPRECATION")

package com.example.smartscan_beta_v01

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text

private lateinit var auth: FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window?.statusBarColor = Color.TRANSPARENT

        auth = FirebaseAuth.getInstance()
        val logout = findViewById<ImageButton>(R.id.btnLogout)
        val scanBarcode : Button = findViewById(R.id.btnScanBarcode)
        val welcomeuser : TextView = findViewById(R.id.tv_welcomeuser)
        val btn_shoplist : Button = findViewById(R.id.btnShoppingList)
        var username = ""

        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid.toString()
        var dbRef : DatabaseReference = FirebaseDatabase.getInstance().getReference("user").child(currentUserId)
        dbRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val item = snapshot.getValue(User::class.java)
                username = item?.name!!
                welcomeuser.text = "Welcome, \n$username"
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        scanBarcode.setOnClickListener {
            val intent = Intent(this, ScanBarcode::class.java)
            startActivity(intent)
        }
        logout.setOnClickListener {
            auth.signOut()
            signOutUser()
        }

        btn_shoplist.setOnClickListener {
            val intent = Intent(this, ShoppingListActivity::class.java)
            startActivity(intent)
        }

    }

    private fun signOutUser() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

}