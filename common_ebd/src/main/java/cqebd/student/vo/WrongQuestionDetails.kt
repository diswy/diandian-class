package cqebd.student.vo

/**
 *
 * Created by @author xiaofu on 2019/4/18.
 */
data class WrongQuestionDetails(
        val StudentQuestionsTasksID: Int,
        val ExaminationPapersID: Int,
        val Count: Int,
        val ExaminationPapersName: String,
        val ErrorList: List<WrongQuestionDetailsItem>
)

data class WrongQuestionDetailsItem(
        val querstionId: Int,
        val sortId: Int
)