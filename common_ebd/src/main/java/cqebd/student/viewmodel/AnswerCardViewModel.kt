package cqebd.student.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cqebd.student.repository.AnswerCardRepository
import cqebd.student.vo.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import xiaofu.lib.AppExecutors
import xiaofu.lib.cache.ACache
import xiaofu.lib.utils.fromJsonArray
import java.io.File
import javax.inject.Inject

/**
 * 开始答题，以及答案的各种事务处理
 * Created by @author xiaofu on 2019/3/21.
 */
class AnswerCardViewModel @Inject constructor(
    private val appExecutors: xiaofu.lib.AppExecutors,
    private val answerCardRepository: AnswerCardRepository,
    private val gson: Gson,
    private val cache: xiaofu.lib.cache.ACache
) : ViewModel() {

    companion object {
        const val END_WORK = -999
    }

    fun getUserId() = answerCardRepository.getUserId()

    private val mCardStatus = MutableLiveData<Boolean>()// 答题卡展示状态

    /**
     * @param isExpand true展开，false折叠
     */
    fun setCardStatus(isExpand: Boolean) {
        mCardStatus.value = isExpand
    }

    fun getCardStatus() = mCardStatus

    /**
     * 开始答题任务
     */
    fun startWork(taskId: Long, paperId: Long) = answerCardRepository.startWork(taskId, paperId)

    /**
     * 学生本地答案同步
     * [startWork]可获取题目答案选项，以及保存的服务器答案
     * 同步规则；
     * 服务器有答案的以为服务器为准
     * 服务器无答案以本地答案为准
     * 服务器本地都无答案则为未做的答案
     */
    private val finalAnswer = MediatorLiveData<StudentLocalAnswer>()// 合并过后的答案

    fun getFinalAnswer() = finalAnswer

    /**
     * 同步并合并答案,只需要进行一次
     * warning:不要乱动此方法！！！嵌套地狱！！！
     * @param taskId 唯一标识
     * @param remoteList 根据服务器的答案处理过后生成的答案列表
     */
    fun syncAndMergeAnswer(taskId: Long, remoteList: ArrayList<String>) {
        val remoteAnswer = StudentLocalAnswer(answerCardRepository.getUserId() + taskId, gson.toJson(remoteList))// 远程答案
        appExecutors.diskIO().execute {
            val localAnswer: StudentLocalAnswer? = answerCardRepository.onlyLoad(taskId)
            if (localAnswer == null) {
                answerCardRepository.insertStudentAnswer(remoteAnswer)
            } else {// 本地答案不为空，按规则走,仅合并一次
                val tempFinal = ArrayList<String>()// 储存最终答案
                try {
                    val mLocalAnswer = gson.fromJsonArray(localAnswer.answerList, String::class.java)// 本地答案
                    for (i in remoteList.indices) {// 第一层循环，取出每一题号
                        // 取出远程答案所有填空的值
                        val tempRemote = gson.fromJsonArray(remoteList[i], StudentAnswer::class.java)
                        // 取出本地答案所有填空的值
                        val tempLocal = gson.fromJsonArray(mLocalAnswer[i], StudentAnswer::class.java)
                        // 储存每一道题的最终答案
                        val tempFinalQuestion = ArrayList<StudentAnswer>()

                        if (tempRemote.size == tempLocal.size) {// 检查数组长度,理论上本地与远程答案的长度相等
                            for (j in tempRemote.indices) {// 第二层循环遍历
                                if (tempRemote[j].Answer == tempLocal[j].Answer) {// 本地和远程答案相同，随便添加一个即可
                                    tempFinalQuestion.add(tempRemote[j])
                                } else if (!TextUtils.isEmpty(tempRemote[j].Answer)) {// 远程答案不为空，此时以远程服务器为主
                                    tempFinalQuestion.add(tempRemote[j])
                                } else {// 本地和远程答案不同、且远程答案为空，本地数据不为空
                                    tempFinalQuestion.add(tempLocal[j])
                                }
                            }
                        } else {// 如果出现长度不同的话以远程答案为准
                            for (j in tempRemote.indices) {
                                tempFinalQuestion.add(tempRemote[j])
                            }
                        }
                        // 转成json字符串并添加
                        tempFinal.add(gson.toJson(tempFinalQuestion))
                    }
                } catch (e: Exception) {
                    // 解析错误，全部以远程为主
                    tempFinal.clear()
                    tempFinal.addAll(remoteList)
                }

                val mergedAnswer = StudentLocalAnswer(answerCardRepository.getUserId() + taskId, gson.toJson(tempFinal))// 最终答案
                answerCardRepository.updateStudentAnswer(mergedAnswer)
            }
        }
    }

    /**
     * 持续监听本地数据库答案变化
     */
    fun observerLocalAnswer(taskId: Long) {
        finalAnswer.addSource(answerCardRepository.queryStudentAnswer(taskId)) { localAnswer ->
            finalAnswer.removeSource(answerCardRepository.queryStudentAnswer(taskId))
            if (localAnswer != null) {// 查询本地数据库答案,不为空才赋值
                finalAnswer.value = localAnswer
            }
        }
    }

    /**
     * 更新答案
     * @param taskId 唯一标记
     * @param questionPos 第几道题
     * @param answerList 答案
     * @param oldLocalAnswer 本地数据库答案
     */
    fun updateAnswer(taskId: Long, questionPos: Int, answerList: ArrayList<StudentAnswer>?, oldLocalAnswer: StudentLocalAnswer) {
        answerList?.let {
            answerCardRepository.updateAnswer(taskId, questionPos, it, oldLocalAnswer)
        }
    }

    fun deleteAnswer(localAnswer: StudentLocalAnswer) {
        answerCardRepository.deleteAnswer(localAnswer)
    }

    /**
     * 滑动过程中提交上一题的答案
     * @param commitPos 需要提交的题号
     * @param localAnswer 本地数据库答案,这里是学生最新更改的
     * @param remoteAnswer 远程答案，只初始化一次，用于比对用，减轻服务器压力
     * @param workInfo 此套题的信息
     * @param questionInfo 对应题号的信息
     */
    fun commitAnswer(commitPos: Int, localAnswer: StudentLocalAnswer,
                     remoteAnswer: ArrayList<String>, workInfo: WorkInfo,
                     questionInfo: QuestionInfo, versionName: String,
                     createTime: String): Disposable? {
        return try {
            val jsonArray = Gson().fromJsonArray(localAnswer.answerList, String::class.java)
            val newAnswer = jsonArray[commitPos]
            if (newAnswer != remoteAnswer[commitPos]) {// 本地与服务器答案不一样，需要提交
                // 空答案不提交！
                val mSingleItemAnswerList = gson.fromJsonArray(newAnswer, StudentAnswer::class.java)
                mSingleItemAnswerList as ArrayList<StudentAnswer>
                var tempDisposable: Disposable? = null
                for (i in 0 until mSingleItemAnswerList.size) {
                    // 只要有一个空不为空，就提交
                    if (!TextUtils.isEmpty(mSingleItemAnswerList[i].Answer)) {
                        tempDisposable = answerCardRepository.commitStudentAnswerItem(commitPos, questionInfo, workInfo, newAnswer, versionName, createTime)
                        break
                    }
                }
                tempDisposable
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 最终提交的时候需要把未成功的答案依次提交完毕
     */
    private var finalTaskCount: Int = 0
    private var finalTaskSuccessCount: Int = 0
    val observerTask = MutableLiveData<Int>()
    val observerError = MutableLiveData<Throwable>()

    fun getFinalTaskCount() = finalTaskCount

    fun setFinalTaskCount(count: Int) {
        finalTaskCount = count
    }

    fun initFinalTaskCount(taskFlag: TaskFlag, localAnswer: StudentLocalAnswer) {

        try {
            val jsonArray = Gson().fromJsonArray(localAnswer.answerList, String::class.java)
            if (taskFlag.taskFlag.size != jsonArray.size)
                return

            // 下面是获取有多少个提交失败的任务的逻辑
            for (i in 0 until jsonArray.size) {

                val mAnswerList = gson.fromJsonArray(jsonArray[i], StudentAnswer::class.java)
                for (j in 0 until mAnswerList.size) {
                    // 只要有一个空不为空，且标记也是false的时候说明任务数+1
                    if (!TextUtils.isEmpty(mAnswerList[j].Answer)
                            && !taskFlag.taskFlag[i]) {
                        finalTaskCount++
                        break
                    }
                }

            }

            println("-------------------------->需要提交的任务数量：$finalTaskCount")

            if (finalTaskCount == 0) {// 没有需要提交的任务，单纯就是需要交卷了
                observerTask.value = END_WORK
            }

        } catch (e: Exception) {
            observerError.value = Throwable(e.message)
        }

    }

    fun finalTask(pos: Int,
                  localAnswer: StudentLocalAnswer,
                  workInfo: WorkInfo,
                  questionInfo: QuestionInfo,
                  versionName: String,
                  createTime: String): Disposable? {
        return try {
            val jsonArray = Gson().fromJsonArray(localAnswer.answerList, String::class.java)
            val newAnswer = jsonArray[pos]
            // 空答案不提交！
            val mSingleItemAnswerList = gson.fromJsonArray(newAnswer, StudentAnswer::class.java)
            mSingleItemAnswerList as ArrayList<StudentAnswer>
            var tempDisposable: Disposable? = null
            for (i in 0 until mSingleItemAnswerList.size) {
                // 只要有一个空不为空，就提交
                if (!TextUtils.isEmpty(mSingleItemAnswerList[i].Answer)) {
                    tempDisposable = answerCardRepository.commitFinalTaskItem(questionInfo, workInfo, newAnswer, versionName, createTime)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({

                                if (it.isSuccess) {// 单个提交成功，修改标识
                                    finalTaskSuccessCount++
                                    observerTask.value = finalTaskSuccessCount

                                    answerCardRepository.updateTaskFlag(pos, getUserId() + workInfo.TaskId)
                                } else {
                                    observerError.value = Throwable(it.message)
                                }

                            }, {
                                observerError.value = it
                            })
                    break
                }
            }
            tempDisposable
        } catch (e: Exception) {
            null
        }
    }

    fun uploadImage(context: Context, file: File) = answerCardRepository.uploadImage(context, file)

    fun commitAnswerAndEnd(taskId: Long, answerList: String) = answerCardRepository.commitAnswerAll(taskId, answerList)

    /**
     * 查询未上传成功的答题任务
     */
    val mTaskFlag = MutableLiveData<TaskFlag>()

    fun queryTaskFlag(taskId: Long) {
        appExecutors.diskIO().execute {
            val taskFlag: TaskFlag? = answerCardRepository.getTaskFlag(getUserId() + taskId)
            appExecutors.mainThread().execute {
                mTaskFlag.value = taskFlag
            }
        }
    }

    fun getPackageAnswer(latestAnswerList: String,
                         questionList: List<QuestionInfo>,
                         info: WorkInfo,
                         status: Int,
                         versionName: String,
                         common: String) = answerCardRepository.getPackageAnswer(latestAnswerList, questionList, info, status, versionName, common)

    //----------------附件处理----------------
    val attachmentBtnEnabled = MutableLiveData<Boolean>()

    init {
        attachmentBtnEnabled.value = true // 默认附件按钮可用
    }

    /**
     * 新作业，答题开始前的附件处理
     * @param info 作业信息
     */
    fun initAttachmentSettings(info: WorkInfo) {
        val canWatchTimes = ArrayList<Int>()
        info.attachments?.forEach { _ ->
            canWatchTimes.add(0)// 初始都是一次没有看过的
        }
        cache.put((getUserId() + info.TaskId).toString(), gson.toJson(canWatchTimes))
    }

    /**
     * 是否跳过附件检查
     * @return true 没有附件，直接开始答题 false 需要跳转到播放附件的页面
     */
    fun isSkipAttachment(info: WorkInfo): Boolean {
        val timeListString: String? = cache.getAsString((getUserId() + info.TaskId).toString())
        if (timeListString == null) {
            initAttachmentSettings(info)
            isSkipAttachment(info)
        } else {
            val watchTimeList = gson.fromJsonArray(timeListString, Integer::class.java)
            info.attachments as ArrayList<Attachment>
            for (i in 0 until watchTimeList.size) {
                if (info.attachments[i].CanWatchTimes == 0 || watchTimeList[i] < info.attachments[i].CanWatchTimes) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 由于存在播放次数，需要播放可以播放的那个
     */
    fun getAttachmentFirstPlay(info: WorkInfo): AttachmentRecord? {
        val timeListString: String? = cache.getAsString((getUserId() + info.TaskId).toString())
                ?: return null
        if (info.attachments.isNullOrEmpty()) return null
        val watchTimeList = gson.fromJsonArray(timeListString!!, Integer::class.java)
        info.attachments as ArrayList<Attachment>
        for (i in 0 until watchTimeList.size) {
            if (info.attachments[i].CanWatchTimes == 0 || watchTimeList[i] < info.attachments[i].CanWatchTimes) {
                return AttachmentRecord(i, info.attachments[i].Url)
            }
        }
        return null
    }

    /**
     * 获取附件是否可以播放
     */
    fun getAttachmentEnabled(info: WorkInfo): ArrayList<Boolean> {
        val enabledList = ArrayList<Boolean>()
        val timeListString: String? = cache.getAsString((getUserId() + info.TaskId).toString())
                ?: return enabledList
        if (info.attachments.isNullOrEmpty()) return enabledList
        val watchTimeList = gson.fromJsonArray(timeListString!!, Integer::class.java)
        info.attachments as ArrayList<Attachment>
        for (i in 0 until watchTimeList.size) {
            if (info.attachments[i].CanWatchTimes == 0 || watchTimeList[i] < info.attachments[i].CanWatchTimes) {
                enabledList.add(true)
            } else {
                enabledList.add(false)
            }
        }
        return enabledList
    }

    /**
     * 判断是否需要强制播放视频
     */
    fun initAttachmentBtnStatus(info: WorkInfo) {
        val timeListString: String? = cache.getAsString((getUserId() + info.TaskId).toString())
                ?: return
        if (info.attachments.isNullOrEmpty()) return
        val watchTimeList = gson.fromJsonArray(timeListString!!, Integer::class.java)
        info.attachments as ArrayList<Attachment>
        loop@ for (i in 0 until watchTimeList.size) {
            if (info.attachments[i].AnswerType == 2// 播完之后才可作答
                    && watchTimeList[i] as Int == 0) {// 一次也没有看过
                attachmentBtnEnabled.value = false
                break@loop
            } else {
                attachmentBtnEnabled.value = true
            }
        }
    }

    @SuppressLint("UseValueOf")
    fun recordPlayer(info: WorkInfo, pos: Int) {
        val timeListString: String? = cache.getAsString((getUserId() + info.TaskId).toString())
                ?: return
        val watchTimeList = gson.fromJsonArray(timeListString!!, Integer::class.java)
        val oldTime = watchTimeList[pos].toInt()
        val newTime = oldTime + 1
        watchTimeList as ArrayList
        watchTimeList[pos] = Integer(newTime)
        cache.put((getUserId() + info.TaskId).toString(), gson.toJson(watchTimeList))
        initAttachmentBtnStatus(info)
    }

    /**
     * 答题中，每小题的附件处理
     */
    fun initInnerAttachmentSettings() {

    }

}