package com.smallcake.demo.ocrdemo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.blankj.utilcode.util.FileIOUtils
import com.googlecode.tesseract.android.TessBaseAPI
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.smallcake.demo.ocrdemo.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.lang.StringBuilder
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 1.对图片进行降噪以及二值化，凸显内容区域
2.对图片进行轮廓检测
3.对轮廓结果进行分析
4.剪裁指定区域
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding
    private  val TAG = "MainActivity"
    private val DATA_PATH = (Environment.getExternalStorageDirectory().toString())

    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private val tessdata = DATA_PATH + File.separator + "tessdata"
    private val language = "chi_sim"

    private lateinit var tv:TextView

    private val pic = R.mipmap.ic_img

    private var showBitmap:Bitmap?=null


    private var ocrDataFilePath = "" //数据识别的文件路径

    private var ocrInfoText: StringBuilder = StringBuilder()
    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                0->{
                    val info:String = msg.obj as String
                    ocrInfoText.appendLine(info)
                }
                1->{
                    //识别成功
                    tv.text = ocrInfoText.toString()
                }
            }

            super.handleMessage(msg)
        }
    }

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        setContentView(R.layout.activity_main)
        val ivShow= findViewById<ImageView>(R.id.iv_show)
        tv= findViewById(R.id.tv_txt_show)
        checkPermission(this)
        //加载图片中的文字
        findViewById<Button>(R.id.btn_load_txt).setOnClickListener {
            showBitmap?.let { it1 -> loadTxt(it1) }
        }
        findViewById<Button>(R.id.btn_made).setOnClickListener {
            ocrInfoText.clear()
            showBitmap = madeImg()
            ivShow.setImageBitmap(showBitmap)
        }
        //二值化
        findViewById<Button>(R.id.btn_threshold).setOnClickListener {
            showBitmap = threshold()
            ivShow.setImageBitmap(showBitmap)
        }
        //变灰色
        findViewById<Button>(R.id.btn_to_gray).setOnClickListener {
            showBitmap = toGray()
            ivShow.setImageBitmap(showBitmap)
        }
        //边缘提取
        findViewById<Button>(R.id.btn_canny).setOnClickListener {
            showBitmap = canny()
            ivShow.setImageBitmap(showBitmap)
        }



    }
    private fun checkPermission(activity:AppCompatActivity){
        XXPermissions.with(activity)
            .permission(arrayOf(Permission.MANAGE_EXTERNAL_STORAGE, Permission.CAMERA))
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) {

                    } else {
                        Log.e("TAG","获取部分权限成功,但部分权限未正常授予")
                    }
                }
                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) {
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(activity, permissions)
                    } else {
                        Log.e("TAG","获取权限失败")
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        initOpenCv()
    }

    /**
     * 加载OpenCv库
     */
    private fun initOpenCv() {
        val b = OpenCVLoader.initDebug()
        if (b) {
            Log.e(TAG,"OpenCv初始化成功")
        }
    }
    private val mHanler=Handler{
        val str = it.obj as String
        tv.text = str
        false
    }

    /**
     * 解析图片中的文字
     * @param imageFile File
     */
    private fun loadTxt(imageFile:File) {
        Thread{
            val t1 =System.currentTimeMillis()
            val tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(DATA_PATH, language)
            tessBaseAPI.setImage(imageFile)
            val retStr = tessBaseAPI.utF8Text
            tessBaseAPI.clear()
            tessBaseAPI.end()
            Log.e(TAG,"retStr==$retStr")
            val msg = mHanler.obtainMessage()
            msg.obj = retStr
            val t2 =System.currentTimeMillis()
            val tx = t2-t1
            Log.i(TAG,"解析图片耗时：${tx.toTimeMs()}")
            mHanler.sendMessage(msg)
        }.start()
    }
    private fun loadTxt(bitmap:Bitmap) {
        Thread{
            val t1 =System.currentTimeMillis()
            val tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(DATA_PATH, language)
            tessBaseAPI.setImage(bitmap)
            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "厂丿「」‖ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?…】丨") // 识别黑名单
            val retStr = tessBaseAPI.utF8Text
            tessBaseAPI.clear()
            tessBaseAPI.end()
            Log.e(TAG,"retStr==$retStr")
            val msg = mHanler.obtainMessage()
            msg.obj = retStr
            val t2 =System.currentTimeMillis()
            val tx = t2-t1
            Log.i(TAG,"解析图片耗时：${tx.toTimeMs()}")
            mHanler.sendMessage(msg)
        }.start()
    }

    private fun toGray():Bitmap{
        val bitmap = BitmapFactory.decodeResource(resources, pic)
        val src = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src) //将Bitmap对象转换为Mat对象
        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 0.0, 0.0) //高斯模糊去噪
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY) //将图像转换为灰度图像
        Imgproc.Laplacian(src, dst, -1, 3) //Laplace边缘提取
        Utils.matToBitmap(dst, bitmap) //将Mat对象转换为Bitmap对象
        return  bitmap

    }
    //Canny（边缘提取）
    private fun canny():Bitmap{
        val bitmap = BitmapFactory.decodeResource(resources, pic)
        val src = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src) //将Bitmap对象转换为Mat对象
        Imgproc.Canny(src, dst, 20.0, 200.0) //边缘提取
        Utils.matToBitmap(dst, bitmap) //将Mat对象转换为Bitmap对象
        return  bitmap
    }
    //二值化
    private fun threshold():Bitmap{
        val bitmap = BitmapFactory.decodeResource(resources, pic) //获取源图像Bitmap对象
        val src = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src) //Bitmap转换为Mat对象
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGBA2GRAY) //颜色空间转换
        Imgproc.threshold(dst, dst, 100.0, 255.0, Imgproc.THRESH_BINARY) //图像二值化
        Utils.matToBitmap(dst, bitmap) //Mat转换为Bitmap对象

        return  bitmap
    }
    //二值化
    private fun madeImg():Bitmap{
        val bitmap = BitmapFactory.decodeResource(resources, pic) //获取源图像Bitmap对象
        val src = Mat()
        Utils.bitmapToMat(bitmap, src) //将bitmap转换为Mat
        val thresholdImage = Mat(src.size(),src.type()) //这个二值图像用于找出关键信息的图像
        val thresholdImageOcr = Mat(src.size(),src.type()) //这个二值图像用于识别信息
        val cannyImage = Mat(src.size(),src.type())
        //将图像转换为灰度图像
        Imgproc.cvtColor(src, thresholdImage, Imgproc.COLOR_RGBA2GRAY)
        //将图像转换为边缘二值图像
        Imgproc.threshold(thresholdImage,thresholdImageOcr,140.0,255.0, Imgproc.THRESH_BINARY)
        Imgproc.threshold(thresholdImage,thresholdImage,100.0,255.0, Imgproc.THRESH_BINARY)

        //闭操作去掉多余的杂点
        var kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(bitmap.width * 0.002, bitmap.width * 0.002)) //获取结构元素
        Imgproc.morphologyEx(thresholdImage, thresholdImage, Imgproc.MORPH_CLOSE, kernel)
        //显示当前阶段效果图像
        val binaryBitmap = Bitmap.createBitmap(bitmap.width,bitmap.height,bitmap.config)
        Utils.matToBitmap(thresholdImageOcr,binaryBitmap)
