package com.example.smartscan_beta_v01

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShoppingListViewModel : ViewModel(){

    private  val  repository : ShoppingListRepository = ShoppingListRepository().getInstance()
    private val _allShoppingList = MutableLiveData<List<ShoppingListModel>>()
    val allShoppingList : LiveData<List<ShoppingListModel>> = _allShoppingList

    init {
        repository.loadShoppingList(_allShoppingList)
    }

}