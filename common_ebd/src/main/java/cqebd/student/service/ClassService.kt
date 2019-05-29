package cqebd.student.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.alibaba.android.arouter.launcher.ARouter
import com.open.net.client.impl.tcp.nio.NioClient
import com.open.net.client.structures.BaseClient
import com.open.net.client.structures.BaseMessageProcessor
import com.open.net.client.structures.IConnectListener
import com.open.net.client.structures.TcpAddress
import com.open.net.client.structures.message.Message
import com.orhanobut.logger.Logger
import org.jetbrains.anko.startActivity
import java.nio.charset.Charset
import java.util.*

class ClassService : Service() {

    override fun onCreate() {
        super.onCreate()
        Logger.d("课堂Service，onCreate")


        val mClient = NioClient(object : BaseMessageProcessor() {
            override fun onReceiveMessages(mClient: BaseClient?, mQueen: LinkedList<Message>?) {
                mQueen?.forEach {
                    val s = String(it.data, it.offset, it.length, Charset.forName("GBK"))
                    Log.d("xiaofu", "接收消息：$s")
//                    val s2 = StringUtils.getS(it.data, it.offset, it.length)
//                    Log.d("xiaofu", "接收消息：$s2")

                    if (s == "111"){
                        ARouter.getInstance()
                                .build("/app/race")
                                .navigation()
                    }

                    if (s == "222"){
                        ARouter.getInstance()
                                .build("/app/main")
                                .navigation()
                    }

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
        mClient.connect()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("课堂Service，onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("课堂Service，onDestroy")
    }

    override fun onBind(intent: Intent): IBinder? {
        Logger.d("课堂Service，onBind")
        return null
    }
}
