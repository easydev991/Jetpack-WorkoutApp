package com.swparks.data.repository

import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.network.SWApi
import com.swparks.ui.model.TextEntryOption
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import retrofit2.HttpException
import java.io.IOException

open class CommentsRepository(
    private val swApi: SWApi,
    private val parkDao: ParkDao,
    private val journalEntryDao: JournalEntryDao,
    logger: Logger,
    crashReporter: CrashReporter
) : BaseRepository(logger, crashReporter) {
    companion object {
        private const val TAG = "CommentsRepository"
    }

    open suspend fun addComment(
        option: TextEntryOption,
        comment: String
    ): Result<Unit> =
        when (option) {
            is TextEntryOption.Park ->
                try {
                    val response = swApi.addCommentToPark(option.id, comment)
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "добавлении комментария"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "добавлении комментария"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "добавлении комментария"))
                }

            is TextEntryOption.Event ->
                try {
                    val response = swApi.addCommentToEvent(option.id, comment)
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "добавлении комментария"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "добавлении комментария"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "добавлении комментария"))
                }

            is TextEntryOption.Journal -> {
                try {
                    val response =
                        swApi.saveJournalEntry(
                            option.ownerId,
                            option.journalId,
                            comment
                        )
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "создании записи в дневнике"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "создании записи в дневнике"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "создании записи в дневнике"))
                }
            }
        }

    open suspend fun editComment(
        option: TextEntryOption,
        commentId: Long,
        newComment: String
    ): Result<Unit> =
        when (option) {
            is TextEntryOption.Park ->
                try {
                    val response = swApi.editParkComment(option.id, commentId, newComment)
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "редактировании комментария"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "редактировании комментария"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "редактировании комментария"))
                }

            is TextEntryOption.Event ->
                try {
                    val response = swApi.editEventComment(option.id, commentId, newComment)
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "редактировании комментария"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "редактировании комментария"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "редактировании комментария"))
                }

            is TextEntryOption.Journal ->
                editJournalEntry(
                    option.ownerId,
                    option.journalId,
                    commentId,
                    newComment
                )
        }

    private suspend fun editJournalEntry(
        ownerId: Long,
        journalId: Long,
        entryId: Long,
        newMessage: String
    ): Result<Unit> =
        try {
            val response = swApi.editJournalEntry(ownerId, journalId, entryId, newMessage)

            if (response.isSuccessful) {
                updateLocalJournalEntry(entryId, newMessage)
                Result.success(Unit)
            } else {
                Result.failure(
                    handleHttpException(
                        HttpException(response),
                        TAG,
                        "редактировании записи в дневнике"
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(handleIOException(e, TAG, "редактировании записи в дневнике"))
        } catch (e: HttpException) {
            Result.failure(handleHttpException(e, TAG, "редактировании записи в дневнике"))
        }

    private suspend fun updateLocalJournalEntry(
        entryId: Long,
        newMessage: String
    ) {
        val existingEntry = journalEntryDao.getById(entryId) ?: return
        val updatedEntry =
            existingEntry.copy(
                message = newMessage,
                modifyDate = System.currentTimeMillis()
            )
        journalEntryDao.insert(updatedEntry)
    }

    open suspend fun deleteComment(
        option: TextEntryOption,
        commentId: Long
    ): Result<Unit> =
        when (option) {
            is TextEntryOption.Park ->
                try {
                    val response = swApi.deleteParkComment(option.id, commentId)
                    if (response.isSuccessful) {
                        updateDeletedCommentCache(option.id, commentId)
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "удалении комментария"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "удалении комментария"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "удалении комментария"))
                }

            is TextEntryOption.Event ->
                try {
                    val response = swApi.deleteEventComment(option.id, commentId)
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "удалении комментария"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "удалении комментария"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "удалении комментария"))
                }

            is TextEntryOption.Journal -> {
                try {
                    val response =
                        swApi.deleteJournalEntry(
                            option.ownerId,
                            option.journalId,
                            commentId
                        )
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            handleHttpException(
                                HttpException(response),
                                TAG,
                                "удалении записи из дневника"
                            )
                        )
                    }
                } catch (e: IOException) {
                    Result.failure(handleIOException(e, TAG, "удалении записи из дневника"))
                } catch (e: HttpException) {
                    Result.failure(handleHttpException(e, TAG, "удалении записи из дневника"))
                }
            }
        }

    @Suppress("ReturnCount")
    private suspend fun updateDeletedCommentCache(
        parkId: Long,
        commentId: Long
    ) {
        val cachedPark = parkDao.getParkById(parkId) ?: return
        val comments = cachedPark.comments ?: return

        val updatedComments = comments.filter { it.id != commentId }
        if (updatedComments.size == comments.size) return

        val newCommentsCount =
            cachedPark.commentsCount?.let { count ->
                maxOf(count - 1, 0)
            }

        parkDao.upsertPark(
            cachedPark.copy(
                comments = updatedComments,
                commentsCount = newCommentsCount
            )
        )
        logger.d(
            TAG,
            "Обновлён кэш комментариев для площадки $parkId, удалён комментарий $commentId"
        )
    }
}
