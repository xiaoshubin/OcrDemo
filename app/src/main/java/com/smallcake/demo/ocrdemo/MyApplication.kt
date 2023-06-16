package com.smallcake.demo.ocrdemo

import android.app.Application
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance

class MyApplication : Application() {
   companion object{
       lateinit var instance:MyApplication
   }

    override fun onCreate() {
        super.onCreate()
        instance= this
    }
}