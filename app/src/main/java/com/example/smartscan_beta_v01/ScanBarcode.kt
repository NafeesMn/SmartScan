package com.example.smartscan_beta_v01

import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

private const val CAMERA_REQUEST_CODE = 101

class ScanBarcode : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner
    private lateinit var shoppingListAdapter : ShoppingListAdapter
    private lateinit var shoppingListViewModel: ShoppingListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_barcode)

        shoppingListAdapter = ShoppingListAdapter(this)
        // Initialize the shoppingListViewModel
        shoppingListViewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)

        // Populate the shoppingList with data from the view model
        shoppingListViewModel.allShoppingList.observe(this) { shoppingList ->
            shoppingListAdapter.updateShoppingList(shoppingList)
        }

        val sheets = findViewById<FrameLayout>(R.id.fl_sheets)
        val tapUp = findViewById<ImageView>(R.id.iv_up)

        setUpTabs()

        BottomSheetBehavior.from(sheets).apply {
            peekHeight = 260
            this.state = BottomSheetBehavior.STATE_COLLAPSED

            sheets.setOnClickListener {
                this.state = BottomSheetBehavior.STATE_EXPANDED
            }

            tapUp.setOnClickListener{
                this.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        val scannerview = findViewById<CodeScannerView>(R.id.scanner_view)
        val txtView = findViewById<TextView>(R.id.tv_textView)

        setupPermissions()
        codeScanner(scannerview, txtView)
    }

    private fun setUpTabs() {
        val viewpager = findViewById<ViewPager>(R.id.viewpager)
        val tablayout = findViewById<TabLayout>(R.id.tabLayout)
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(ShoppingCart(), "My Cart")
        adapter.addFragment(ShoppingList(), "Shopping List")
        viewpager.adapter = adapter
        tablayout.setupWithViewPager(viewpager)
        tablayout.getTabAt(0)!!.setCustomView(R.layout.shoppingcart_notification_badge)
        tablayout.getTabAt(1)!!.setCustomView(R.layout.shoppinglist_badge)
    }

    private fun codeScanner(scanner_view: CodeScannerView, tv_textView: TextView){
        codeScanner = CodeScanner(this, scanner_view)

        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK //which camera to use
            formats = CodeScanner.ONE_DIMENSIONAL_FORMATS  //choose what format of barcode i want to use

            autoFocusMode = AutoFocusMode.SAFE  //auto focus at 6 intervals
            scanMode = ScanMode.CONTINUOUS // continuously try to scan and find barcode
            isAutoFocusEnabled = true
            isFlashEnabled = false


            decodeCallback = DecodeCallback {
                runOnUiThread{
//                    tv_textView.text = it.text
                    codeScanner.stopPreview()

                    // Dialog box pop up
                    // When the product id is found
                    // Display product details & quantity
                    previewProductAlertBox(it.text)
                }
            }

            errorCallback = ErrorCallback {
                runOnUiThread {
                    Log.e("Main", "Camera initialization error: ${it.message}")
                }
            }
        }

        scanner_view.setOnClickListener{
            codeScanner.startPreview()
        }

    }

    private fun previewProductAlertBox(prodId: String) {

        val dialogBinding = layoutInflater.inflate(R.layout.activity_preview_product_alert_box, null)
        val addProductDialog = Dialog(this@ScanBarcode)
        addProductDialog.setContentView(dialogBinding)
        addProductDialog.setCancelable(false) // To hide the alert box when the user press outside the box or the back button
        addProductDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // To retrieve the product info
        val dbRef = FirebaseDatabase.getInstance().getReference("products").child(prodId)
        dbRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot){
                var prodList = arrayListOf<ProductModel>()
                if(snapshot.exists())
                {
                    val prodData = snapshot.getValue(ProductModel::class.java)
                    prodList.add(prodData!!)
                    dialogBinding.findViewById<TextView>(R.id.tv_productName).text = prodList[0].productName
                    dialogBinding.findViewById<TextView>(R.id.tv_brand).text = prodList[0].brand
                    dialogBinding.findViewById<TextView>(R.id.tv_productQuantity).text = prodList[0].quantity
                    dialogBinding.findViewById<TextView>(R.id.tv_category).text = prodList[0].category
                    prodList[0].category.toString()

                    var count = 1;
                    val itemQty = dialogBinding.findViewById<EditText>(R.id.et_item_quantity_layout)
                    val addBtn = dialogBinding.findViewById<ImageButton>(R.id.btn_IncreaseQty)
                    val removeBtn = dialogBinding.findViewById<ImageButton>(R.id.btn_decreaseQty)

                    val prodImage = dialogBinding.findViewById<ImageView>(R.id.IV_productImg)
                    val prodImageUrl = prodList[0].imageUrl
                    Picasso.get().load(prodImageUrl).into(prodImage)

                    // To add item quantity
                    addBtn.setOnClickListener {
                        count++
                        itemQty.setText(count.toString())
                        if (count > 1)
                        {
                            removeBtn.alpha = 1F
                            removeBtn.isEnabled = true
                        }
                    }
                    // To remove item quantity
                    removeBtn.isEnabled = false
                    removeBtn.alpha = 0.2F
                    removeBtn.setOnClickListener {
                        count--
                        itemQty.setText(count.toString())

                        // To disable remove btn if quantity is less than 1
                        if(count <= 1)
                        {
                            removeBtn.alpha = 0.2F
                            removeBtn.isEnabled = false
                        }
                    }

                    addProductDialog.show()
                    val btnCancel = dialogBinding.findViewById<Button>(R.id.btn_cancel)
                    btnCancel.setOnClickListener {
                        codeScanner.startPreview()
                        addProductDialog.dismiss()
                    }

                    // To add scanned item to My Cart
                    val addItem = dialogBinding.findViewById<Button>(R.id.btn_addItem)
                    addItem.setOnClickListener {
                        addItemToCart(prodList[0], count, shoppingListAdapter)
                        codeScanner.startPreview()
                        addProductDialog.dismiss()
                    }
                }
                else{
                    Toast.makeText(this@ScanBarcode, "Product not found", Toast.LENGTH_SHORT).show()
                    codeScanner.startPreview()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Database", "Database error")
            }
        })
    }

    private fun addItemToCart(item: ProductModel, itemQty: Int, shoppingListAdapter: ShoppingListAdapter) {

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid.toString()
        var dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("user").child(currentUserId).child("cart").child(item.productId.toString())
        var dbRef2: DatabaseReference = FirebaseDatabase.getInstance().getReference("user").child(currentUserId).child("shoppingList")

        var totalPriceItem = item.price?.times(itemQty)
        val product = CartProductModel(item.brand, item.category, item.imageUrl, item.price, item.productId, item.productName, itemQty, item.quantity, totalPriceItem)
        dbRef.setValue(product)
            .addOnCompleteListener{
                Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {err ->
                Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
            }

        // Checkout the item in the shopping list after adding it to the cart
        val position = shoppingListAdapter.shoppingList.indexOfFirst { it.productId == item.productId }
        Log.e("Checked", "Position: $position")
        if (position != -1) {
            shoppingListAdapter.checkoutItem(position)
            Log.e("Checked", "Item checked")
        }
    }

    // lifecycle method
    // when leave the app and come back to the app, it will try to scan a new barcode
    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    // to avoid memory leaks
    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun setupPermissions(){
        val permission: Int = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED){
            makeRequest()
        }
    }
    private fun makeRequest(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "You need the camera permission to be able to use this app", Toast.LENGTH_SHORT).show()
                }
                else{
                    // successful
                }
            }
        }
    }
}