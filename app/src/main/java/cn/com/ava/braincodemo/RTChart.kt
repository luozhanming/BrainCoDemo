package cn.com.ava.braincodemo

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

const val MSG_NOTIFY_DRAW = 1001
const val MSG_QUIT = 1002


class RTChart(ctx: Context, attrs: AttributeSet?, defStyle: Int = 0) : SurfaceView(ctx, attrs, defStyle),
    SurfaceHolder.Callback, Runnable {

    private var maxValue: Int = 100
    private var minValue: Int = 0
    private val midValue
        get() = (maxValue + minValue) / 2
    private lateinit var vAxisName: String
    private lateinit var hAxisName: String

    private var isDrawing = false
    private lateinit var mDrawThread: Thread

    //private lateinit var mCanvas: Canvas
    private lateinit var mDrawHandler: Handler

    init {
        holder.addCallback(this)
        parseAttrs(attrs)
    }

    private fun parseAttrs(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RTChart)
        minValue = typedArray.getInt(R.styleable.RTChart_minValue, 0)
        maxValue = typedArray.getInt(R.styleable.RTChart_maxValue, 100)
        vAxisName = typedArray.getString(R.styleable.RTChart_vAxisName)
        hAxisName = typedArray.getString(R.styleable.RTChart_hAxisNAme)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = when {
            MeasureSpec.getSize(widthMeasureSpec) < 540 -> 540
            else -> MeasureSpec.getSize(widthMeasureSpec)
        }
        val height = width * 9 / 16
        setMeasuredDimension(width, height)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isDrawing = false
        holder?.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isDrawing = true
        mDrawThread = Thread(this)
        mDrawThread.start()
    }

    override fun run() {
        Looper.prepare()
        mDrawHandler = DrawHandler(Looper.myLooper()) {
            drawChart(canvas = it)
        }
        Looper.loop()
    }

    private fun drawChart(canvas: Canvas?) {
        drawChartBg()
        drawAxis()
        drawAxisText()
        drawDataLine()
    }

    private fun drawAxisText() {

    }

    private fun drawDataLine() {


    }

    private fun drawAxis() {

    }

    private fun drawChartBg() {

    }

    fun postNewValue(value: Int) {
        Message.obtain(mDrawHandler, MSG_NOTIFY_DRAW, holder).sendToTarget()
    }


    class DrawHandler(looper: Looper, drawWhat: (canvas: Canvas?) -> Unit) : Handler(looper) {

        private var draw: (canvas: Canvas?) -> Unit

        init {
            draw = drawWhat
        }


        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_NOTIFY_DRAW -> {
                    val holder: SurfaceHolder? = msg?.obj as? SurfaceHolder
                    val canvas = holder?.lockCanvas()
                    draw(canvas)
                    holder?.unlockCanvasAndPost(canvas)
                }
                MSG_QUIT -> {
                    looper.quit()
                }
            }
        }
    }

}

fun Context.dp2px(dip: Int): Int {
    val density = resources.displayMetrics.density
    return (dip * density).toInt()
}