package cqebd.student.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cqebd.student.repository.ClassRepository
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import xiaofu.lib.tools.TimeFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 *
 * Created by @author xiaofu on 2019/6/5.
 */

class ClassViewModel @Inject constructor(private val classRepository: ClassRepository) : ViewModel() {

    private val currentTime = MutableLiveData<String>()

    fun getTime() = currentTime

    fun startTime(): Disposable {
        return Observable.interval(0, 1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    currentTime.value = TimeFormat.clock(System.currentTimeMillis())
                }
    }

    fun getStringData(): String {
        val c = Calendar.getInstance()
        c.timeZone = TimeZone.getTimeZone("GMT+8:00")
        val month = c.get(Calendar.MONTH) + 1
        val day = c.get(Calendar.DAY_OF_MONTH)
        val week = when (c.get(Calendar.DAY_OF_WEEK)) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            7 -> "日"
            else -> "一"
        }

        val format = "%d月%d日 周%s"
        return format.format(month, day, week)
    }
}