package cqebd.student.network

/**
 * 基础数据
 * Created by @author xiaofu on 2019/2/27.
 */

class EbdUrl {
    companion object {
        private const val EBD_URL = "https://service-student.cqebd.cn/"
        const val HOMEWORK_END_LOOK = EbdUrl.EBD_URL + "HomeWork/CheckPaper?StudentQuestionsTasksId=%s"
        const val HOMEWORK_PREVIEW_LOOK = EbdUrl.EBD_URL + "HomeWork/ExaminationPapers?id=%s&taskid=%s"
        const val USER_LEADER = EbdUrl.EBD_URL + "StudentGroup/task?GroupStudentId=%s"
        const val MESSAGE_RESPONSE = EbdUrl.EBD_URL + "Help/Feedback?id=%s"
        const val QUESTION_ITEM = EbdUrl.EBD_URL + "HomeWork/Question?id=%s&PapersID=%s&studentid=%s"
        const val HOMEWORK_COLLECTION_ALL = EbdUrl.EBD_URL + "studentCollect/StudentCollectList?studentid=%s"
        const val HOMEWORK_COLLECTION_SUBJECT = EbdUrl.EBD_URL + "studentCollect/StudentCollectList?studentid=%s&SubjectTypeId=%d"
        const val SHARE_HOMEWORK_DETAIL = EbdUrl.EBD_URL + "HomeWork/TaskShare?id=%d"
        const val READ_MESSAGE = EbdUrl.EBD_URL + "homework/msgdetails?id=%d"
        const val WRONG_QUESTION = EbdUrl.EBD_URL + "HomeWork/ErrorQustionAnswer?QuestionID=%s&StudentQuestionsTasksId=%s"
    }
}
