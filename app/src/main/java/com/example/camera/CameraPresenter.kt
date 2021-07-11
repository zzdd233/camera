package com.knight.cameraone

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.Camera
import android.location.Address
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.camera.utils.LocationUtils
import com.example.camera.utils.LocationUtils.AddressCallback
import com.knight.cameraone.utils.ImageUtil
import com.knight.cameraone.utils.SystemUtil
import com.knight.cameraone.utils.ThreadPoolUtil
import com.knight.cameraone.utils.ToastUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class CameraPresenter(mAppCompatActivity: AppCompatActivity, mSurfaceView: SurfaceView) : Camera.PreviewCallback  {


    //相机对象
    private var mCamera: Camera? = null
    //相机对象参数设置
    private var mParameters: Camera.Parameters? = null
    //自定义相机页面
    private var mAppCompatActivity: AppCompatActivity = mAppCompatActivity
    //SurfaceView 用于预览对象
    private var mSurfaceView: SurfaceView = mSurfaceView
    //SurfaceHolder对象
    private var mSurfaceHolder: SurfaceHolder
    //摄像头Id 默认后置 0，前置的值是1
    private var mCameraId: Int = Camera.CameraInfo.CAMERA_FACING_BACK
    //预览旋转的角度
    private var orientation: Int = 0

    //自定义回调
    private var mCameraCallBack: CameraCallBack? = null
    //手机的像素宽和高
    private var screenWidth: Int
    private var screenHeight: Int
    //拍照数量
    private var photoNum: Int = 0
    //拍照存放的文件
    private var photosFile: File? = null
    //当前缩放具体值
    private var mZoom: Int = 0

    private var mediaRecorder: MediaRecorder? = null



    private var isFull = false
    private var times = 0f
    private lateinit var videoFilePath:String
    private lateinit var profile:CamcorderProfile
    private var date:String=""
    //地址列表
    var addressLine:ArrayList<String>?=null
    //水印类型
    var waterType:Int=1


    //最新文件信息
     var messagea: Message? =null

    fun setFull(full: Boolean){
        isFull = full

    }

    //自定义回调
    interface CameraCallBack {
        //预览帧回调
        fun onPreviewFrame(data: ByteArray?, camera: Camera?)

        //拍照回调
        fun onTakePicture(data: ByteArray?, camera: Camera?)

        //人脸检测回调
        fun onFaceDetect(rectFArrayList: ArrayList<RectF>?, camera: Camera?)

        //拍照路径返回
        fun getPhotoFile(imagePath: String?)

        //返回视频路径
        fun getVideoFile(videoFilePath: String)

    }

    fun setCameraCallBack(mCameraCallBack: CameraCallBack) {
        this.mCameraCallBack = mCameraCallBack
    }


    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        initData()
        mCameraCallBack?.onPreviewFrame(data, camera)
    }


    var mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> mCameraCallBack?.getPhotoFile(msg?.obj.toString())

            }
        }
    }


    init {
        mSurfaceView.holder.setKeepScreenOn(true)
        mSurfaceHolder = mSurfaceView.holder
        val dm = DisplayMetrics()
        mAppCompatActivity.windowManager.defaultDisplay.getMetrics(dm)
        //获取宽高像素
        screenHeight = dm.heightPixels
        screenWidth = dm.widthPixels
        setUpFile()
        init()
    }


    /**
     * 创建拍照文件夹
     *
     */
    private fun setUpFile() {

        //app的外部存储私有存储目录 /storage/emulated/0/Android/data/com.example.camera/files/Pictures/
        photosFile =  mAppCompatActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!photosFile!!.exists() || !photosFile!!.isDirectory) {
            var isSuccess: Boolean? = false

            try {
                isSuccess = photosFile?.mkdirs()
            } catch (e: Exception) {
                ToastUtil.showShortToast(mAppCompatActivity, "创建存放目录失败,请检查磁盘空间")
            } finally {
                when (isSuccess) {
                    false -> {
                        ToastUtil.showShortToast(mAppCompatActivity, "创建存放目录失败,请检查磁盘空间")
                        mAppCompatActivity.finish()
                    }

                }
            }

        }

    }


    /**
     * 初始化回调
     *
     */
    private fun init() {
        mSurfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                //surface绘制是执行
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                openCamera(mCameraId)
                //并设置预览
                startPreview()


            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }
        })
    }


    /**
     * 拍照
     *
     */
    fun takePicture(takePhotoOrientation: Int) {
        mCamera?.let {
            //拍照回调 点击拍照时回调
            it.takePicture(object : Camera.ShutterCallback {
                override fun onShutter() {

                }
            }, object : Camera.PictureCallback {
                //回调没压缩的原始数据
                override fun onPictureTaken(data: ByteArray?, camera: Camera?) {

                }
            }, object : Camera.PictureCallback {
                //回调图片数据 点击拍照后相机返回的照片byte数组，照片数据
                override fun onPictureTaken(data: ByteArray?, camera: Camera?) {
                    mCamera?.startPreview()
                    //回调
                    var bitmap = BitmapFactory.decodeByteArray(data, 0, data!!.size);

                    val m = Matrix()
                    m.postRotate(takePhotoOrientation.toFloat())
                    //1是前置
                    if (mCameraId == 1) {
                        if (takePhotoOrientation == 90) {
                            m.postRotate(180f)
                        }
                    }
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                    //如果是前置 需要镜面翻转处理
                    if (mCameraId == 1) {
                        val martix1 = Matrix()
                        martix1.postScale(-1f, 1f)
                        bitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            martix1,
                            true
                        )
                    }
                    bitmap = addWater(bitmap, takePhotoOrientation)

                    mCameraCallBack?.onTakePicture(data, camera)
                    //保存图片
                    if (bitmap != null) {
                        getPhotoPath(bitmap, takePhotoOrientation)
                    }
                }
            })

        }
    }

    /**
     *
     * 设置预览
     */
    fun startPreview() {
        try {
            //根据所传入的SurfaceHolder对象来设置实时预览
            mCamera?.setPreviewDisplay(mSurfaceHolder)
            //调整预览角度
            setCameraDisplayOrientation(mAppCompatActivity, mCameraId, mCamera)
            mCamera?.startPreview()


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }




    /**
     *
     * 设置前置还是后置
     *
     */
    fun setFrontOrBack(mCameraId: Int){
        this.mCameraId = mCameraId

    }



    /**
     *
     * 前后摄像切换
     *
     */
    fun switchCamera(){
       //先释放资源
       releaseCamera()
       //在Android P之前 Android设备仍然最多只有前后两个摄像头，在Android p后支持多个摄像头 用户想打开哪个就打开哪个
       mCameraId = (mCameraId + 1) % Camera.getNumberOfCameras()
        Log.d("ssd", mCameraId.toString())
       //打开摄像头
       openCamera(mCameraId)
       //切换摄像头之后开启预览
       startPreview()
    }

    /**
     * 闪光灯
     * @param turnSwitch true 为开启 false 为关闭
     *
     */
    fun turnLight(turnSwitch: Boolean){
        val parameters = mCamera?.parameters
        parameters?.flashMode = if(turnSwitch) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
        mCamera?.parameters = parameters
    }



    /**
     * 打开相机，并且判断是否支持该摄像头
     *
     * @param FaceOrBack 前置还是后置
     * @return
     *
     */
    fun openCamera(FaceOrBack: Int): Boolean {
        //是否支持前后摄像头
        val isSupportCamera: Boolean = isSupport(FaceOrBack)
        //如果支持
        if (isSupportCamera) {
            try {
                mCamera = Camera.open(FaceOrBack)
                initParameters(mCamera)
                //设置预览回调
                mCamera?.setPreviewCallback(this)

            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtil.showShortToast(mAppCompatActivity, "打开相机失败~")
                return false
            }
        }
        return isSupportCamera

    }

    /**
     * 判断是否支持某个相机
     * @param faceOrBack 前置还是后置
     * @return
     *
     */
    private fun isSupport(faceOrBack: Int): Boolean {
        val cameraInfo: Camera.CameraInfo = Camera.CameraInfo()
        for (index in 0 until Camera.getNumberOfCameras()) {
            //返回相机信息
            Camera.getCameraInfo(index, cameraInfo)
            if (cameraInfo.facing == faceOrBack) {
                return true
            }
        }
        return false
    }


    /**
     * 判断是否支持对焦模式
     * @return
     *
     */
    fun isSupportFocus(focusMode: String): Boolean {
        var isSupport: Boolean = false
        //获取所支持对焦模式
        val listFocus: List<String>? = mParameters?.supportedFocusModes
        for (index in 0 until listFocus!!.size) {
            //如果存在 返回true
            if (listFocus[index].equals(focusMode)) {
                isSupport = true
            }
        }
        return isSupport
    }

    /**
     * 初始化相机参数
     *
     */
    private fun initParameters(camera: Camera?) {
        try {
            //获取Parameters对象
            mParameters = camera?.parameters
            //设置预览格式
            mParameters?.previewFormat = ImageFormat.NV21
            //mParameters?.exposureCompensation = 5
            if(isFull){
                setPreviewSize(screenWidth, screenHeight)
            } else {
                setPreviewSize(mSurfaceView.measuredWidth, mSurfaceView.measuredHeight)
            }
            setPictureSize()
            //连续自动对焦图像
            if (isSupportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParameters?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            } else if (isSupportFocus(Camera.Parameters.FOCUS_MODE_AUTO)) {
                //自动对焦(单次)
                mParameters?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
            mCamera?.parameters = mParameters
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.showShortToast(mAppCompatActivity, "初始化相机失败")
        }

    }

    /**
     *
     * 设置预览界面尺寸
     */
    fun setPreviewSize(width: Int, height: Int) {
        //获取系统支持预览大小
        val localSizes: List<Camera.Size> = mParameters?.supportedPreviewSizes!!
        var biggestSize: Camera.Size? = null //最大分辨率
        var fitSize: Camera.Size? = null//优先选屏幕分辨率
        var targetSize: Camera.Size? = null//没有屏幕分辨率就取跟屏幕分辨率相近(大)的size
        var targetSize2: Camera.Size? = null//没有屏幕分辨率就取跟屏幕分辨率相近(小)的size
        val cameraSizeLength: Int = localSizes.size

        if(width.toFloat() / height == 3.0f / 4){

            for(index in 0 until cameraSizeLength){
                var size : Camera.Size = localSizes[index]
                if(size.width.toFloat() / size.height == 4/0f / 3){
                    mParameters?.setPreviewSize(size.width, size.height)
                    break
                }

            }
        } else {
            for (index in 0 until cameraSizeLength) {
                val size: Camera.Size = localSizes[index]
                if (biggestSize == null || (size.width >= biggestSize.width && size.height >= biggestSize.height)) {
                    biggestSize = size
                }

                //如果支持的比例都等于所获取到的宽高
                if (size.width == screenHeight && size.height == screenWidth) {
                    fitSize = size
                    //如果任一宽高等于所支持的尺寸
                } else if (size.width == screenHeight || size.height == screenWidth) {
                    if (targetSize == null) {
                        targetSize = size
                    } else if (size.width < screenHeight || size.height < screenWidth) {
                        targetSize2 = size
                    }
                }
            }

           if(fitSize == null){
               fitSize = targetSize
           }

           if(fitSize == null){
               fitSize = targetSize2
           }

           if(fitSize == null){
               fitSize = biggestSize
           }

            mParameters?.setPreviewSize(fitSize?.width!!, fitSize?.height!!)

        }




    }

    /**
     * 设置最佳保存图片的尺寸
     *
     */
    private fun setPictureSize() {
        val localSizes: List<Camera.Size> = mParameters?.supportedPreviewSizes!!
        var biggestSize: Camera.Size? = null
        var fitSize: Camera.Size? = null
        val previewSize: Camera.Size? = mParameters?.previewSize
        var previewSizeScale: Float = 0f
        if (previewSize != null) {
            previewSizeScale = previewSize.width / previewSize.height.toFloat()
        }

        val cameraSizeLength: Int = localSizes.size
        for (index in 0 until cameraSizeLength) {
            val size: Camera.Size = localSizes[index]
            if (biggestSize == null) {
                biggestSize = size
            } else if (size.width >= biggestSize.width && size.height >= biggestSize.height) {
                biggestSize = size
            }

            //选出与预览界面等比的最高分辨率
            if (previewSizeScale > 0 && size.width >= previewSize?.width!! && size.height >= previewSize?.height!!) {
                val sizeScale: Float = size.width / size.height.toFloat()
                if (sizeScale == previewSizeScale) {
                    if (fitSize == null) {
                        fitSize = size
                    } else if (size.width >= fitSize.width && size.height >= fitSize.height) {
                        fitSize = size
                    }
                }
            }
        }

        //如果没有选出fitsize，那么最大的Size就是FitSize
        if (fitSize == null) {
            fitSize = biggestSize
        }

        mParameters?.setPictureSize(fitSize?.width!!, fitSize?.height!!)


    }


    /**
     * 保证预览方向正确
     * @param appCompatActivity Activity
     * @param cameraId 相机Id
     * @param camera 相机
     */
    private fun setCameraDisplayOrientation(
        appCompatActivity: AppCompatActivity,
        cameraId: Int,
        camera: Camera?
    ) {
        val info: Camera.CameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        //rotation是预览Window的旋转方向，对于手机而言，当在清单文件设置Activity的screenOrientation="portait"时，
        //rotation=0，这时候没有旋转，当screenOrientation="landScape"时，rotation=1。
        val rotation: Int = appCompatActivity.windowManager.defaultDisplay.rotation
        var degree: Int = 0
        when (rotation) {
            Surface.ROTATION_0 -> degree = 0
            Surface.ROTATION_90 -> degree = 90
            Surface.ROTATION_180 -> degree = 180
            Surface.ROTATION_270 -> degree = 270
        }

        var result: Int = 0
        //计算图像所要旋转的角度
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degree) % 360
            result = (360 - result) % 360

        } else {
            result = (info.orientation - degree + 360) % 360
        }
        orientation = result
        //调整预览图像旋转角度
        camera?.setDisplayOrientation(result)

    }



    /**
     *
     * 释放相机资源
     */
    fun releaseCamera() {
        //停止预览
        mCamera?.stopPreview()
        mCamera?.setPreviewCallback(null)
        //释放相机资源
      //  mCamera?.unlock()
        mCamera?.release()
        mCamera = null
        mHandler.removeMessages(1)
        mediaRecorder?.release()
        mediaRecorder = null
    }


    /**
     * 变焦
     * @param zoom 缩放系数
     */
    fun setZoom(zoom: Int) {
        //获取Paramters对象
        val parameters: Camera.Parameters? = mCamera?.parameters
        //如果不支持变焦
        if (!parameters?.isZoomSupported!!) {
            return
        }

        parameters.zoom = zoom
        //Camera对象重新设置Paramters对象参数
        mCamera?.parameters = parameters
        mZoom = zoom

    }

    /**
     *
     * 返回缩放值
     * @return 返回缩放值
     */
    fun getZoom(): Int {
        return mZoom
    }

    /**
     * 获取最大Zoom值
     * @return zoom
     */
    fun getMaxZoom(): Int {
        val parameters: Camera.Parameters? = mCamera?.parameters
        if (!parameters?.isZoomSupported!!) {
            return -1
        }
        return if (parameters.maxZoom > 50) {
            50
        } else {
            parameters.maxZoom
        }


    }


    /**
     *
     * 自动变焦
     *
     */
    fun autoFocus(){
        mCamera?.autoFocus(object : Camera.AutoFocusCallback {
            override fun onAutoFocus(success: Boolean, camera: Camera?) {

            }
        })
    }


    /**
     * @return 返回路径
     *
     *
     */
    fun getPhotoPath(bitmap: Bitmap, takePhotoOrientation: Int) {
        ThreadPoolUtil.execute(object : Runnable {
            override fun run() {
                val timeMillis: Long = System.currentTimeMillis()
                var time: String = SystemUtil.formatTime(timeMillis)
                //拍照数量
                photoNum++
                //图片名字
                val name: String = SystemUtil.formatTime(
                    timeMillis, SystemUtil.formatRandom(
                        photoNum
                    ) + ".jpg"
                )
                //创建具体文件
                val file = File(photosFile, name)
                if (!file.exists()) {
                    try {
                        file.createNewFile()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    }

                }


                try {
                    val fos = FileOutputStream(file)
                    try {
                        //将数据写入文件
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            fos.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    ImageUtil.saveAlbum(mAppCompatActivity, file)

                    val message = Message()

                    message.what = 1
                    //    message.obj = Configuration.insidePath + file.name
                    message.obj = file.absolutePath
                    messagea = message
                    mHandler.sendMessage(message)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        })
    }


    private fun addWater(mBitmap: Bitmap, orientation: Int): Bitmap? {
        var bitmapConfig = mBitmap.config
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888
        }
        //获取原始图片与水印图片的宽与高
        val mBitmapWidth = mBitmap.width
        val mBitmapHeight = mBitmap.height
        val dm = DisplayMetrics()
        mAppCompatActivity.windowManager.defaultDisplay.getMetrics(dm)

        val screenWidth = dm.widthPixels.toFloat() //1080
        val mBitmapWidthF = mBitmapWidth.toFloat()
        times = mBitmapWidthF / screenWidth
        val mNewBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, bitmapConfig)
        val canvas = Canvas(mNewBitmap)
        //向位图中开始画入MBitmap原始图片
        canvas.drawBitmap(mBitmap, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val location = IntArray(2)

        paint.color = Color.WHITE
        paint.isDither = true //获取跟清晰的图像采样
        paint.isFilterBitmap = true //过滤一些

        var myTextView: TextView
        myTextView = if (waterType==1){
            mAppCompatActivity.tv_word
        }else
            mAppCompatActivity.tv_time

        paint.textSize = myTextView.textSize
        var text = myTextView.text.toString()
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        myTextView.getLocationOnScreen(location)
        val textH = -paint.ascent() + paint.descent()//字符高度
        var firstY=0f//横屏时第一个字符y坐标

        if(orientation==0){//横屏
            myTextView = if (waterType==1){
                mAppCompatActivity.tv_word2
            }else
                mAppCompatActivity.tv_water2
            myTextView.getLocationOnScreen(location)
            canvas.drawText(text, location[1].toFloat(), 2 * location[0].toFloat(), paint) //mBitmapWidth=3024
             firstY=2 * location[0].toFloat()//横屏时第一个字符y坐标
        }
        else{//竖屏
            canvas.drawText(text, location[0].toFloat(), location[1].toFloat(), paint) //mBitmapWidth=3024
        }

        if(waterType==1){
            myTextView=mAppCompatActivity.tv_date

            paint.textSize = myTextView.textSize
            text=myTextView.text.toString()
            paint.getTextBounds(text, 0, text.length, bounds)
            myTextView.getLocationOnScreen(location)
            if(orientation==0){//横屏
                if(waterType==1){
                    myTextView= mAppCompatActivity.tv_date2
                }
                myTextView.getLocationOnScreen(location)
                text=myTextView.text.toString()
                canvas.drawText(text, location[1].toFloat(), firstY+textH, paint) //mBitmapWidth=3024
            }
            else{//竖屏
                canvas.drawText(text, location[0].toFloat(), location[1].toFloat(), paint) //mBitmapWidth=3024
            }
        }

        canvas.save()
        return mNewBitmap
    }
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun initData() {
        val myFmt = SimpleDateFormat("yyyy-MM-dd E ")
        val myFmt2 = SimpleDateFormat("HH:mm ")

        var mString=""
        //注意6.0及以上版本需要在申请完权限后调用方法
        date = myFmt.format(Date().time)

        mAppCompatActivity.tv_word.text=myFmt2.format(Date().time)
        mAppCompatActivity.tv_word2.text=myFmt2.format(Date().time)
        mAppCompatActivity.tv_time.text="时间："+date+myFmt2.format(Date().time)
        mAppCompatActivity.tv_workTime.text=myFmt2.format(Date().time)+"\n"+date
        mAppCompatActivity.tv_work_title.text="工程记录\n"+date+myFmt2.format(Date().time)
        if(addressLine!!.isEmpty()){
            LocationUtils.getInstance(mAppCompatActivity)!!.setAddressCallback(object :
                AddressCallback {
                override fun onGetAddress(address: Address?) {
                    val countryName: String = address!!.countryName //国家
                    val adminArea: String = address.adminArea //省
                    val locality: String = address.locality //市
                    val subLocality: String = address.subLocality //区
                    //val featureName: String = address.featureName //街道


                    if (addressLine?.size == 0) {
                        addressLine?.add(locality + subLocality)
                        date += locality + subLocality
                        mAppCompatActivity.tv_date2.text = date
                        mAppCompatActivity.tv_date.text = date
                        mAppCompatActivity.tv_location.text = "地址：$locality$subLocality"
                        mAppCompatActivity.tv_contain.text="地址：$locality$subLocality"
                        mAppCompatActivity.tv_work_address.text="地址：$locality$subLocality"
                    }
                    var i = 0
                    var ad: String = address.getAddressLine(i)
                    while (address.getAddressLine(i) != null) {
                        ad = address.getAddressLine(i)
                        if (addressLine!!.contains(ad)) {
                            i++
                            continue
                        }
                        addressLine?.add(ad)
                        Log.d("定位地址1", ad)
                        i++
                    }
                    Log.d("定位地址2", countryName + adminArea + locality + subLocality)
                }

                override fun onGetLocation(lat: Double, lng: Double) {
                    // Log.d("定位经纬度", lat.toString()+ lng.toString())
                    mAppCompatActivity.tv_latitude.text= "纬度："+String.format("%.4f",lat)+"°N"
                    mAppCompatActivity.tv_longitude.text="经度："+String.format("%.4f",lng)+"°E"




                }
            })


        }
        mString+=mAppCompatActivity.tv_time.text.toString()+"\n" +
                ""+mAppCompatActivity.tv_latitude.text.toString()+ "\n" +
                ""+mAppCompatActivity.tv_longitude.text.toString()+"\n" +
                ""+mAppCompatActivity.tv_location.text.toString()
        mAppCompatActivity.tv_time.text=mString
        mAppCompatActivity.tv_water2.text=mString
        val locationUtils=LocationUtils



    }
    /**
     * dip转pix
     */
    fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @return
     */
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }
}