package com.example.smartscan_beta_v01

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.RoundingMode
import java.text.DecimalFormat


class Checkout : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val productList : MutableList<CartProductModel> = mutableListOf()
    private lateinit var googlePlayButton: PayButton
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var totalTextView : TextView
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    private var total: Long = 0L

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Initialize the Google Pay PaymentsClient
        paymentsClient = createPaymentsClient(this)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid.toString()
        val sheets = findViewById<FrameLayout>(R.id.fl_checkout)
        googlePlayButton = findViewById(R.id.google_pay_payment_button)

        // Fetching from Firebase
        database = FirebaseDatabase.getInstance().getReference("user").child(currentUserId).child("cart")
        database.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for(snapshot2 in snapshot.children){
                    val product = snapshot2.getValue(CartProductModel::class.java)
                    product?.let {
                        productList.add(it)
                    }
                }
                updateScrollView()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Database Error", "Error fetching data from Firebase: ${error.message}")
            }
        })

        // Bottom sheet for the total price and button
        BottomSheetBehavior.from(sheets).apply {
            this.state = BottomSheetBehavior.STATE_EXPANDED
            this.isDraggable = false
        }

        // Google Pay button
        val payButton: PayButton = findViewById(R.id.google_pay_payment_button)
        val paymentMethods: JSONArray = JSONArray().put("")
        payButton.initialize(
            ButtonOptions.newBuilder()
                .setButtonTheme(ButtonConstants.ButtonTheme.LIGHT)
                .setButtonType(ButtonConstants.ButtonType.PAY)
                .setCornerRadius(100)
                .setAllowedPaymentMethods(paymentMethods.toString())
                .build()
        )

        payButton.setOnClickListener { requestPayment() }
    }

    @SuppressLint("MissingInflatedId")
    private fun updateScrollView() {
        val scrollView: ScrollView = findViewById(R.id.scrollView)
        val parentLinearLayout: LinearLayout = findViewById(R.id.parentLinearLayout)

        // Clear the previous views
        parentLinearLayout.removeAllViews()

        // Group products by categories using a HashMap
        val productMap: HashMap<String, MutableList<CartProductModel>> = HashMap()
        for (product in productList) {
            val category = product.category ?: "Uncategorized"
            if (!productMap.containsKey(category)) {
                productMap[category] = mutableListOf()
            }
            productMap[category]?.add(product)
        }

        // Create a custom view for each category and its products
        for (category in productMap.keys) {
            val categoryView = layoutInflater.inflate(R.layout.item_category, null)
            val categoryNameTextView: TextView = categoryView.findViewById(R.id.categoryNameTextView)
            val subtotalTextView : TextView = categoryView.findViewById(R.id.categorySubTotal)

            // Set the category name as the header for each group
            categoryNameTextView.text = category

            // Calculate and display the subtotal for each category
            val df1 = DecimalFormat("#.##")
            df1.roundingMode = RoundingMode.DOWN
            subtotalTextView.text = df1.format(calculateSubtotal(productMap[category]!!)).toString()
//            subtotalTextView.text = "${calculateSubtotal(productMap[category]!!)}"

            parentLinearLayout.addView(categoryView)

            // Create a LinearLayout for each category to host the products
            val categoryLinearLayout = LinearLayout(this)
            categoryLinearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            categoryLinearLayout.orientation = LinearLayout.VERTICAL


            // Create a custom view for each product in the category
            for (product in productMap[category]!!) {
                val customView = layoutInflater.inflate(R.layout.checkout_listitem, null)

                val productNameTextView: TextView = customView.findViewById(R.id.tv_prodName)
                val productPriceTextView: TextView = customView.findViewById(R.id.tv_cart_itemPrice)
                val prodcutImage: ImageView = customView.findViewById(R.id.iv_itemImage)
                val productQtyBrand: TextView = customView.findViewById(R.id.tv_prodQty_brand)

                productNameTextView.text = product.productName
                val df3 = DecimalFormat("#.##")
                df3.roundingMode = RoundingMode.DOWN
                productPriceTextView.text = "${df3.format(product.price?.times(product.quantity!!))}"
                val prodcutImageUrl = product.imageUrl
                Picasso.get().load(prodcutImageUrl).into(prodcutImage)
                productQtyBrand.text = "Quantity: ${product.quantity}"

                categoryLinearLayout.addView(customView)
            }
            parentLinearLayout.addView(categoryLinearLayout)
        }

        // Calculate and display the total price of all items
        totalTextView = findViewById(R.id.btmsheet_totalprice)
        total = calculateTotal((productList)).toLong()

        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.DOWN
        totalTextView.text = df.format(calculateSubtotal(productList))
    }

    private fun calculateSubtotal(products: List<CartProductModel>): Double {
        return products.sumByDouble { it.price?.times(it.quantity!!) ?: 0.0 }
    }

    private fun calculateTotal(products: List<CartProductModel>): Double {
        return products.sumByDouble { it.price?.times(it.quantity!!) ?: 0.0 }
    }

    private fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    private fun requestPayment() {

        val totalPrice = total

        val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(totalPrice)
        if (paymentDataRequestJson == null) {
            Log.e("RequestPayment", "Can't fetch payment data request")
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Value passed in AutoResolveHelper
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK ->
                        data?.let { intent ->
                            PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                        }

                    RESULT_CANCELED -> {
                        // The user cancelled the payment attempt
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            handleError(it.statusCode)
                        }
                    }
                }
            }
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson() ?: return

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)
            Toast.makeText(this, "Payment successful", Toast.LENGTH_LONG).show()
            // Logging token string.
            Log.d("GooglePaymentToken", paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token"))
            val paymentsuccess = Intent(this, PaymentSuccess::class.java)
            startActivity(paymentsuccess)
            finish()
        } catch (e: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $e")
        }
    }
    private fun handleError(statusCode: Int) {
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode))
    }
}