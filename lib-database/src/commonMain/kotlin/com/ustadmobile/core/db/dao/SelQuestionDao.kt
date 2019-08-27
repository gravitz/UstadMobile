package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.SelQuestion

@UmDao(selectPermissionCondition = "(:accountPersonUid = :accountPersonUid)")
@UmRepository
@Dao
abstract class SelQuestionDao : BaseDao<SelQuestion> {

    @Insert
    abstract override fun insert(entity: SelQuestion): Long

    @Update
    abstract override fun update(entity: SelQuestion)

    @Query("SELECT * FROM SelQuestion")
    abstract fun findAllQuestions(): DataSource.Factory<Int, SelQuestion>

    @Update
    abstract suspend fun updateAsync(entity: SelQuestion):Int

    @Query("SELECT * FROM SelQuestion WHERE selQuestionUid = :uid")
    abstract fun findByUid(uid: Long): SelQuestion?

    @Query("SELECT * FROM SelQuestion WHERE selQuestionUid = :uid")
    abstract suspend fun findByUidAsync(uid: Long) : SelQuestion?

    @Query("SELECT * FROM SelQuestion WHERE selQuestionUid = :uid")
    abstract fun findByUidLive(uid: Long): DoorLiveData<SelQuestion?>

    @Query("SELECT MAX(questionIndex) FROM SelQuestion")
    abstract suspend fun getMaxIndexAsync() : Int

    @Query("SELECT MAX(questionIndex) FROM SelQuestion WHERE " +
            "selQuestionSelQuestionSetUid = :questionSetUid " +
            " AND questionActive = 1")
    abstract suspend fun getMaxIndexByQuestionSetAsync(questionSetUid: Long): Int

    @Query("SELECT * FROM SelQuestion where " + "selQuestionSelQuestionSetUid = :questionSetUid")
    abstract suspend fun findAllByQuestionSetUidAsync(questionSetUid: Long) :SelQuestion?

    @Query("SELECT * FROM SelQuestion WHERE " + "selQuestionSelQuestionSetUid = :questionUid")
    abstract fun findAllQuestionsInSet(questionUid: Long): DataSource.Factory<Int, SelQuestion>

    @Query("SELECT * FROM SelQuestion WHERE " +
            "selQuestionSelQuestionSetUid = :questionUid AND " +
            "questionActive = 1")
    abstract fun findAllActivrQuestionsInSet(questionUid: Long): DataSource.Factory<Int, SelQuestion>

    @Query("SELECT * FROM SelQuestion WHERE " +
            " selQuestionSelQuestionSetUid = :questionSetUid " +
            " AND questionIndex > :previousIndex ORDER BY questionIndex ASC LIMIT 1    ")
    abstract suspend fun findNextQuestionByQuestionSetUidAsync(questionSetUid: Long,
                                                       previousIndex: Int) : SelQuestion?

    @Query("SELECT MIN(questionIndex) FROM SelQuestion")
    abstract suspend fun getMinIndexAsync() : Int

    @Query("SELECT COUNT(*) FROM SelQuestion")
    abstract fun findTotalNumberOfQuestions(): Int

    @Query("SELECT COUNT(*) FROM SelQuestion WHERE" +
            " selQuestionSelQuestionSetUid = :questionSetUid AND " +
            " questionActive = 1")
    abstract fun findTotalNumberOfActiveQuestionsInAQuestionSet(questionSetUid: Long): Int

    @Query("SELECT * FROM SelQuestion WHERE questionText = :question")
    abstract suspend fun findByQuestionStringAsync(question: String) : List<SelQuestion>

    companion object {

        val SEL_QUESTION_TYPE_NOMINATION = 0
        val SEL_QUESTION_TYPE_MULTI_CHOICE = 1
        val SEL_QUESTION_TYPE_FREE_TEXT = 2
    }


}
