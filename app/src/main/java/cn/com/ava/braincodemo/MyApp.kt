package cn.com.ava.braincodemo

import android.app.Application
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.DiskLogAdapter
import tech.brainco.fusi.sdk.FusiSDK
import java.util.logging.Logger

class MyApp:Application() {

    override fun onCreate() {
        super.onCreate()
        FusiSDK.startLogging()
        com.orhanobut.logger.Logger.addLogAdapter(AndroidLogAdapter())
        com.orhanobut.logger.Logger.addLogAdapter(DiskLogAdapter())
    }
}