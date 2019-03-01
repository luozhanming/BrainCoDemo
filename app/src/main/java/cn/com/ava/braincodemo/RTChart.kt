package cn.com.ava.braincodemo

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class RTChart(ctx: Context, attrs: AttributeSet?, defStyle: Int = 0) : SurfaceView(ctx, attrs, defStyle),
    SurfaceHolder.Callback,Runnable {


    var isDrawing = false
    lateinit var mDrawThread:Thread



    init {
        holder.addCallback(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = when {
            MeasureSpec.getSize(widthMeasureSpec) < 540 -> 540
            else ->  MeasureSpec.getSize(widthMeasureSpec)
        }
        val height = width * 3 / 4
        setMeasuredDimension(width, height)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isDrawing = false;
        holder?.removeCallback(this);

    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isDrawing = true
        mDrawThread = Thread(this)
        mDrawThread.start()
    }

    override fun run() {
        while (isDrawing){
            val canvas = holder.lockCanvas()
            drawChart(canvas)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawChart(canvas: Canvas?) {

    }


}