package com.example.smartscan_beta_v01

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

import android.os.Handler
import android.os.Looper
import android.util.Log

class ShoppingListAdapter(private val context: Context?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val shoppingList = ArrayList<ShoppingListModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutShoppingList =
            LayoutInflater.from(parent.context).inflate(R.layout.shopping_list_item, parent, false)
        return ViewHolder(layoutShoppingList)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentList = shoppingList[position]

        holder as ViewHolder
        holder.tvProductName.text = currentList.productName
        holder.tvBrand.text = "${currentList.brand} | ${currentList.quantity}"
        val prodImageUrl = currentList.imageUrl
        Picasso.get().load(prodImageUrl).into(holder.itemImage)

        // Set the checkbox state and appearance
        holder.checkbox.isChecked = currentList.isChecked
        if (currentList.isChecked) {
            holder.tvProductName.setTextColor(context?.resources?.getColor(R.color.grey) ?: 0)
            holder.tvBrand.setTextColor(context?.resources?.getColor(R.color.grey) ?: 0)
            holder.itemImage.alpha = 0.5f
        } else {
            holder.tvProductName.setTextColor(context?.resources?.getColor(android.R.color.black) ?: 0)
            holder.tvBrand.setTextColor(context?.resources?.getColor(android.R.color.black) ?: 0)
            holder.itemImage.alpha = 1.0f
        }

        // Set checkbox click listener
        holder.checkbox.setOnCheckedChangeListener(null) // Remove previous listener to avoid conflicts
        holder.checkbox.setOnClickListener {
            val isChecked = holder.checkbox.isChecked
            currentList.isChecked = isChecked
            if (isChecked) {
                Handler(Looper.getMainLooper()).post {
                    shoppingList.removeAt(position)
                    shoppingList.add(currentList)
                    notifyDataSetChanged()
                }
            } else {
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return shoppingList.size
    }

    fun updateShoppingList(shoppingList: List<ShoppingListModel>) {
        this.shoppingList.clear()
        this.shoppingList.addAll(shoppingList)
        notifyDataSetChanged()
    }

    fun checkoutItem(position: Int) {
        Log.e("Checked", "checkoutItem called")
        if (position in 0 until shoppingList.size) {
            Log.e("Checked", "if clause called")
            val checkedItem = shoppingList[position]
            checkedItem.isChecked = true
            notifyItemChanged(position)
        }
    }
}
class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvProductName: TextView = itemView.findViewById(R.id.tv_list_prodName)
    val tvBrand: TextView = itemView.findViewById(R.id.tv_list_prodQty_brand)
    val itemImage: ImageView = itemView.findViewById(R.id.iv_list_itemImage)
    val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
}
