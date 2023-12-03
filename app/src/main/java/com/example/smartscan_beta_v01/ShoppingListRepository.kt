package com.example.smartscan_beta_v01

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ShoppingListRepository {
    val currId = FirebaseAuth.getInstance().currentUser
    val userId = currId?.uid.toString()
    val databaseReference = FirebaseDatabase.getInstance().getReference("user").child(userId).child("shoppingList")

    @Volatile private var INSTANCE : ShoppingListRepository ?= null

    fun getInstance() : ShoppingListRepository{
        return INSTANCE ?: synchronized(this){
            val instance = ShoppingListRepository()
            INSTANCE = instance
            instance
        }
    }

    fun loadShoppingList(shoppingList : MutableLiveData<List<ShoppingListModel>>){
        databaseReference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val _shoppingList : List<ShoppingListModel> = snapshot.children.map { dataSnapshot ->
                        dataSnapshot.getValue(ShoppingListModel::class.java)!!
                    }
                    shoppingList.postValue(_shoppingList)
                }catch (e : Exception){

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}