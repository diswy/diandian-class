package cqebd.student.repository


/**
 *
 * Created by @author xiaofu on 2019/7/5.
 */
class KRepository {
    private var allPeopleCount = 0
    fun setPeopleCout(count:Int){
        allPeopleCount = count
    }
    fun addPeople(){
        allPeopleCount++
    }
    fun removePeople(){
        allPeopleCount--
    }
    fun getPeople() = allPeopleCount

}