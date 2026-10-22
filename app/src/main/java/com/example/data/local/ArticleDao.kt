package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY timestamp DESC LIMIT 60")
    fun getAllCachedArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Query("UPDATE articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: String, bookmarked: Boolean)

    @Query("SELECT isBookmarked FROM articles WHERE id = :id LIMIT 1")
    suspend fun isBookmarked(id: String): Boolean?

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND timestamp < :olderThanTimestamp")
    suspend fun cleanupOldArticles(olderThanTimestamp: Long)
}
