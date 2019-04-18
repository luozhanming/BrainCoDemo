package cn.com.ava.braincodemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class StatementActivity:AppCompatActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setContentView(R.layout.activity_statement)
        findViewById<TextView>(R.id.tv_statement)
            .setText("使用步骤\n" +
                    "1.将平板连接到BrainCo_Demo的wifi中\n" +
                    "2.点击右上角菜单的\"录播设置\"设置录播的ip号和端口;\n" +
                    "3.录播开启后，网页导播进入录播录课界面\n" +
                    "4.下拉刷新出接入wifi的头环\n" +
                    "5.点击 全部连接 按钮以连接平板和所有头环通信，也可以单独点击头环列表上的头环改变其连接状态\n" +
                    "6.依次点击 开启->输出->开始传输 按钮 ，至此可在录播界面看到专注度曲线图\n" +
                    "7.使用完毕后，点击 关闭 按钮\n" +
                    "8.全部断开 按钮可按可不按\n\n" +
                    "按钮说明\n" +
                    "开始传输：点击后平板开始向录播传输专注度数据\n" +
                    "全部连接：连接所有头环设备\n" +
                    "断开连接：断开所有头环连接\n" +
                    "开启：开启录播专注度计算功能\n" +
                    "关闭：关闭录播专注度计算功能\n" +
                    "输出：将专注度曲线图输出到主屏幕")
    }
}