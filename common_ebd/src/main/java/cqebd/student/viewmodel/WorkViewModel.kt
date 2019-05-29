package cqebd.student.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cqebd.student.repository.WorkRepository
import cqebd.student.vo.*
import xiaofu.lib.network.Resource
import javax.inject.Inject

/**
 *
 * Created by @author xiaofu on 2019/2/21.
 */
class WorkViewModel @Inject constructor(private val workRepository: WorkRepository) : ViewModel() {

    val workToDoList = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<List<WorkInfo>>>>()
    val workDoneList = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<List<WorkInfo>>>>()
    val wrongQuestionDetails = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<WrongQuestionDetails>>>()
    val shareHomeworkList = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<ShareHomework>>>()
    val workWrongList = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<List<WrongQuestion>>>>()
    val subjectCurrent = MutableLiveData<User.Subject>()

    /**
     * 筛选学科
     */
    fun filterSubject(subject: User.Subject) {
        subjectCurrent.value = subject
    }

    /**
     * 获取学科
     */
    fun getSubjectList() = workRepository.getSubjectList()

    /**
     * 学生未完成作业列表
     */
    fun getWorkToDoList(page: Int) {
        // 默认未初始化时为null
        val subjectId: Int? = if (subjectCurrent.value == null
                || subjectCurrent.value?.Id == -1) null else subjectCurrent.value!!.Id
        workToDoList.addSource(workRepository.getWorkToDoList(page, subjectId)) {
            workToDoList.removeSource(workRepository.getWorkToDoList(page, subjectId))
            workToDoList.value = it
        }
    }

    /**
     * 学生已完成作业列表
     */
    fun getWorkDoneList(page: Int) {
        // 默认未初始化时为null
        val subjectId: Int? = if (subjectCurrent.value == null
                || subjectCurrent.value?.Id == -1) null else subjectCurrent.value!!.Id
        workDoneList.addSource(workRepository.getWorkDoneList(page, subjectId)) {
            workDoneList.removeSource(workRepository.getWorkDoneList(page, subjectId))
            workDoneList.value = it
        }
    }

    //作业分享列表
    fun getShareHomeworkList(page: Int) {
        // 默认未初始化时为null
        val subjectTypeId: Int? = if (subjectCurrent.value == null
                || subjectCurrent.value?.Id == -1) null else subjectCurrent.value!!.Id
        shareHomeworkList.addSource(workRepository.getShareHomeworkList(page, subjectTypeId)) {
            shareHomeworkList.removeSource(workRepository.getShareHomeworkList(page, subjectTypeId))
            shareHomeworkList.value = it
        }
    }

    //错题列表
    fun getWrongQuestionList(page: Int, subjectId: Int) {
        workWrongList.addSource(workRepository.getWrongQuestionList(page, subjectId)) {
            workWrongList.removeSource(workRepository.getWrongQuestionList(page, subjectId))
            workWrongList.value = it
        }
    }

    fun getWrongQuesionDetails(questionTaskId: Int) {
        wrongQuestionDetails.addSource(workRepository.getWrongQuestionDetails(questionTaskId)) {
            wrongQuestionDetails.removeSource(workRepository.getWrongQuestionDetails(questionTaskId))
            wrongQuestionDetails.value = it
        }
    }
}