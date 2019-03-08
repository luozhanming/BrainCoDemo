package cn.com.ava.braincodemo

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.*

const val MSG_INITIAL = 1000
const val MSG_NOTIFY_DRAW = 1001
const val MSG_QUIT = 1002


class RTChart(ctx: Context, attrs: AttributeSet?, defStyle: Int = 0) : SurfaceView(ctx, attrs, defStyle),
    SurfaceHolder.Callback, Runnable {

    private var maxValue: Int = 100
    private var minValue: Int = 0
    private val midValue
        get() = (maxValue + minValue) / 2
    private var vAxisName: String? = ""
    private var hAxisName: String? = ""
    private var vScaleNum: Int = 10
    private var hScaleNum: Int = 10

    private var isDrawing: Boolean = false
    private lateinit var mDrawThread: Thread
    private lateinit var mDrawHandler: Handler

    private var mDrawPaint: Paint

    /**
     * x轴数据偏移
     * */
    private var xOffsetIndex: Int = 0

    /**
     *
     * */
    private val mDataDeque: ArrayList<Int> by lazy {
        ArrayList<Int>(10)
    }

    constructor(ctx: Context, attrs: AttributeSet?) : this(ctx, attrs, 0)

    init {
        mDrawPaint = Paint()
        holder.addCallback(this)
        parseAttrs(attrs)
    }

    private fun parseAttrs(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RTChart)
        minValue = typedArray.getInt(R.styleable.RTChart_minValue, 0)
        maxValue = typedArray.getInt(R.styleable.RTChart_maxValue, 100)
        vAxisName = typedArray.getString(R.styleable.RTChart_vAxisName)
        hAxisName = typedArray.getString(R.styleable.RTChart_hAxisName)
        vScaleNum = typedArray.getInt(R.styleable.RTChart_vScaleNum, 10)
        hScaleNum = typedArray.getInt(R.styleable.RTChart_hScaleNum, 10)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = when {
            MeasureSpec.getSize(widthMeasureSpec) < 360 -> 360
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
        Message.obtain(mDrawHandler, MSG_QUIT).sendToTarget()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isDrawing = true
        mDrawThread = Thread(this)
        mDrawThread.start()
    }

    override fun run() {
        Looper.prepare()
        mDrawHandler = DrawHandler(Looper.myLooper()) { canvas: Canvas?, isDrawData: Boolean ->
            drawChart(
                canvas,
                isDrawData
            )
        }
        Looper.loop()
    }

    private fun drawChart(canvas: Canvas?, isDrawData: Boolean) {
        mDrawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas?.drawPaint(mDrawPaint)
        drawChartBg(canvas)
        drawAxis(canvas)
        drawAxisText(canvas)
        if (isDrawData) {
            drawDataLine(canvas)
        }
    }

    private fun drawAxisText(canvas: Canvas?) {
        changeTextPaint()
        val axisWidth = width * 15 / 16
        val axisHeigth = height * 8 / 9
        val scaleWidth = axisWidth.toFloat() / hScaleNum
        val scaleHeight = axisHeigth.toFloat() / vScaleNum
        val horizontalOffset = context.sp2px(8) / 2
//        canvas?.save()
//        canvas?.translate(horizontalOffset.toFloat(), axisHeigth.toFloat() + context.dp2px(16))
//        for (i in 0..hScaleNum) {
//            val scaleText = "${0.5 * (i + xOffsetIndex)}"
//            canvas?.drawText(scaleText, (scaleWidth * i).toFloat(), 0f, mDrawPaint)
//        }
//        canvas?.restore()
        canvas?.save()
        canvas?.translate(axisWidth.toFloat() + context.sp2px((scaleHeight/2).toInt()).toFloat() + horizontalOffset, 0f)
        mDrawPaint.textSize = context.sp2px((scaleHeight/2).toInt()).toFloat()
        for (i in vScaleNum downTo 0) {
            val scaleText = "${(minValue + (maxValue - minValue) * (vScaleNum - i) / vScaleNum)}"
            canvas?.drawText(scaleText, 0f, scaleHeight * i + context.sp2px(8), mDrawPaint)
        }
        canvas?.restore()
    }

    private fun drawDataLine(canvas: Canvas?) {
        val axisWidth = width * 15 / 16
        val axisHeigth = height * 8 / 9
        val scaleWidth = axisWidth.toFloat() / hScaleNum
        val scaleHeight = axisHeigth.toFloat() / vScaleNum
        val horizontalOffset = context.sp2px(8) / 2
        changeDataPaint()
        canvas?.save()
        canvas?.translate(horizontalOffset.toFloat(), horizontalOffset.toFloat() + axisHeigth)
        canvas?.scale(1f, -1f)
        for (i in 0..mDataDeque.size - 1) {
            val value = mDataDeque.get(i)
            val x = i * scaleWidth
            val y = (maxValue - value).toFloat() / maxValue * axisHeigth
            canvas?.drawPoint(x, y, mDrawPaint)
        }
        for (i in 1..mDataDeque.size - 1) {
            val value = mDataDeque.get(i)
            val lastValue = mDataDeque.get(i - 1)
            val x = i * scaleWidth
            val y = (maxValue - value).toFloat() / maxValue * axisHeigth
            val lastX = (i - 1) * scaleWidth
            val lastY = (maxValue - lastValue).toFloat() / maxValue * axisHeigth
            canvas?.drawLine(lastX, lastY, x, y, mDrawPaint)
        }
        canvas?.restore()
    }

    private fun drawAxis(canvas: Canvas?) {
        changeAxisPaint()
        val axisWidth = width * 15 / 16
        val axisHeigth = height * 8 / 9
        val scaleWidth = axisWidth.toFloat() / hScaleNum
        val scaleHeight = axisHeigth.toFloat() / vScaleNum
        val horizontalOffset = context.sp2px(8) / 2
        val verticalOffset = context.sp2px(8) / 2
        canvas?.save()
        canvas?.translate(horizontalOffset.toFloat(), verticalOffset.toFloat())
        for (i in 0..hScaleNum) {
            val x = (i * scaleWidth).toFloat()
            canvas?.drawLine(x, 0f, x, axisHeigth.toFloat(), mDrawPaint)
        }
        for (i in 0..vScaleNum) {
            val y = (i * scaleHeight).toFloat()
            canvas?.drawLine(0f, y, axisWidth.toFloat(), y, mDrawPaint)
        }
        canvas?.restore()
    }

    private fun drawChartBg(canvas: Canvas?) {
        changeBgPaint()

    }

    fun postDrawNewValue(value: Int) {
        if ((value > maxValue) or (value < minValue))
            throw IllegalArgumentException("Value must range from min to max.")
        Message.obtain(mDrawHandler, MSG_NOTIFY_DRAW, holder).sendToTarget()
        if (mDataDeque.size >= hScaleNum + 1) {
            mDataDeque.removeAt(0)
            xOffsetIndex++
            mDataDeque.add(value)
        } else {
            mDataDeque.add(value)
        }
    }

    fun postDrawInitialValue() {
        Message.obtain(mDrawHandler, MSG_INITIAL, holder).sendToTarget()
    }

    private fun changeBgPaint() {
        mDrawPaint.reset()
        mDrawPaint.color = Color.RED
    }

    private fun changeTextPaint() {
        mDrawPaint.reset()
        mDrawPaint.strokeWidth = context.dp2px(1).toFloat()
        mDrawPaint.color = Color.WHITE
        mDrawPaint.textSize = context.sp2px(8).toFloat()
        mDrawPaint.textAlign = Paint.Align.CENTER
    }

    private fun changeAxisPaint() {
        mDrawPaint.reset()
        mDrawPaint.strokeWidth = context.dp2px(1).toFloat()
        mDrawPaint.color = Color.GREEN
        mDrawPaint.alpha = 64
    }

    private fun changeDataPaint() {
        mDrawPaint.reset()
        mDrawPaint.strokeWidth = context.dp2px(1).toFloat()
        mDrawPaint.color = Color.YELLOW
    }


    private class DrawHandler(looper: Looper, drawWhat: (canvas: Canvas?, isDrawData: Boolean) -> Unit) :
        Handler(looper) {

        private val draw: (canvas: Canvas?, isDrawData: Boolean) -> Unit = drawWhat

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_INITIAL -> {
                    val holder = msg?.obj as? SurfaceHolder
                    val canvas = holder?.lockCanvas()
                    draw(canvas, false)
                    holder?.unlockCanvasAndPost(canvas)
                }
                MSG_NOTIFY_DRAW -> {
                    val holder: SurfaceHolder? = msg?.obj as? SurfaceHolder
                    val canvas = holder?.lockCanvas()
                    draw(canvas, true)
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

fun Context.sp2px(sp: Int): Int {
    val scaleDensity = resources.displayMetrics.scaledDensity
    return (sp * scaleDensity).toInt()
}