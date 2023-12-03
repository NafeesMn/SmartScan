package com.example.smartscan_beta_v01

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShoppingCart : Fragment() {

    private lateinit var viewModel: CartViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RvCartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shopping_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.rv_cart_list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        adapter = RvCartAdapter(context)
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[CartViewModel::class.java]

        viewModel.allCart.observe(viewLifecycleOwner, Observer {
            adapter.updateCartList(it)
        })

    }
}