package com.smallcake.demo.ocrdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

class MainActivity : AppCompatActivity() {
    private  val TAG = "MainActivity"
    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_select).setOnClickListener {
            SelectImgUtils.getOnePic(this){
                val file = File(it)
                loadTxt(file)
            }
        }


    }

    private fun loadTxt(imageFile:File) {
      val   mTess = TessBaseAPI()
        val datapath = Environment.getExternalStorageDirectory().toString() + "/tesseract/"
        val language = "eng"
        val dir = File(datapath + "tessdata/")
        if (!dir.exists()) dir.mkdirs()
        mTess.init(datapath, language)

        Thread{
            mTess.setImage(imageFile)
            val text = mTess.utF8Text
            Log.i(TAG, "识别出来的文字为：$text")
        }.start()




    }
}