package com.example.smartscan_beta_v01

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CartViewModel : ViewModel(){

    private  val  repository : CartRepository = CartRepository().getInstance()
    private val _allCart = MutableLiveData<List<CartProductModel>>()
    val allCart : LiveData<List<CartProductModel>> = _allCart

    init {
        repository.loadCart(_allCart)
    }

}