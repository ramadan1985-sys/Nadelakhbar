package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.NewsArticle

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val sourceName: String,
    val originalLanguage: String,
    val country: String,
    val category: String,
    val publishedAt: String,
    val keyPointsJson: String,
    val aiAnalysis: String,
    val isBreaking: Boolean,
    val isBookmarked: Boolean,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toModel(): NewsArticle {
        val points = if (keyPointsJson.isBlank()) {
            emptyList()
        } else {
            keyPointsJson.split("|||").filter { it.isNotBlank() }
        }
        return NewsArticle(
            id = id,
            title = title,
            summary = summary,
            content = content,
            sourceName = sourceName,
            originalLanguage = originalLanguage,
            country = country,
            category = category,
            publishedAt = publishedAt,
            keyPoints = points,
            aiAnalysis = aiAnalysis,
            isBreaking = isBreaking,
            isBookmarked = isBookmarked,
            url = url
        )
    }

    companion object {
        fun fromModel(model: NewsArticle, bookmarked: Boolean = model.isBookmarked): ArticleEntity {
            return ArticleEntity(
                id = model.id,
                title = model.title,
                summary = model.summary,
                content = model.content,
                sourceName = model.sourceName,
                originalLanguage = model.originalLanguage,
                country = model.country,
                category = model.category,
                publishedAt = model.publishedAt,
                keyPointsJson = model.keyPoints.joinToString("|||"),
                aiAnalysis = model.aiAnalysis,
                isBreaking = model.isBreaking,
                isBookmarked = bookmarked,
                url = model.url
            )
        }
    }
}
