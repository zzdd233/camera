package com.example.camera

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_gps.*
import java.util.ArrayList

class Gps : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps)

        val addressLine: ArrayList<String>? =intent.getStringArrayListExtra("addressLine")

        //val adapter=GpsAdapter(this,R.layout.activity_gps_item,addressLine)
        val adapter=ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,addressLine!!.toList())

        lv_gps.adapter=adapter
        lv_gps.setOnItemClickListener{parent,view,position,id->
            setResult(RESULT_OK, Intent().putExtra("address", addressLine[position]))
            Log.d("chose address", addressLine[position])
            finish()
        }
    }
}