package com.mrayush.calculator.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mrayush.calculator.R



class FirstScreen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_first_screen, container, false)

        val next = view.findViewById<TextView>(R.id.tvNext1)
//        val viewPager = activity?.findViewById<ViewPager2>(R.id.view_pager)

        next.setOnClickListener {



        }
        return view
    }


}