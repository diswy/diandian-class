package com.cqebd.live

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.alibaba.android.arouter.facade.annotation.Route
import com.open.net.client.impl.tcp.nio.NioClient
import com.open.net.client.structures.BaseClient
import com.open.net.client.structures.BaseMessageProcessor
import com.open.net.client.structures.IConnectListener
import com.open.net.client.structures.TcpAddress
import com.open.net.client.structures.message.Message
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset
import java.util.*

@Route(path = "/app/main")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val mClient = NioClient(object : BaseMessageProcessor() {
            override fun onReceiveMessages(mClient: BaseClient?, mQueen: LinkedList<Message>?) {
                mQueen?.forEach {
                    val s = String(it.data, it.offset, it.length,Charset.forName("GBK"))
                    Log.d("xiaofu", "接收消息：$s")
                    val s2 = StringUtils.getS(it.data, it.offset, it.length)
                    Log.d("xiaofu", "接收消息：$s2")
                }
            }

        }, object : IConnectListener {
            override fun onConnectionSuccess() {
                Log.d("xiaofu", "socket连接成功")

            }

            override fun onConnectionFailed() {
                Log.d("xiaofu", "socket连接失败")

            }
        })
        mClient.setConnectAddress(arrayOf(TcpAddress("192.168.1.117", 6000)))


        btn_connect.setOnClickListener {
            mClient.connect()
            Log.d("xiaofu", "socket连接状态：${mClient.isConnected}")
        }
    }
}
