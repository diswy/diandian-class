package cqebd.student.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cqebd.student.repository.VideoRepository
import cqebd.student.vo.BaseResponse
import cqebd.student.vo.CourseInfo
import cqebd.student.vo.User
import cqebd.student.vo.VideoInfo
import xiaofu.lib.network.Resource
import javax.inject.Inject

/**
 *
 * Created by @author xiaofu on 2019/1/9.
 */
class VideoViewModel @Inject constructor(private val videoRepository: VideoRepository) : ViewModel() {

    val videoList = MediatorLiveData<xiaofu.lib.network.Resource<List<VideoInfo>>>()
    val courseList = MediatorLiveData<xiaofu.lib.network.Resource<List<CourseInfo>>>()
    val collectList = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<List<VideoInfo>>>>()
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
    fun getSubjectList() = videoRepository.getSubjectList()

    fun getVideoList(shouldFetch: Boolean = false) {
        videoList.addSource(videoRepository.getVideoList(shouldFetch)) {
            videoList.removeSource(videoRepository.getVideoList(shouldFetch))
            videoList.value = it
        }
    }

    fun getCourseList(id: Int, shouldFetch: Boolean = false) {
        courseList.addSource(videoRepository.getCourseList(id, shouldFetch)) {
            courseList.removeSource(videoRepository.getCourseList(id, shouldFetch))
            courseList.value = it
        }
    }

    fun getCourseDetail(id: Int) = videoRepository.getCourseDetail(id)

    /**
     * 获取收藏课程列表
     */
    fun getCollectList(){
        collectList.addSource(videoRepository.getCollectList()){
            collectList.removeSource(videoRepository.getCollectList())
            collectList.value = it
        }
    }
}