//        activityMainBinding.imgBinary.setImageBitmap(binaryBitmap)

        //开操作让数字联结到一起方便查出数字的位置
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(bitmap.width * 0.05, bitmap.width * 0.036)) //获取结构元素
        Imgproc.morphologyEx(thresholdImage, cannyImage, Imgproc.MORPH_OPEN, kernel)
        //显示当前阶段效果图像
        val couplingBitmap = Bitmap.createBitmap(bitmap.width,bitmap.height,bitmap.config)
        Utils.matToBitmap(cannyImage,couplingBitmap)
//        activityMainBinding.imgCoupling.setImageBitmap(couplingBitmap)


        //查找边缘
        Imgproc.Canny(cannyImage, cannyImage, 100.0, 200.0,3)

        //膨胀让边缘更明显
        kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(bitmap.width * 0.0036, bitmap.width * 0.0036)) //获取结构元素
        Imgproc.dilate(cannyImage, cannyImage, kernel) //膨胀操作
        //显示当前阶段效果图像
        val contoursBitmap = Bitmap.createBitmap(bitmap.width,bitmap.height,bitmap.config)
        Utils.matToBitmap(cannyImage,contoursBitmap)
//        activityMainBinding.imgContours.setImageBitmap(contoursBitmap)

        val hierarchy = Mat()
        val contours: ArrayList<MatOfPoint> = ArrayList()
        //轮廓发现
        Imgproc.findContours(
            cannyImage,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_NONE
        )

        //找出信息所在的轮廓
        val allRect = ArrayList<Rect>()
        contours.forEach {
            val rect = Imgproc.boundingRect(it)
            //left为0的是不正确的
            if(rect.x != 0 && rect.x + rect.width != src.width()){
                //对区域进行小量的扩充，方便识别数据
                rect.x = rect.x - 10
                rect.y = rect.y - 10
                rect.width = rect.width + 20
                rect.height = rect.height + 20
                allRect.add(rect)
            }
        }
        //对包括头像的全部区域进行排序（头像排在最后）
        allRect.sortByDescending { -(it.x + it.width) }
        //展示头像区域
        val userIconRect = allRect[allRect.size - 1]
        val userBitmap = Bitmap.createBitmap(bitmap,userIconRect.x,userIconRect.y,userIconRect.width,userIconRect.height)
