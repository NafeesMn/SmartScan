package com.example.smartscan_beta_v01

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartRepository {
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val currentUserId = currentUser?.uid.toString()
    private val databaseReference : DatabaseReference = FirebaseDatabase.getInstance().getReference("user").child(currentUserId).child("cart")

    @Volatile private var INSTANCE : CartRepository ?= null

    fun getInstance() : CartRepository{
        return INSTANCE ?: synchronized(this){
            val instance = CartRepository()
            INSTANCE = instance
            instance
        }
    }

    fun loadCart(cartList : MutableLiveData<List<CartProductModel>>){
        databaseReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val _cartList : List<CartProductModel> = snapshot.children.map { dataSnapshot ->
                        dataSnapshot.getValue(CartProductModel::class.java)!!
                    }
                    cartList.postValue(_cartList)
                }catch (e : Exception){

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}