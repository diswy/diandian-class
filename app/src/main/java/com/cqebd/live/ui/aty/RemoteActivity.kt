package com.cqebd.live.ui.aty

import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityRemoteBinding
import com.cqebd.live.socketTool.KTool
import com.cqebd.live.socketTool.MySocketClient
import com.cqebd.live.socketTool.SocketTool
import com.jeremyliao.liveeventbus.LiveEventBus
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.tools.ByteTools
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import xiaofu.lib.inline.loadUrl
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * 远程桌面，显示PC端内容
 */
@Route(path = "/app/aty/remote")
class RemoteActivity : BaseBindActivity<ActivityRemoteBinding>() {

    private val cmd = KTool.getByte()// 收到此命令才开始收图

    private var isContinue = true// 是否保持远程桌面连接

    private val observer = Observer<String> {
        when (it) {
            Command.BROADCAST_STOP -> {
                isContinue = false
                finish()
            }
        }
    }

    override fun isFullScreen(): Boolean = true
    override fun isKeepScreenOn(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_remote

    override fun initialize(binding: ActivityRemoteBinding) {

        LiveEventBus.get()
            .with(Command.COMMAND, String::class.java)
            .observe(this, observer)


        val directory = File(Environment.getExternalStorageDirectory().absolutePath + "/picpic")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        SocketTool.connect("",Environment.getExternalStorageDirectory().absolutePath,this,binding.ivRemote)

//        val cache = ACache.get(this)
//        val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
//        val disposable = MySocketClient.get().connect(ip, 2021, object : MySocketClient.Socket {
//            override fun connectSuccess(socketOs: OutputStream, socketIs: InputStream) {
//                readImageAndShow(socketOs, socketIs)
//            }
//        }) ?: return
//        mDisposablePool.add(disposable)
    }

    private fun readImageAndShow(socketOs: OutputStream, socketIs: InputStream) {
        Log.w("远程桌面", "远程桌面连接成功，并发送命令")
        socketOs.write(cmd)
        socketOs.flush()
        while (isContinue) {
            Log.w("远程桌面", "远程桌面连接成功，并发送命令")
            val cmdBuffer = ByteArray(24)
            var read = 0
            read = socketIs.read(cmdBuffer, 0, 24)// 读取24个字节
            val receiveCommand = String(cmdBuffer, 0, 16, Charset.forName("UTF-8")).trim()
            val lenByte = ByteArray(4)// 读取4字节 图片大小
            System.arraycopy(cmdBuffer, 16, lenByte, 0, 4)
            val nameByte = ByteArray(4)// 读取4字节 图片名字
            System.arraycopy(cmdBuffer, 20, nameByte, 0, 4)
            val fileSize = ByteTools.bytes2Int(lenByte)
            val name = ByteTools.bytes2Int(nameByte)
            if (receiveCommand == Command.SCREENS_RESPONSE) {
                Log.w("远程桌面", "收到一张远程图片：图片大小:$fileSize;图片名称:$name")
                var remaining = fileSize//待接收长度
                read = 0

                val imgFile = File(Environment.getExternalStorageDirectory().absolutePath + "/picpic/" + name + ".png")
                var fos: FileOutputStream? = FileOutputStream(imgFile)

//                var imgByteArray = ByteArray(0)
                val imgBuffer = ByteArray(4096)
                while (socketIs.read(imgBuffer, 0, Math.min(imgBuffer.size, remaining)).apply {
                        read = this
                    } > 0) {
                    remaining -= read
                    fos?.write(imgBuffer)
//                    imgByteArray += imgBuffer
                }
                fos?.flush()
                fos?.close()
                fos = null
                Log.w("远程桌面", "图片全部接收完毕，准备显示")
//                val bitmap = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)
//                val disposable = Flowable.just(Environment.getExternalStorageDirectory().absolutePath + "/picpic/" + name + ".png")
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe({
//                        binding.ivRemote.loadUrl(this, it)
//                    }, {
//                        Log.w("远程桌面", "显示图片发生了错误，错误内容：${it.message}")
//                    })
            }
        }
    }
}
