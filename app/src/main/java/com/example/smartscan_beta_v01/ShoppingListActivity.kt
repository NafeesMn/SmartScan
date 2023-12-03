package com.example.smartscan_beta_v01

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class ShoppingListActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    lateinit var suggestionAdapter: CursorAdapter
    private lateinit var productList: MutableList<ShoppingListModel>
    private lateinit var filteredProductList: MutableList<ShoppingListModel>
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var viewModel: ShoppingListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        // Recycler view
        recyclerView = findViewById(R.id.rv_shoplistA)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        adapter = ShoppingListAdapter(this)
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[ShoppingListViewModel::class.java]
        viewModel.allShoppingList.observe(this, Observer {
            adapter.updateShoppingList(it)
        })

        val search_view : SearchView = findViewById(R.id.searchview)
        search_view.setOnQueryTextListener(this)

//            databaseRef = FirebaseDatabase.getInstance().getReference("shoppingList")

            productList = mutableListOf()
            filteredProductList = mutableListOf()

            // bila type dia keluar suggestion based on what we have write
            suggestionAdapter = SuggestionsAdapter(
                this,
                R.layout.dropdown_shopping_list,
                null,
                arrayOf("productName"),
                intArrayOf(android.R.id.text1),
                0
            )
            search_view.suggestionsAdapter = suggestionAdapter

            val databaseReference = FirebaseDatabase.getInstance().getReference("products")
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    productList.clear()

                    for (snapshot in dataSnapshot.children) {
                        val product = snapshot.getValue(ShoppingListModel::class.java)
                        productList.add(product!!)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                }
            })

            suggestionAdapter.setFilterQueryProvider { constraint ->
                // Filter the suggestion cursor based on the query constraint
                val filteredCursor = MatrixCursor(arrayOf(BaseColumns._ID, "productName", "price", "imageUrl", "brand", "quantity"))
                for ((index, product) in filteredProductList.withIndex()) {
                    if (product.productName!!.contains(constraint.toString(), ignoreCase = true)) {
                        filteredCursor.addRow(arrayOf(index, product.productName, product.price, product.imageUrl, product.brand, product.quantity))
                    }
                }
                filteredCursor
            }

            (suggestionAdapter as SuggestionsAdapter).setCursorToStringConverter { cursor ->
                // Convert the cursor item to a string representation
                val productNameIndex = cursor.getColumnIndex("productName")
                cursor.getString(productNameIndex)
            }

            search_view.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                override fun onSuggestionSelect(position: Int): Boolean {
                    return false
                }

                override fun onSuggestionClick(position: Int): Boolean {
                    // Handle suggestion click

                    val alertBox = AlertDialog.Builder(this@ShoppingListActivity)
                        .setTitle("Add Item")
                        .setMessage("You want to add this item into your Shopping List?")
                        .setPositiveButton("Yes") {dialog, _ ->
                            val selectedProduct = filteredProductList[position]

                            // Add the selected product to the Firebase database
                            val currId = FirebaseAuth.getInstance().currentUser
                            val userId = currId?.uid.toString()
                            val databaseReference = FirebaseDatabase.getInstance().getReference("user").child(userId)
                            selectedProduct.productId = filteredProductList[position].productId
                            databaseReference.child("shoppingList").child(selectedProduct.productId.toString()).setValue(filteredProductList[position])
                            dialog.dismiss()
                        }
                        .setNegativeButton("No") {dialog, _ ->
                            dialog.dismiss()
                        }
                    alertBox.show()
                    return true
                }
            })
    }

    private class SuggestionsAdapter(context: Context, layout: Int, c: Cursor?, from: Array<String>, to: IntArray, flags: Int) :
        SimpleCursorAdapter(context, layout, c, from, to, flags) {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
            return inflater.inflate(R.layout.dropdown_shopping_list, parent,false)
        }

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val itemName: TextView = view.findViewById(R.id.dd_tv_prodName)
            val itemImg : ImageView = view.findViewById(R.id.dd_iv_itemImage)
            val itemBrand_qty : TextView = view.findViewById(R.id.dd_tv_prodQty_brand)

            // Get the column index of the productName column
            val productNameIndex = cursor.getColumnIndex("productName")
            val productImage = cursor.getColumnIndex("imageUrl")
            val productBrand = cursor.getColumnIndex("brand")
            val productQuantity = cursor.getColumnIndex("quantity")

            // Set the text of the TextView using the cursor
            itemName.text = cursor.getString(productNameIndex)
            itemBrand_qty.text = "${cursor.getString(productBrand)} | ${cursor.getString(productQuantity)}"
            Picasso.get().load(cursor.getString(productImage)).into(itemImg)
        }

        override fun convertToString(cursor: Cursor): CharSequence {
            val productNameIndex = cursor.getColumnIndex("productName")
            return cursor.getString(productNameIndex)
        }
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        filteredProductList.clear()
        for (product in productList) {
            if (product.productName!!.contains(newText.toString(), ignoreCase = true)) {
                filteredProductList.add(product)
            }
        }
        // Convert the filteredUserList to a MatrixCursor
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "productName", "price", "imageUrl", "brand", "quantity"))
        for ((index, product) in filteredProductList.withIndex()) {
            cursor.addRow(arrayOf(index, product.productName, product.price, product.imageUrl, product.brand, product.quantity))
        }
        suggestionAdapter.changeCursor(cursor)

        return true
    }
}