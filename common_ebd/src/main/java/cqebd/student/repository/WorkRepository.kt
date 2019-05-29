package cqebd.student.repository

import androidx.lifecycle.LiveData
import cqebd.student.cache.EbdMemoryCache
import cqebd.student.db.WorkDao
import cqebd.student.network.EbdWorkService
import cqebd.student.vo.*
import xiaofu.lib.network.ApiResponse
import xiaofu.lib.network.NetworkResource
import xiaofu.lib.network.Resource
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * Created by @author xiaofu on 2019/2/21.
 */
@Singleton
class WorkRepository @Inject constructor(
        private val workService: EbdWorkService,
        private val workDao: WorkDao,
        private val memoryCache: EbdMemoryCache
) {

    fun getWorkToDoList(page: Int, subjectId: Int?): LiveData<xiaofu.lib.network.Resource<BaseResponse<List<WorkInfo>>>> {

        return object : xiaofu.lib.network.NetworkResource<BaseResponse<List<WorkInfo>>>() {

            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<WorkInfo>>>> {
                return workService.getWorkList(memoryCache.getId(), subjectId, null, 10, page)
            }

        }.asLiveData()
    }

    fun getWorkDoneList(page: Int, subjectId: Int?): LiveData<xiaofu.lib.network.Resource<BaseResponse<List<WorkInfo>>>> {

        return object : xiaofu.lib.network.NetworkResource<BaseResponse<List<WorkInfo>>>() {

            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<WorkInfo>>>> {
                return workService.getWorkList(memoryCache.getId(), subjectId, null, 12, page)
            }

        }.asLiveData()
    }

    fun getSubjectList() = memoryCache.getSubjectList()


    //作业分享列表
    fun getShareHomeworkList(page: Int, subjectTypeId: Int?): LiveData<xiaofu.lib.network.Resource<BaseResponse<ShareHomework>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<ShareHomework>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<ShareHomework>>> {
                return workService.getShareHomeworkList(null, subjectTypeId, null, null, memoryCache.getId(), page)
            }
        }.asLiveData()
    }

    //错题列表
    fun getWrongQuestionList(page: Int, subjectId: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<List<WrongQuestion>>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<List<WrongQuestion>>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<WrongQuestion>>>> {
                return workService.getWrongQuestionList(memoryCache.getId(), subjectId, null, null, page)
            }
        }.asLiveData()
    }

    fun getWrongQuestionDetails(questionTaskId: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<WrongQuestionDetails>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<WrongQuestionDetails>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<WrongQuestionDetails>>> {
                return workService.getErrorQuestions(questionTaskId)
            }
        }.asLiveData()
    }

}