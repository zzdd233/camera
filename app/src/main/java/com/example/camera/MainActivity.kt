@file:Suppress("DEPRECATION")

package com.example.camera



import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.knight.cameraone.CameraPresenter
import com.knight.cameraone.adapter.PhotosAdapter
import com.knight.cameraone.utils.Permissions
import com.knight.cameraone.utils.SystemUtil
import com.knight.cameraone.utils.ToastUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),View.OnClickListener, CameraPresenter.CameraCallBack,View.OnTouchListener,PhotosAdapter.OnItemClickListener{
    //权限
    private var needPermissions : Array<String> = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)

    //逻辑层
    var mCameraPresenter: CameraPresenter?=null

    val MODE_INIT:Int = 0
    //两个触摸点触摸屏幕状态
    val MODE_ZOOM:Int = 1
    //标识模式
    var mode:Int = MODE_ZOOM
    //两点的初始距离
    var startDis:Float = 0f
    //适配器
    var mPhotosAdapter: PhotosAdapter? = null
    //图片List
    var photoList:MutableList<String>?=null

    var isMove:Boolean = false

    //闪光灯开关
    var isTurn:Boolean = false



    var isFull :Boolean = false
    private var photosFile: File? = null

    //传感器方向监听对象 监听屏幕旋转角度
    var orientationEventListener: OrientationEventListener?=null
    //拍照时方向(传感器角度方向 -> 转变而来)
    var takePhotoOrientation:Int = 90


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getScreenBrightness()
        //闪光灯按钮初始化
        iv_flash.setBackgroundResource(if (isTurn) R.drawable.icon_turnon else R.drawable.icon_turnoff)
        initListener()
        initOrientate()
        checkNeedPermissions()


        //初始化CameraPresenter
        mCameraPresenter = CameraPresenter(this, sf_camera)
        //设置后置摄像头
        mCameraPresenter?.setFrontOrBack(Camera.CameraInfo.CAMERA_FACING_BACK)
        //添加监听
        mCameraPresenter?.setCameraCallBack(this)

        mCameraPresenter?.addressLine= ArrayList()


        photoList = ArrayList<String>()
        photosFile =  this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileList=photosFile?.listFiles()
        fileList?.forEach {
            (photoList as ArrayList<String>).add(it.absolutePath)
        }
        //设置图像
        Glide.with(this).load((photoList as ArrayList<String>).last()).apply(RequestOptions.bitmapTransform(CircleCrop()).override(iv_photo.width, iv_photo.height).error(R.drawable.default_person_icon))
                .into(iv_photo)

    }


    /**
     * 检查权限
     *
     *
     */
    fun checkNeedPermissions(){
        //6.0以上需要动态申请权限 动态权限校验 Android 6.0 的 oppo & vivo 手机时，始终返回 权限已被允许 但是当真正用到该权限时，却又弹出权限申请框。
        when (Build.VERSION.SDK_INT >= 23){
            true -> when (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) !== PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) !== PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            ) !== PackageManager.PERMISSION_GRANTED) {
                //多个权限一起申请
                true -> ActivityCompat.requestPermissions(this, needPermissions, 1)
            }

        }
    }
    /**
     * 动态处理申请权限的结果
     * 用户点击同意或者拒绝后触发
     * @param requestCode 请求码
     * @param permissions 权限
     * @param grantResults 结果码
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            1 -> when (grantResults.size > 1) {
                true -> when (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    true -> when (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        true -> when (grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                        }
                        false -> Permissions.showPermissionSettingDialog(this, needPermissions[1])
                    }
                    false -> Permissions.showPermissionSettingDialog(this, needPermissions[0])
                }
                false -> ToastUtil.showShortToast(this, "请重新尝试~")
            }

        }
    }



    /**
     * 预览回调
     * @param data 预览数据
     *
     */
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if(takePhotoOrientation==0) {
           // Log.i("info", "landscape"); // 横屏
            gp1_water1.visibility=View.INVISIBLE
            gp2_water1.visibility=View.INVISIBLE
            if(mCameraPresenter?.waterType==1){
                gp1_water2.visibility=View.VISIBLE
            }else if(mCameraPresenter?.waterType==2){
                tv_water2.visibility=View.VISIBLE
            }else if(mCameraPresenter?.waterType==3){
                gp3_water1.visibility=View.VISIBLE
            }else if(mCameraPresenter?.waterType==4){
                gp4_water1.visibility=View.VISIBLE
            }
        } else  {
           // Log.i("info", "portrait"); // 竖屏
            gp1_water2.visibility=View.INVISIBLE
            tv_water2.visibility=View.INVISIBLE
            gp1_water1.visibility=View.INVISIBLE
            gp2_water1.visibility=View.INVISIBLE
            gp3_water1.visibility= View.INVISIBLE
            gp4_water1.visibility= View.INVISIBLE
               if(mCameraPresenter?.waterType==1){
                   gp1_water1.visibility=View.VISIBLE
               }else if(mCameraPresenter?.waterType==2){
                   gp2_water1.visibility=View.VISIBLE
               }else if(mCameraPresenter?.waterType==3){
                   gp3_water1.visibility= View.VISIBLE
               }else if(mCameraPresenter?.waterType==4){
                   gp4_water1.visibility= View.VISIBLE
               }
        }
    }
    fun getTakePhotoOrientation ():Int?{
        return takePhotoOrientation
    }

    /**
     *
     * 拍照回调
     * @param data 拍照数据
     *
     */
    override fun onTakePicture(data: ByteArray?, camera: Camera?) {

    }

    /**
     * 人脸检测回调
     * @param rectFArrayList
     *
     */
    override fun onFaceDetect(rectFArrayList: ArrayList<RectF>?, camera: Camera?) {

    }

    /**
     *
     * 返回拍照后的照片
     * @param imagePath
     *
     */
    override fun getPhotoFile(imagePath: String?) {
        //设置图像
        Glide.with(this).load(imagePath).apply(RequestOptions.bitmapTransform(CircleCrop()).override(iv_photo.width, iv_photo.height).error(R.drawable.default_person_icon))
            .into(iv_photo)
        photoList?.add(imagePath!!)
        Log.d("-----------", photoList?.last()!!)
        mPhotosAdapter?.notifyDataSetChanged()
    }

    override fun getVideoFile(videoFilePath: String) {
    }

    //放大缩小
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        //无论多少跟手指加进来，都是MotionEvent.ACTION_DWON MotionEvent.ACTION_POINTER_DOWN
        //MotionEvent.ACTION_MOVE:
        when (event.action and MotionEvent.ACTION_MASK) {
            //手指按下屏幕
            MotionEvent.ACTION_DOWN -> mode = MODE_INIT
            //当屏幕上已经有触摸点按下的状态的时候，再有新的触摸点被按下时会触发
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = MODE_ZOOM
                //计算两个手指的距离 两点的距离
                startDis = SystemUtil.twoPointDistance(event)
            }
            //移动的时候回调
            MotionEvent.ACTION_MOVE -> {
                isMove = true
                //这里主要判断有两个触摸点的时候才触发
                if (mode == MODE_ZOOM) {
                    //只有两个点同时触屏才执行
                    if (event.pointerCount < 2) {
                        return true
                    }
                    //获取结束的距离
                    val endDis = SystemUtil.twoPointDistance(event)
                    //每变化10f zoom变1
                    val scale = ((endDis - startDis) / 10f).toInt()
                    if (scale >= 1 || scale <= -1) {
                        var zoom = mCameraPresenter!!.getZoom() + scale
                        //判断zoom是否超出变焦距离
                        if (zoom > mCameraPresenter!!.getMaxZoom()) {
                            zoom = mCameraPresenter!!.getMaxZoom()
                        }
                        //如果系数小于0
                        if (zoom < 0) {
                            zoom = 0
                        }
                        //设置焦距
                        mCameraPresenter!!.setZoom(zoom)
                        //将最后一次的距离设为当前距离
                        startDis = endDis
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                //判断是否点击屏幕 如果是自动聚焦
                if (!isMove) {
                    //自动聚焦
                    mCameraPresenter?.autoFocus()
                    isMove = false
                }
            }
        }
        return true
    }
    override fun onDestroy() {
        super.onDestroy()
        mCameraPresenter?.releaseCamera()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.iv_photo ->{
                val intent=Intent(this, BigPhotoActivity::class.java)
                intent.putExtra("imagePhoto", photoList?.last())
                intent.putExtra("operation",takePhotoOrientation.toString())
                startActivity(intent)
            }

            //改变摄像头
            R.id.tv_change_camera -> mCameraPresenter?.switchCamera()
            //关闭还是开启闪光灯
            R.id.iv_flash -> {
                mCameraPresenter?.turnLight(isTurn)
                iv_flash.setBackgroundResource(if (isTurn) R.drawable.icon_turnon else R.drawable.icon_turnoff)
                isTurn = !isTurn
            }
            //修改地址
            R.id.iv_gps->{
                val intent=Intent(this,Gps::class.java)
                Log.d("address----------",mCameraPresenter?.addressLine?.isEmpty().toString())
                intent.putExtra("addressLine",mCameraPresenter?.addressLine)

                startActivityForResult(intent,1)
            }
            //更改水印
            R.id.iv_water->{
                startActivityForResult(Intent(this,ChoseWater::class.java),2)
            }
        }
    }
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            1->if(resultCode== RESULT_OK){
                val address=data?.getStringExtra("address")
                val myFmt = SimpleDateFormat("yyyy-MM-dd E ")
                tv_date.text = myFmt.format(Date().time)+" "+address
                tv_date2.text=tv_date.text
                tv_location.text= "地址：$address"
                tv_contain.text="地址：$address"
                tv_work_address.text="地址：$address"
            }
            2->if(resultCode== RESULT_OK){
                if(data?.getStringExtra("type").equals("经典样式")){
                    mCameraPresenter?.waterType = 1
                }else if(data?.getStringExtra("type").equals("现场拍照")){
                    mCameraPresenter?.waterType =2
                }else if(data?.getStringExtra("type").equals("物业管理")){
                    mCameraPresenter?.waterType =3
                }else if(data?.getStringExtra("type").equals("工程记录")){
                    mCameraPresenter?.waterType =4
                }
            }
        }
    }

    override fun onItemClick(v: View, path: String) {
        startActivity(Intent(this, BigPhotoActivity::class.java).putExtra("imagePhoto", path))
    }
    /**
     * 添加点击事件，触摸事件
     *
     */
    private fun initListener(){
        sf_camera.setOnTouchListener(this)
        iv_photo.setOnClickListener(this)
        tv_change_camera.setOnClickListener(this)
        iv_flash.setOnClickListener(this)
        iv_gps.setOnClickListener(this)
        iv_water.setOnClickListener(this)
        //点击事件
        tv_takephoto.setOnClickListener {
                //拍照的调用方法
                mCameraPresenter?.takePicture(takePhotoOrientation)

            }
        }

    /**
     * 调整亮度
     *
     */
    private fun getScreenBrightness(){
        val lp: WindowManager.LayoutParams = window.attributes
        //screenBrightness的值是0.0-1.0 从0到1.0 亮度逐渐增大 如果是-1，那就是跟随系统亮度
        lp.screenBrightness = 200f * (1f/255f)
        window.attributes = lp
    }

    /**
     *
     * 初始化传感器方向 转为拍照方向
     *
     */
    private fun initOrientate(){
        if(orientationEventListener == null){
            orientationEventListener = object:OrientationEventListener(this){
                override fun onOrientationChanged(i: Int) {
                    // i的范围是0-359
                    // 屏幕左边在顶部的时候 i = 90;
                    // 屏幕顶部在底部的时候 i = 180;
                    // 屏幕右边在底部的时候 i = 270;
                    // 正常的情况默认i = 0;
                    takePhotoOrientation = when (i) {
                        in 45..134 -> {
                            180
                        }
                        in 135..224 -> {
                            270
                        }
                        in 225..314 -> {
                            0
                        }
                        else -> {
                            90
                        }
                    }
                }

            }
        }

        orientationEventListener?.enable()
    }

}

