package com.mrayush.calculator

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mrayush.calculator.screens.DataScreen
import com.mrayush.calculator.screens.FirstScreen
import com.mrayush.calculator.screens.SecondScreen
import com.mrayush.calculator.screens.ThirdScreen


class OnBoardingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_on_boarding, container, false)


        val fragmentList = arrayListOf<Fragment>(
            FirstScreen(),
            SecondScreen(),
            ThirdScreen())

//        val adapter = ViewPagerAdapter(
//            fragmentList,
//            requireActivity().supportFragmentManager,
//            lifecycle
//        )



        return view
    }


}