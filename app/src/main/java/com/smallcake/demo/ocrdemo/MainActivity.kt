package com.smallcake.demo.ocrdemo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File


class MainActivity : AppCompatActivity() {
    private  val TAG = "MainActivity"
    private val DATA_PATH = (Environment.getExternalStorageDirectory().toString())

    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private val tessdata = DATA_PATH + File.separator + "tessdata"
    var language = "chi_sim"

    private lateinit var tv:TextView

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val ivShow= findViewById<ImageView>(R.id.iv_show)
        findViewById<Button>(R.id.btn_select).setOnClickListener {
//            SelectImgUtils.getOnePic(this){
//                val file = File(it)
//                loadTxt(file)
//            }
            val bitmapGray = matToBitmap()
            ivShow.setImageBitmap(bitmapGray)

        }
        tv= findViewById(R.id.tv_select)


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

    private fun loadTxt(imageFile:File) {
        Thread{
            val tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(DATA_PATH, language)
            tessBaseAPI.setImage(imageFile)
            val retStr = tessBaseAPI.utF8Text
            tessBaseAPI.clear()
            tessBaseAPI.end()
            Log.e(TAG,"retStr==$retStr")
            val msg = mHanler.obtainMessage()
            msg.obj = retStr
            mHanler.sendMessage(msg)
        }.start()


    }

    private fun matToBitmap():Bitmap{
        val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_hehua)
        val src = Mat()
        val dst = Mat()
        Utils.bitmapToMat(bitmap, src) //将Bitmap对象转换为Mat对象

        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 0.0, 0.0) //高斯模糊去噪

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY) //将图像转换为灰度图像

        Imgproc.Laplacian(src, dst, -1, 3) //Laplace边缘提取

        Utils.matToBitmap(dst, bitmap) //将Mat对象转换为Bitmap对象
        return  bitmap

    }
}