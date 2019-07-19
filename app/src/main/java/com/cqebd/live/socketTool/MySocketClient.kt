package com.cqebd.live.socketTool

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 线程安全的单例
 * Created by @author xiaofu on 2019/6/27.
 */
class MySocketClient private constructor() {
    companion object {
        private var instance: MySocketClient? = null
            get() {
                if (field == null) {
                    field = MySocketClient()
                }
                return field
            }

        @Synchronized
        fun get(): MySocketClient {
            return instance!!
        }
    }

    interface Socket {
        fun connectSuccess(socketOs: OutputStream, socketIs: InputStream)
    }

    // socket连接
    fun connect(ip: String, port: Int, callback: Socket): Disposable? {
        return Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({
                val socketAddress = InetSocketAddress(ip, port)
                val socket = Socket()
                socket.connect(socketAddress, 3000)
                Log.w("Socket连接", "Socket连接成功,连接ip：$ip,端口$port")
                val socketOs = socket.getOutputStream()
                val socketIs = socket.getInputStream()
                callback.connectSuccess(socketOs, socketIs)
                socketOs?.close()
                socketIs?.close()
                Log.w("Socket连接", "socket流已经关闭")
            }, {
                Log.e("Socket连接", "socket所在RX发生错误，错误内容：${it.message}")
            })
    }

}