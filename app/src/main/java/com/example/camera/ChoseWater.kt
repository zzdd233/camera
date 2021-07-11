package com.example.camera

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_chose_water.*
import kotlinx.android.synthetic.main.activity_gps.*

class ChoseWater : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chose_water)

        val list= listOf("经典样式","现场拍照","物业管理","工程记录")
        val adapter= ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,list)

        lv_water.adapter=adapter
        lv_water.setOnItemClickListener{parent,view,position,id->
            setResult(RESULT_OK, Intent().putExtra("type", list[position]))
            finish()
        }
    }
}