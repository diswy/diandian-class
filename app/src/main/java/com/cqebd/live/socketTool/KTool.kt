package com.cqebd.live.socketTool

import android.os.Environment
import android.util.Log
import com.cqebd.live.TCommand
import com.cqebd.live.TImg
import cqebd.student.commandline.Command
import cqebd.student.tools.ByteTools

/**
 *
 * Created by @author xiaofu on 2019/6/26.
 */
object KTool {

    fun getSDPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath + "/diandian/class"
    }

    fun getPCRemotePath(count: Int): String {
        return Environment.getExternalStorageDirectory().absolutePath + "/diandian/class/pc_screen_remote$count.png"
    }

    fun getScreenShortPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath + "/diandian/class/screen_short.png"
    }

    fun getFilePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath + "/diandian/receive/"
    }


    fun getByte(): ByteArray {
        val cmdRequest = String.format("%-16s", Command.SCREENS_REQUEST).toByteArray()
        val cmdEnd = ByteTools.intToByteArray(0)
        return cmdRequest + cmdEnd
    }

    fun getSendByte(length: Int, count: Int): ByteArray {
        val cmdRequest = String.format("%-16s", Command.SHARE_HOT).toByteArray()
        val cmdEnd = ByteTools.Int2Bytes_LE(length)
        val name = ByteTools.Int2Bytes_LE(count)
        return cmdRequest + cmdEnd + name
    }

    /**
     * @param length 文件大小
     * @param count 文件名称-序号
     * @param currentSize 当前长度
     */
    fun getSendByteShareHot(length: Int, count: Int, currentSize: Int): ByteArray {
        val cmdRequest = String.format("%-16s", Command.SHARE_HOT).toByteArray()
        val cmdEnd = ByteTools.Int2Bytes_LE(length)
        val name = ByteTools.Int2Bytes_LE(count)
        val current = ByteTools.Int2Bytes_LE(currentSize)
        return cmdRequest + name + cmdEnd + current
    }


    fun outputFile(cmdList: List<TCommand>, imgList: List<TImg>) {
//        cmdList.sortedBy {
//            it.pos
//        }
//        imgList.sortedBy {
//            it.picPos
//        }



    }
}