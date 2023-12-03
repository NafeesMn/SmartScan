@file:Suppress("DEPRECATION")
package com.example.smartscan_beta_v01

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // To remove the status bar
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window?.statusBarColor = Color.TRANSPARENT

        auth = Firebase.auth

        // To navigate to Login if the user clicked on the Login Now text
        val loginNowText: TextView = findViewById(R.id.LoginNow)

        loginNowText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val registerBtn: Button = findViewById(R.id.btnRegister)
        registerBtn.setOnClickListener{
            // Get input from user
            performSignUp()
        }
    }

    private fun performSignUp() {
        val email = findViewById<EditText>(R.id.etEmailReg)
        val password = findViewById<EditText>(R.id.etPasswordReg)
        val name = findViewById<EditText>(R.id.etName)

        // to check whether users have fill in all the requirements
        if(email.text.isNotEmpty()|| password.text.isNotEmpty()){

            val inputEmail = email.text.toString()
            val inputPassword = password.text.toString()
            val inputName = name.text.toString()
            auth.createUserWithEmailAndPassword(inputEmail, inputPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success navigate to next activity
                        Toast.makeText(baseContext, "Successfully registered.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)

                        val currentUser = auth.currentUser
                        val currentUserId = currentUser?.uid.toString()
                        var dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("user").child(currentUserId)
                        val user = User(inputName, inputEmail)
                        dbRef.setValue(user)
                            .addOnCompleteListener{
                                Toast.makeText(this, "Welcome ${user.name}!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {err ->
                                Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        // if the sign up fails
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }

                }
        }else{
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }
}