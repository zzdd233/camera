package com.example.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_big_photo.*
import java.security.MessageDigest


class BigPhotoActivity : AppCompatActivity() {
    var path:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_big_photo)
        path = intent.getStringExtra("imagePhoto")
        val option=intent.getStringExtra("operation")?.toInt()
        if (option==0){
            Glide.with(this).load(path)
                    .error(R.drawable.default_person_icon)
                    .into(iv_photo)
        }else{
            Glide.with(this).load(path)
                    .error(R.drawable.default_person_icon)
                    .into(iv_photo)
        }

    }
}
class RotateTransformation(private var rotateRotationAngle: Float) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotateRotationAngle)
        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
    }
}
