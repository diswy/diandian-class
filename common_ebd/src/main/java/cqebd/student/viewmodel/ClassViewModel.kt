package cqebd.student.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cqebd.student.repository.ClassRepository
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import xiaofu.lib.tools.TimeFormat
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
        return Observable.interval(0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    currentTime.value = TimeFormat.clock(System.currentTimeMillis())
                }
    }
}