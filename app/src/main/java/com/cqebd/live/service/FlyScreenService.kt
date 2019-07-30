package com.cqebd.live.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import com.cqebd.live.socketTool.KTool
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.runOnUiThread
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 学生飞屏演示
 */
class FlyScreenService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("xiaofu", "飞屏服务启动")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("xiaofu", "飞屏服务停止")
    }


    companion object {
        /**
         * 本地抓屏
         */
        private var mSocketOs: OutputStream? = null
        private var mSocketIs: InputStream? = null
        private val imgBuffer = ByteArray(4096)// 图片缓存区
        private var mShareHotCount = 0
        private lateinit var mediaProjection: MediaProjection
        /**
         * 连接服务器
         */
        fun connectServer(ctx: Context, ip: String, mediaProjection: MediaProjection) {
            Log.e("屏幕分享","进入这方法了")
            Thread(Runnable {
                try {
                    val socketAddress = InetSocketAddress(ip, 2022)
                    val socket = Socket()
                    socket.connect(socketAddress, 1000)
                    mSocketOs = socket.getOutputStream()
                    mSocketIs = socket.getInputStream()
                    Log.e("屏幕分享","连接成功")
                    this.mediaProjection = mediaProjection
                    ctx.runOnUiThread {
                        startScreenShot(ctx, mediaProjection)
                    }
                } catch (e: Exception) {
                    Log.e("屏幕分享","socket错误："+e.message)
                }
            }).start()
        }

        private fun startScreenShot(ctx: Context, mediaProjection: MediaProjection) {
            Log.e("屏幕分享","开始截屏")
            val metrics = ctx.resources.displayMetrics
            val imageReader = ImageReader.newInstance(
                    metrics.widthPixels,
                    metrics.heightPixels,
                    PixelFormat.RGBA_8888,
                    2)
            mediaProjection.createVirtualDisplay(
                    "screen_short",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null)

            imageReader.setOnImageAvailableListener({ reader ->
                var image: Image? = null
                var fos: FileOutputStream? = null
                var bitmap: Bitmap? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        Log.e("屏幕分享", "截图-------------")
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * metrics.widthPixels
                        bitmap = Bitmap.createBitmap(
                                metrics.widthPixels + rowPadding / pixelStride,
                                metrics.heightPixels, Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        val fileName = KTool.getScreenShortPath()
                        fos = FileOutputStream(fileName)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                        fos.flush()
                        val disposable = Flowable.just(1)
                                .subscribeOn(Schedulers.io())
                                .subscribe({
                                    mSocketOs?.let { os ->
                                        val file = File(fileName)
                                        val fileInputOs = FileInputStream(file)
                                        val fileSize = file.length().toInt()

                                        var read = 0
                                        var totalRead = 0
                                        var remaining = fileSize
                                        Log.e("屏幕分享", "文件大小：$fileSize")
                                        while (totalRead < fileSize && fileInputOs.read(
                                                        imgBuffer,
                                                        0,
                                                        Math.min(imgBuffer.size, remaining)
                                                ).apply {
                                                    read = this
                                                } > 0
                                        ) {
                                            Log.e("屏幕分享", "开始发送文件")
                                            os.write(KTool.getSendByteShareHot(fileSize, mShareHotCount, read))
                                            os.flush()
                                            os.write(imgBuffer, 0, read)
                                            os.flush()
                                            totalRead += read
                                            remaining -= read
                                        }
                                        Log.e("屏幕分享", "发送完毕~")
                                        mShareHotCount++
                                    }
                                }, {
                                    Log.e("屏幕分享", "RX错误${it.message}")
                                }, {
                                    try {
                                        fos.close()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                })
                    }
                } catch (e: Exception) {
                    Log.e("屏幕分享", "发生错误，错误内容：${e.message}")
                } finally {
                    bitmap?.recycle()
                    image?.close()
                }
            }, null)
        }

        fun stopScreenShot() {
            Log.e("屏幕分享", "服务停止")
            if (::mediaProjection.isInitialized) {
                mediaProjection.stop()
            }
        }
    }

}
