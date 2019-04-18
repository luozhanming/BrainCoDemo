package cn.com.ava.braincodemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import tech.brainco.fusi.sdk.DeviceConnectionState
import tech.brainco.fusi.sdk.DeviceContactState
import tech.brainco.fusi.sdk.FusiHeadband
import tech.brainco.fusi.sdk.FusiSDK
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    /**
     * 录播是否输出专注度图
     * */
    var isOutput: Boolean = false

    var isSending:Boolean = false

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_open -> {
                val map = LinkedHashMap<String, String>()
                map.put("action", "6")
                map.put("user", "admin")
                map.put("pswd", "21232f297a57a5a743894a0e4a801fc3")
                map.put("command", "1")
                map.put("data", "BrainWave_open")
                mApi?.commandHttpApi(map)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ t ->

                    }, { t ->
                        t.printStackTrace()
                    })
            }
            R.id.btn_close -> {
                val map = LinkedHashMap<String, String>()
                map.put("action", "6")
                map.put("user", "admin")
                map.put("pswd", "21232f297a57a5a743894a0e4a801fc3")
                map.put("command", "1")
                map.put("data", "BrainWave_close")
                mApi?.commandHttpApi(map)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ t ->
                        isOutput = false
                        var btnOutput = findViewById<Button>(R.id.btn_output)
                        btnOutput.setText("输出")
                        btnOutput.isEnabled = true
                        timeDisposable?.dispose()
                        isSending = false
                        val btnConnect = findViewById<Button>(R.id.btn_connect)
                        btnConnect.isEnabled = true
                        btnConnect.setText("开始传输")
                    }, { t ->
                        t.printStackTrace()
                    })
            }
            R.id.btn_output -> {
                findViewById<Button>(R.id.btn_output).isEnabled = false
                val map = LinkedHashMap<String, String>()
                map.put("action", "6")
                map.put("user", "admin")
                map.put("pswd", "21232f297a57a5a743894a0e4a801fc3")
                map.put("command", "1")
                map.put("data", "BrainWave_DevOutput")
                mApi?.commandHttpApi(map)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ t ->
                        isOutput = true
                        var btnOutput = findViewById<Button>(R.id.btn_output)
                        btnOutput.setText("已输出")
                        btnOutput.isEnabled = false
                    }, { t ->
                        findViewById<Button>(R.id.btn_output).isEnabled = true
                        t.printStackTrace()
                    })
            }
            R.id.btn_allconnect->{
                val data = mHeadBandAdapter.mDatas
                data.forEach {item->
                    //判断是否map中是否存在此headband
                    val headband = FusiHeadband.createFusiHeadband(item.mac, item.name, item.ip)
                    if (!mHeadBandMap.any { it.key.mac.equals(item.mac) }) {
                        mHeadBandMap.put(headband, arrayListOf())
                    }
                    if(item.connectionState!=DeviceConnectionState.CONNECTED){
                        item.connect { mHeadBandAdapter.notifyDataSetChanged() }
                    }
                    headband?.setOnContactStateChangeListener(object : OnContactChangedListenerEx(headband) {
                        override fun onContactStateChange(it: Int) {
                            mHeadBandAdapter.notifyDataSetChanged()
                            when (it) {
                                DeviceContactState.CONTACT -> {
                                    this.headband.setForeheadLEDLight(255, 255, 255)
                                }
                                DeviceContactState.CHECKING_CONTACT_STATE -> {
                                    this.headband.setForeheadLEDLight(0, 0, 255)
                                }
                                DeviceContactState.NO_CONTACT -> {
                                    this.headband.setForeheadLEDLight(255, 0, 0)
                                }
                            }
                        }
                    })
                    headband.setOnAttentionListener(object : OnAttentionChangedListenerEx(headband) {
                        override fun onAttention(p0: Double) {
                            //  mChart.postDrawNewValue(p0.toInt())
                            val attentions = mHeadBandMap.getOrElse(this.headband) {
                                null
                            }
                            //记录数据
                            attentions?.add(HeadBandAttention(System.currentTimeMillis(), this.headband, p0))
                            Log.i("${headband.ip}", "$p0")
                        }
                    })
                }
            }
        }

    }

    //private lateinit var mChart: RTChart
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var mListView: RecyclerView

    private var mApi: HttpApi? = null

    // private var mHeadBand: FusiHeadband? = null
    //保存5秒内头环专注度数据
    private val mHeadBandMap: MutableMap<FusiHeadband, MutableList<HeadBandAttention>> by lazy {
        hashMapOf<FusiHeadband, MutableList<HeadBandAttention>>()
    }
    private val mHeadBandAdapter: BrainCoAdapter by lazy {
        BrainCoAdapter(arrayListOf()) {

            val headband = FusiHeadband.createFusiHeadband(it.mac, it.name, it.ip)
            //判断是否map中是否存在此headband
            if (!mHeadBandMap.any { it.key.mac.equals(headband.mac) }) {
                mHeadBandMap.put(headband, arrayListOf())
            }
            when (headband?.connectionState) {
                //
                DeviceConnectionState.CONNECTED or DeviceConnectionState.CONNECTING -> {
                    it.setForeheadLEDLight(0, 0, 0)
                    it.disconnect()
                    return@BrainCoAdapter
                }
                DeviceConnectionState.DISCONNECTED -> {
                }
            }
            headband?.connect {
             //   Toast.makeText(this@MainActivity, "$it", Toast.LENGTH_SHORT).show()
                mHeadBandAdapter.notifyDataSetChanged()
            }
            headband?.setOnOrientationChangeListener {
                //                Toast.makeText(this@MainActivity,"${when(it){
//                    DeviceOrientation.BOTTOM_UP->"Headband Oreintation Bottom Up"
//                    DeviceOrientation.LEFT_ARM_END_DOWN->"Headband Oreintation Left Arm End Down"
//                    DeviceOrientation.LEFT_ARM_END_UP->"Headband Oreintation Left Arm End Up"
//                    DeviceOrientation.TOP_UP->"Headband Oreintation Top Up"
//                    DeviceOrientation.LEFT_FACE_UP->"Headband Oreintation Face Up"
//                    DeviceOrientation.LEFT_FACE_DOWN->"Headband Oreintation Face Down"
//                    else->"Headband Oreintation UNKNOWN"
//                }
//                }",Toast.LENGTH_SHORT).show()
            }
            headband?.setOnContactStateChangeListener(object : OnContactChangedListenerEx(headband) {
                override fun onContactStateChange(it: Int) {
                    mHeadBandAdapter.notifyDataSetChanged()
                    when (it) {
                        DeviceContactState.CONTACT -> {
                            this.headband.setForeheadLEDLight(255, 255, 255)
                        }
                        DeviceContactState.CHECKING_CONTACT_STATE -> {
                            this.headband.setForeheadLEDLight(0, 0, 255)
                        }
                        DeviceContactState.NO_CONTACT -> {
                            this.headband.setForeheadLEDLight(255, 0, 0)
                        }
                    }
                }

            })
            headband.setOnAttentionListener(object : OnAttentionChangedListenerEx(headband) {
                override fun onAttention(p0: Double) {
                    //  mChart.postDrawNewValue(p0.toInt())
                    val attentions = mHeadBandMap.getOrElse(this.headband) {
                        null
                    }
                    //记录数据
                    attentions?.add(HeadBandAttention(System.currentTimeMillis(), this.headband, p0))
                    Log.i("${headband.ip}", "$p0")
                }
            })
        }
    }

    private var timeDisposable: Disposable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //  mChart = findViewById(R.id.chart)
        mListView = findViewById(R.id.recyclerview)
        refreshLayout = findViewById(R.id.refreshLayout)
        mListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mListView.adapter = mHeadBandAdapter
        findViewById<Button>(R.id.btn_connect).setOnClickListener {
            isSending = true
            val btnConnect = findViewById<Button>(R.id.btn_connect)
            btnConnect.isEnabled = false
            btnConnect.setText("正在传输")
            timeDisposable = Observable.interval(15, TimeUnit.SECONDS)
                .map {
                    val studentDataStr = arrayListOf<String>()
                    val studentAverage = arrayListOf<Int>()
                    var index = 1
                    mHeadBandMap.forEach {
                        val band = it.key
                        val value = it.value
                        //计算单个学生的5秒内数据的专注度平均值
                        val average =
                            value.asSequence().map { 100 * it.attention }.toList().average()
                                .toInt()
                        if (average > 0) {
                            //数据正常保留
                            val studentData =
                                "{${index++},${average},${(band.batteryLevel?.times(100)?.toInt())
                                    ?: 0},${band.mac},${band.ip},${band.name.replace("_", "-")}}"
                            studentDataStr.add(studentData)
                            studentAverage.add(average)
                        } else {
                            //数据异常置0
                            val studentData =
                                "{${index++},0,${(band.batteryLevel?.times(100)?.toInt())
                                    ?: 0},${band.mac},${band.ip},${band.name.replace("_", "-")}}"
                            studentDataStr.add(studentData)
                            studentAverage.add(0)
                        }
                    }
                    val buffer = StringBuffer()
                    //指令头
                    var sendAverage = studentAverage.average()
                    if (sendAverage >= 0) {
                        sendAverage = studentAverage.filter { it > 0 }.average()
                    } else {
                        sendAverage = 0.0
                    }
                    val sendHead =
                        "BrainWave_InputDevData_${it}_${studentAverage.size}_${if (studentAverage.size > 0) {
                            sendAverage
                        } else {
                            0
                        }}"
                    buffer.append(sendHead)
                    for (data in studentDataStr) {
                        buffer.append("_$data")
                    }
                    Log.i("send Data", buffer.toString())
                    buffer.toString()  //要发送的数据
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Logger.i("MainActivity", "send Data: $it")
                        val map = LinkedHashMap<String, String>()
                        map.put("action", "6")
                        map.put("user", "admin")
                        map.put("pswd", "21232f297a57a5a743894a0e4a801fc3")
                        map.put("command", "1")
                        map.put("data", it)
                        mApi?.commandHttpApi(map)
                            ?.subscribeOn(Schedulers.io())
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.subscribe({
                                Log.i("MainActivity", "response data ${it.string()}")
                                mHeadBandMap.forEach { it.value.clear() }
                            },
                                { it.printStackTrace() })
                    },
                    { it.printStackTrace() }
                )
        }
        findViewById<Button>(R.id.btn_disconnect).setOnClickListener {
            mHeadBandAdapter.disconnectAllband()
            if (timeDisposable == null) {
                return@setOnClickListener
            } else {
                if (false.equals(timeDisposable?.isDisposed)) {
                    timeDisposable?.dispose()
                    isSending = false
                    val btnConnect = findViewById<Button>(R.id.btn_connect)
                    btnConnect.isEnabled = true
                    btnConnect.setText("开始传输")
                }
            }
        }
        refreshHeadbandList()
        findViewById<Button>(R.id.btn_allconnect).setOnClickListener(this)
        findViewById<Button>(R.id.btn_open).setOnClickListener(this)
        findViewById<Button>(R.id.btn_close).setOnClickListener(this)
        findViewById<Button>(R.id.btn_output).setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        refreshLayout.setOnRefreshListener {
            refreshHeadbandList()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpInterceptor())
            .build()
        val sp = getSharedPreferences("http", Context.MODE_PRIVATE)
        val ip = sp.getString("ip", "xxx.xxx.xxx.xxx")
        val host = sp.getString("host", "80")
        mApi = Retrofit.Builder()
            .baseUrl("http://${ip}:${host}/")
            .client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(
                HttpApi::
                class.java
            )
        findViewById<TextView>(R.id.tv_iphost).setText("录播地址：http://${ip}:${host}")


    }

    private fun refreshHeadbandList() {
        FusiSDK.searchHeadbands({ list ->
            refreshLayout.isRefreshing = false
            if (list.size == 0) {
                mHeadBandAdapter.mDatas = list
                Toast.makeText(this@MainActivity, "搜不到头环", Toast.LENGTH_SHORT).show()
            } else {
                mHeadBandAdapter.mDatas = list
            }
        },
            { fusiHeadbandError ->
                refreshLayout.isRefreshing = false
           //     Toast.makeText(this, fusiHeadbandError.message, Toast.LENGTH_SHORT).show()
            })
    }

    class BrainCoAdapter(val datas: MutableList<FusiHeadband>, val clickCallback: ((FusiHeadband) -> Unit)?) :
        RecyclerView.Adapter<BrainCoAdapter.ViewHolder>() {

        var mDatas: List<FusiHeadband>
            get() = datas
            set(value) {
                datas.clear()
                datas.addAll(value)
                notifyDataSetChanged()
            }


        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
            val root = LayoutInflater.from(p0.context).inflate(R.layout.item_headband, p0, false)
            return ViewHolder(root)
        }

        override fun getItemCount(): Int = datas.size

        override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
            val fusiHeadband = datas.get(p1)
            p0.mac.text = fusiHeadband.mac
            p0.ip.text = fusiHeadband.ip
            p0.name.text = fusiHeadband.name
            p0.conntect.text = when (fusiHeadband.connectionState) {
                DeviceConnectionState.CONNECTED -> "已连接"
                DeviceConnectionState.CONNECTING -> "连接中"
                DeviceConnectionState.DISCONNECTED -> "未连接"
                else -> "N/A"
            }
            if (fusiHeadband.connectionState == DeviceConnectionState.CONNECTED) {
                p0.contact.text = when (fusiHeadband.deviceContactState) {
                    DeviceContactState.CONTACT -> "已接触传感器"
                    DeviceContactState.CHECKING_CONTACT_STATE -> "检测接触状态"
                    DeviceContactState.NO_CONTACT -> "未接触传感器"
                    else -> "未知"
                }
            }

            if (fusiHeadband.connectionState == DeviceConnectionState.CONNECTED) {
                //     p0.attention.text = "${(fusiHeadband.attention)}"
            }

            p0.itemView.setOnClickListener {
                clickCallback?.invoke(fusiHeadband)
            }
        }

        fun connectAllband() {
            mDatas.filter { it.connectionState != DeviceConnectionState.CONNECTED }
                .forEach { it.connect { notifyDataSetChanged() } }
        }

        fun disconnectAllband() {
            mDatas.filter { it.connectionState != DeviceConnectionState.DISCONNECTED }
                .forEach { it.disconnect() }
        }


        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val mac: TextView by lazy {
                itemView.findViewById(R.id.mac) as TextView
            }
            val name: TextView by lazy {
                itemView.findViewById(R.id.name) as TextView
            }
            val ip: TextView by lazy {
                itemView.findViewById(R.id.ip) as TextView
            }
            val conntect: TextView by lazy {
                itemView.findViewById(R.id.connect) as TextView
            }

            val contact: TextView by lazy {
                itemView.findViewById(R.id.contact) as TextView
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.item_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if (item?.itemId == R.id.setting) {
            //恢复输出
            isOutput = false
            var btnOutput = findViewById<Button>(R.id.btn_output)
            btnOutput.setText("输出")
            btnOutput.isEnabled = true

            val intent = Intent(this, HttpSettingActivity::class.java)
            startActivity(intent)
            findViewById<Button>(R.id.btn_disconnect).performClick()
            timeDisposable?.dispose()
            isSending = false
            val btnConnect = findViewById<Button>(R.id.btn_connect)
            btnConnect.isEnabled = true
            btnConnect.setText("开始传输")
            val map = LinkedHashMap<String, String>()
            map.put("action", "6")
            map.put("user", "admin")
            map.put("pswd", "21232f297a57a5a743894a0e4a801fc3")
            map.put("command", "1")
            map.put("data", "BrainWave_close")
            mApi?.commandHttpApi(map)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({ t ->
                }, { t ->
                    t.printStackTrace()
                })
        }else if(item?.itemId==R.id.statement){
            val intent = Intent(this,StatementActivity::class.java)
            startActivity(intent)
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        // timeDisposable?.dispose()
    }


}
