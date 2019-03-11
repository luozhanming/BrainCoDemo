package cn.com.ava.braincodemo

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class MainActivity : AppCompatActivity() {

    private lateinit var mChart: RTChart
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var mListView: RecyclerView

    private val mApi: HttpApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpInterceptor())
            .build()
        Retrofit.Builder()
            .baseUrl("http://192.168.6.181/")
            .client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(HttpApi::class.java)
    }

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
                Toast.makeText(this@MainActivity, "$it", Toast.LENGTH_SHORT).show()
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
            headband?.setOnAttentionListener(object : OnAttentionChangedListenerEx(headband) {
                override fun onAttention(p0: Double) {
                    //   mChart.postDrawNewValue(p0.toInt())
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

    private lateinit var timeDisposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mChart = findViewById(R.id.chart)
        mListView = findViewById(R.id.recyclerview)
        refreshLayout = findViewById(R.id.refreshLayout)
        mListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mListView.adapter = mHeadBandAdapter
        findViewById<Button>(R.id.btn_connect).setOnClickListener {
            timeDisposable = Observable.interval(5, TimeUnit.SECONDS)
                .map {
                    val studentDataStr = arrayListOf<String>()
                    val studentAverage = arrayListOf<Int>()
                    var index = 1
                    mHeadBandMap.forEach {
                        val band = it.key
                        val value = it.value
                        //计算单个学生的5秒内数据的专注度平均值
                        val average = value.asSequence().map { it.attention.times(100) }.toList().average().toInt()
                        if (average > 0) {
                            //数据正常保留
                            val studentData =
                                "{${index++},${average.toInt()},${(band.batteryLevel?.times(100)?.toInt())
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
                    val sendHead =
                        "BrainWave_InputDevData_${it}_${studentAverage.size}_${studentAverage.asSequence().filter { it != 0 }
                            .map { item ->
                                item
                            }
                            .toList().average().toString().replaceAfter(".", "").replace(".", "")}_"
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
                        mApi.commandHttpApi(map)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
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
            if (!timeDisposable?.isDisposed) {
                timeDisposable.dispose()
            }
        }
        refreshHeadbandList()

    }

    override fun onStart() {
        super.onStart()
        mChart.post {
            mChart.postDrawInitialValue()
        }
        refreshLayout.setOnRefreshListener {
            refreshHeadbandList()
        }
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
                Toast.makeText(this, fusiHeadbandError.message, Toast.LENGTH_SHORT).show()
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
                DeviceConnectionState.CONNECTED -> "Connected"
                DeviceConnectionState.CONNECTING -> "Connecting"
                DeviceContactState.CHECKING_CONTACT_STATE -> "Checking"
                DeviceConnectionState.DISCONNECTED -> "Disconnected"
                else -> "N/A"
            }
            if (fusiHeadband.connectionState == DeviceConnectionState.CONNECTED) {
                p0.contact.text = when (fusiHeadband.deviceContactState) {
                    DeviceContactState.CONTACT -> "Contact"
                    DeviceContactState.CHECKING_CONTACT_STATE -> "Checking"
                    DeviceContactState.NO_CONTACT -> "NoContact"
                    else -> "Unknown"
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

            val attention: TextView by lazy {
                itemView.findViewById(R.id.attention) as TextView
            }

        }
    }


}