//        activityMainBinding.imgUser.setImageBitmap(userBitmap)
        //剔除头像区域（方便后面的OCR识别）
        val infoRect = ArrayList<Rect>(allRect.take(allRect.size - 1))

        //对矩形轮廓进行排序（姓名往下依次排列）
        val c1: java.util.Comparator<Rect> = Comparator { o1, o2 ->
            if (abs(o1.y - o2.y) <= 10) {
                //可容误差为10（因为有些识别的区域y轴存在小量偏移，比如性别和民族）
                //当y轴在同一位置的时候，比较x轴
                o1.x - o2.x
            } else {
                o1.y - o2.y
            }
        }
        infoRect.sortWith(c1)

        //画出信息所在的位置
        val showInfoRectImg = Mat()
        src.copyTo(showInfoRectImg)
        infoRect.forEach {
            Imgproc.rectangle(showInfoRectImg,it,
                Scalar(0.0, 255.0, 0.0, 255.0),(bitmap.width * 0.003).toInt(), 8)
        }
        Utils.matToBitmap(showInfoRectImg,bitmap)
//        activityMainBinding.imgNumrect.setImageBitmap(bitmap)


        ocrInfo(binaryBitmap,infoRect)


        //释放资源
        thresholdImage.release()
        thresholdImageOcr.release()
        cannyImage.release()
        src.release()
        showInfoRectImg.release()

        return  bitmap
    }

    /**
     * 根据信息所在的位置，识别信息
     */
    private fun ocrInfo(dstBitmap: Bitmap, infoRect: ArrayList<Rect>) {
        thread {
            initOcrData()
            if(!TextUtils.isEmpty(ocrDataFilePath)){
                // 开始调用Tess函数对图像进行识别
                val tessBaseAPI = TessBaseAPI()
                tessBaseAPI.setDebug(true)
                tessBaseAPI.init(ocrDataFilePath, "chi_sim")
//                tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789") // 识别白名单
                tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?…】丨") // 识别黑名单
                tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD)//设置识别模式

                infoRect.forEach {
                    val ocrBitmap = Bitmap.createBitmap(dstBitmap,it.x,it.y,it.width,it.height)
                    //当识别的字为单个字符的时候，切换识别模式为单字符的，识别的比较准确（这里设置宽高比只要小于1.5就是单字符）
                    //用大的值除以小的值，这样才不至于产生小于0的值
                    val maxVal = max(ocrBitmap.width,ocrBitmap.height).toDouble()
                    val minVal = min(ocrBitmap.width,ocrBitmap.height).toDouble()
                    if(maxVal / minVal <= 1.5){
                        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD)//设置识别模式
                    }else{
                        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)//设置识别模式
                    }
                    tessBaseAPI.setImage(ocrBitmap)//设置需要识别图片的bitmap
                    val number = tessBaseAPI.utF8Text
                    val msg = Message()
                    msg.what = 0
                    msg.obj = number
                    handler.sendMessage(msg)
                }
                tessBaseAPI.end()
                handler.sendEmptyMessage(1)
            }
        }
    }
    /**
     * 加载数据识别的文件
     */
    private fun initOcrData() {
        val ocrDataStream = resources.openRawResource(R.raw.chi_sim)
        val file = this.externalCacheDir
        file?.let {
            ocrDataFilePath = it.absolutePath
            val ocrDataFile = File("${ocrDataFilePath}${File.separator}tessdata${File.separator}chi_sim.traineddata")
            if(!ocrDataFile.exists()){
                FileIOUtils.writeFileFromIS(ocrDataFile,ocrDataStream)
            }
        }
    }
}

fun Long.toTimeMs():String{
    val s = this/1000
    val ms = this%1000
    return "${s}s${ms}ms"
}