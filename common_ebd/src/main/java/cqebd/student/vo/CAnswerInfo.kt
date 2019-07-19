package cqebd.student.vo

/**
 *
 * Created by @author xiaofu on 2019/6/17.
 */
data class CAnswerInfo(
        val QuestionDesc: String,
        val QuestionType: Int,
        val QuestionCount: Int,
        val QuestionOptions: Int,
        val DownCountMinute: Int
)

enum class CAnswerType(val value: Int) {
    SINGLE(1),
    MULTIPLE(2),
    TRUE_OR_FALSE(3),
    BLANK(4),
    SUBJECTIVE(5),
    PHOTO(6)
}