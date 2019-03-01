package cn.com.ava.braincodemo

import android.app.Application
import tech.brainco.fusi.sdk.FusiSDK

class MyApp:Application() {

    override fun onCreate() {
        super.onCreate()
        FusiSDK.startLogging()
    }
}