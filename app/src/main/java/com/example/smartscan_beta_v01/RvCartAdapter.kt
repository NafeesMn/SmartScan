package com.example.smartscan_beta_v01

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.math.RoundingMode
import java.text.DecimalFormat

class RvCartAdapter (private val context: Context?): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val cartList = ArrayList<CartProductModel>()
    companion object{
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_TOTAL_PRICE = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_item, parent, false)
                MyViewHolder(view)
            }
            VIEW_TYPE_TOTAL_PRICE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_total_price, parent, false)
                TotalPriceViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM)
        {
            holder as MyViewHolder
            val currentItem = cartList[position]
            val prodImageUrl = currentItem.imageUrl
            Picasso.get().load(prodImageUrl).into(holder.itemImage)
            holder.itemName.text = currentItem.productName
            holder.brand.text = "${currentItem.brand} | ${currentItem.productQty}"
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.DOWN
            holder.itemPrice.text = df.format(currentItem.totalPrice).toString()
            holder.itemQuantity.text = currentItem.quantity.toString()
            var count : Int = Integer.parseInt(holder.itemQuantity.text.toString())

            holder.qtyAdd.setOnClickListener {
                count++
                val updatedTotalPrice = count * currentItem.price!!
                holder.itemQuantity.text = count.toString()
                holder.itemPrice.text = updatedTotalPrice.toString()
                updateTotalPriceInFirebase(currentItem.productId.toString(),
                    updatedTotalPrice, Integer.parseInt(holder.itemQuantity.text.toString()))

                if (count > 1)
                {
                    holder.qtyAdd.alpha = 1F
                    holder.qtyAdd.isEnabled = true
                }
            }

            // To remove item quantity
            holder.qtyMinus.setOnClickListener {
                if (count > 1)
                {
                    count--
                    val updatedTotalPrice = count * currentItem.price!!
                    holder.itemQuantity.text = count.toString()
                    holder.itemPrice.text = updatedTotalPrice.toString()
                    updateTotalPriceInFirebase(currentItem.productId.toString(), updatedTotalPrice, Integer.parseInt(holder.itemQuantity.text.toString()))
                }else{
                    showRemoveItemDialog(currentItem.productId)
                }
            }
        }else{

            holder as TotalPriceViewHolder
            calculateTotalPrice(holder)
            holder.checkout.setOnClickListener {
                val intent = Intent(context, Checkout::class.java)
                context?.startActivity(intent)
            }

        }
    }

    private fun showRemoveItemDialog(productId: Long?) {
        val alertBox = AlertDialog.Builder(context)
            .setTitle("Remove Item")
            .setMessage("Are you sure you want to remove this item from the cart?")
            .setPositiveButton("Yes") {dialog, _ ->
                removeItemFromCart(productId)
                dialog.dismiss()
            }
            .setNegativeButton("No") {dialog, _ ->
                dialog.dismiss()
            }
        alertBox.show()
    }

    private fun removeItemFromCart(productId: Long?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val databaseReference = FirebaseDatabase.getInstance().reference
        val cartReference = databaseReference.child("user").child(userId).child("cart")
        val itemReference = cartReference.child(productId.toString())

        itemReference.removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Item removed from cart in Firebase")
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Failed to remove item from cart in Firebase: ${exception.message}")
            }
    }

    private fun updateTotalPriceInFirebase(productId: String, totalPrice: Double, itemQty: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val databaseReference = FirebaseDatabase.getInstance().reference
        val totalPriceReference = databaseReference.child("user").child(userId)
            .child("cart").child(productId).child("totalPrice")

        val updatedQty = databaseReference.child("user").child(userId)
            .child("cart").child(productId).child("quantity")

        updatedQty.setValue(itemQty)
            .addOnSuccessListener {
                Log.d(TAG, "Quantity updated")
            }
            .addOnFailureListener {
            Log.d(TAG, "Failed to update quantity in Firebase")
            }

        totalPriceReference.setValue(totalPrice)
            .addOnSuccessListener {
                Log.d(TAG, "Total price updated in Firebase")
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Failed to update total price in Firebase: ${exception.message}")
            }
    }

    private fun calculateTotalPrice(holder: RecyclerView.ViewHolder) {
        holder as TotalPriceViewHolder
        var totalPrice = 0.0
        val databaseReference = FirebaseDatabase.getInstance().reference
        val currId = FirebaseAuth.getInstance().currentUser
        val userId = currId?.uid.toString()
        val cartReference = databaseReference.child("user").child(userId).child("cart")
        cartReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartProductModel::class.java)
                    totalPrice += item?.totalPrice!!
                }
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.DOWN
                holder.totalPrice.text = "RM ${df.format(totalPrice)}"
            }
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Cannot retrieve")
            }
        })
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < cartList.size) VIEW_TYPE_ITEM
        else VIEW_TYPE_TOTAL_PRICE
    }

    override fun getItemCount(): Int {
        return cartList.size + 1
    }

    fun updateCartList(cartList : List<CartProductModel>){
        this.cartList.clear()
        this.cartList.addAll(cartList)
        notifyDataSetChanged()
    }

    inner class MyViewHolder (itemView : View) : RecyclerView.ViewHolder(itemView){
        val itemImage: ImageView = itemView.findViewById(R.id.iv_itemImage)
        val itemName: TextView = itemView.findViewById(R.id.tv_prodName)
        val brand: TextView = itemView.findViewById(R.id.tv_prodQty_brand)
        val itemPrice: TextView = itemView.findViewById(R.id.tv_cart_itemPrice)
        val itemQuantity: TextView = itemView.findViewById(R.id.et_cart_item_quantity)
        val qtyAdd : ImageButton = itemView.findViewById(R.id.btn_cart_IncreaseQty)
        val qtyMinus : ImageButton = itemView.findViewById(R.id.btn_cart_decreaseQty)
    }

    inner class TotalPriceViewHolder (itemView : View) : RecyclerView.ViewHolder(itemView){
        val totalPrice: TextView = itemView.findViewById(R.id.tv_totalPrice)
        val checkout: Button = itemView.findViewById(R.id.btn_checkout)
    }
}