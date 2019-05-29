package cqebd.student.repository

import android.content.Context
import android.os.Build
import android.text.TextUtils
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import cqebd.student.cache.EbdMemoryCache
import cqebd.student.db.WorkDao
import cqebd.student.network.EbdWorkService
import cqebd.student.vo.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import top.zibin.luban.Luban
import xiaofu.lib.AppExecutors
import xiaofu.lib.utils.fromJsonArray
import xiaofu.lib.utils.unicode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 做作业模块
 * Created by @author xiaofu on 2019/3/21.
 */
@Singleton
class AnswerCardRepository @Inject constructor(
    private val appExecutors: xiaofu.lib.AppExecutors,
    private val workService: EbdWorkService,
    private val workDao: WorkDao,
    private val gson: Gson,
    private val memoryCache: EbdMemoryCache
) {

    fun getUserId() = memoryCache.getId()

    fun getTaskFlag(taskId: Long) = workDao.onlyLoadTaskFlag(taskId)

    /**
     * 会成对出现，将提交模式的值再这里处理
     * [commitAnswerAll]
     */
    private var commitMode: Int = 0// 提交模式

    fun startWork(taskId: Long, paperId: Long): Flowable<BaseResponse<List<ExamPaper>>> {
        return workService.userStartWork(taskId)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    commitMode = it.data?.submitMode ?: 0
                    return@flatMap workService.getExamPaper(paperId, taskId)
                }

    }

    /**
     * 创建本地答案以及答题标记
     * 标记用于记录是否上传成功
     */
    fun insertStudentAnswer(localAnswer: StudentLocalAnswer) {
        appExecutors.diskIO().execute {
            workDao.insertStudentLocalAnswer(localAnswer)
            // 初始化本地数据库的时候，也记录一下答题标记
            // true 答案已提交过，false 答案未提交
            val flagList = ArrayList<Boolean>()
            val array: List<String> = gson.fromJsonArray(localAnswer.answerList, String::class.java)
            array.forEach {
                //flagList.add(false)// 如果换了设备，姑且都认为没有上传成功需要重新上传，注释掉的部分可处理另外一种情况
                val answers: ArrayList<StudentAnswer> = gson.fromJsonArray(it, StudentAnswer::class.java) as ArrayList<StudentAnswer>
                var count = 0// 计数是否已做空数
                for (i in 0 until answers.size) {
                    if (!TextUtils.isEmpty(answers[i].Answer)) {
                        count++
                    }
                }
                if (count > 0) {
                    flagList.add(true)
                } else {
                    flagList.add(false)
                }
            }
            val taskFlag = TaskFlag(localAnswer.taskId, flagList)
            workDao.insertTaskFlag(taskFlag)
        }
    }

    fun updateStudentAnswer(localAnswer: StudentLocalAnswer) {
        appExecutors.diskIO().execute {
            workDao.updateStudentLocalAnswer(localAnswer)
        }
    }

    fun queryStudentAnswer(taskId: Long): LiveData<StudentLocalAnswer> {
        return workDao.queryStudentLocalAnswerByTask(getUserId() + taskId)
    }

    fun onlyLoad(taskId: Long) = workDao.onlyLoadAnswer(getUserId() + taskId)

    /**
     * @param taskId 唯一标记
     * @param questionPos 第几道题
     * @param answerList 更新后得答案
     * @param oldLocalAnswer 前一波的本地答案
     */
    fun updateAnswer(taskId: Long, questionPos: Int, answerList: ArrayList<StudentAnswer>, oldLocalAnswer: StudentLocalAnswer) {
        appExecutors.diskIO().execute {
            val array: ArrayList<String> = gson.fromJsonArray(oldLocalAnswer.answerList, String::class.java) as ArrayList<String>
            val oldList = gson.fromJsonArray(array[questionPos], StudentAnswer::class.java)
            if (array[questionPos] == gson.toJson(answerList)
                    && answerList.size != oldList.size)// 答案一样不需要重新插入数据库,新答案与旧答案长度不一样也不允许写入
                return@execute

            array[questionPos] = gson.toJson(answerList)
            val newLocalAnswer = StudentLocalAnswer(getUserId() + taskId, gson.toJson(array))
            workDao.updateStudentLocalAnswer(newLocalAnswer)

            // 同样的更新了答案，机器认为该任务需要重新被提交
            val taskFlag: TaskFlag? = workDao.onlyLoadTaskFlag(getUserId() + taskId)
            taskFlag ?: return@execute
            taskFlag.taskFlag as ArrayList
            taskFlag.taskFlag[questionPos] = false
            workDao.updateTaskFlag(taskFlag)
        }
    }

    /**
     * 删除答案同时清除答题标记,如果存在附件记录也需要删除
     */
    fun deleteAnswer(localAnswer: StudentLocalAnswer) {
        appExecutors.diskIO().execute {
            workDao.deleteStudentLocalAnswer(localAnswer)
            workDao.deleteTaskFlagById(localAnswer.taskId)
            memoryCache.removeKey(localAnswer.taskId.toString())
        }
    }

    /**
     * 提交单道题目的答案
     * 参数太多，不想写注释，反正就是沿用以前逻辑，没有那么多为什么
     */
    fun commitStudentAnswerItem(
            commitPos: Int,
            questionInfo: QuestionInfo,
            workInfo: WorkInfo,
            answerString: String,
            versionName: String,
            createTime: String): Disposable {
        return workService.submitAnswer(memoryCache.getId(), questionInfo.Id,
                workInfo.PapersId.toInt(), workInfo.TaskId.toInt(), questionInfo.QuestionTypeId,
                answerString.unicode(),// 这里进行转码操作
                versionName, createTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    if (it.isSuccess) {// 单个提交成功，修改标识
                        updateTaskFlag(commitPos, getUserId() + workInfo.TaskId)
                    }

                }, {})
    }

    /**
     * 更新答题标记
     */
    fun updateTaskFlag(pos: Int, taskId: Long) {
        appExecutors.diskIO().execute {
            val taskFlag: TaskFlag? = workDao.onlyLoadTaskFlag(getUserId() + taskId)
            taskFlag ?: return@execute
            taskFlag.taskFlag as ArrayList
            println("---->>>>答案：修改之前${taskFlag.taskFlag[pos]}")
            taskFlag.taskFlag[pos] = true
            println("---->>>>答案：修改之后${taskFlag.taskFlag[pos]}")
            workDao.updateTaskFlag(taskFlag)
        }
    }

    /**
     * 交卷之前的二次检测
     * 提交单道题目的答案
     * 逻辑同[commitStudentAnswerItem]
     */
    fun commitFinalTaskItem(
            questionInfo: QuestionInfo,
            workInfo: WorkInfo,
            answerString: String,
            versionName: String,
            createTime: String): Flowable<BaseResponse<Unit>> {
        return workService.submitAnswer(memoryCache.getId(), questionInfo.Id,
                workInfo.PapersId.toInt(), workInfo.TaskId.toInt(), questionInfo.QuestionTypeId,
                answerString.unicode(),// 这里进行转码操作
                versionName, createTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 提交所有答案
     * @param taskId 唯一标记
     * @param answerList 答案，打包上传
     * @param status 提交模式：0 每个题提交，最后打包再提交；1 最后打包提交；任务开始时后台提供
     */
    fun commitAnswerAll(taskId: Long, answerList: String, status: Int = commitMode): Flowable<BaseResponse<Int>> {
        return workService.submitAnswers(taskId, answerList.unicode(), status)
                .flatMap {
                    return@flatMap workService.endWork(taskId)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 上传图片
     */
    fun uploadImage(context: Context, file: File): Flowable<BaseResponse<String>> {
        return Flowable.just(file)
                .observeOn(Schedulers.io())
                .map {
                    return@map Luban.with(context).load(it).get()[0]
                }
                .flatMap {
                    val requestBody = RequestBody.create(MediaType.parse("image/jpeg"), it)
                    return@flatMap workService.uploadFile(requestBody)
                }
    }


    fun getPackageAnswer(latestAnswerList: String, questionList: List<QuestionInfo>,
                         info: WorkInfo, status: Int,
                         versionName: String, common: String): String {
        val answerList = ArrayList<Answer>()
        val array: ArrayList<String> = gson.fromJsonArray(latestAnswerList, String::class.java) as ArrayList<String>
        for (i in 0 until questionList.size) {

            val answerItemArray: ArrayList<StudentAnswer> = gson.fromJsonArray(array[i], StudentAnswer::class.java) as ArrayList<StudentAnswer>
            var needCommit = false // 只要有一个不为空就应该提交答案
            answerItemArray.forEach {
                if (!it.Answer.isEmpty()) {
                    needCommit = true
                    return@forEach
                }
            }
            if (needCommit) {
                answerList.add(Answer(questionList[i].Id, questionList[i].QuestionTypeId, array[i]))
            }
        }
        val packageAnswer = PackageAnswer(
                memoryCache.getId(),
                info.TaskId,
                status,
                info.PapersId,
                info.PushId,
                versionName,
                common,
                Build.MODEL,
                answerList)

        val mAnswerCommon = PackageAnswerCommon(packageAnswer)
        return gson.toJson(mAnswerCommon)
    }
}