package cqebd.student.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cqebd.student.service.ClassService

/**
 *
 * Created by @author xiaofu on 2019/6/6.
 */
class WifiBroadcastReceiver : BroadcastReceiver() {

    companion object {
        fun isServiceRunning(ctx: Context, serviceName: String): Boolean {
            if (serviceName == "") {
                return false
            }

            val manager: ActivityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningService = manager.getRunningServices(Integer.MAX_VALUE)
            runningService.forEach {
                if (it.service.className.toString() == serviceName) {
                    return true
                }
            }
            return false
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val isServiceStarted = isServiceRunning(it, "cqebd.student.service.ClassService")
            Log.d("xiaofu", "收到广播::::::$isServiceStarted")
            if (!isServiceStarted) {
                it.startService(Intent(it, ClassService::class.java))
            }
        }

    }


}