package cqebd.student.vo

data class MessageData(
        val total: Int,
        val pages: Int,
        val index: Int,
        val dataList: List<Data>
)

data class Data(
        val Id: Int,
        val Title: String,
        val Content: String,
        val CreateDateTime: String,
        var Status: Int,
        val Type: Int,
        val Images: String,
        val StudentId: Int
)