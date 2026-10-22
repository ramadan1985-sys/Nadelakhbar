package com.example.data.model

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val sourceName: String,
    val originalLanguage: String,
    val country: String,
    val category: String,
    val publishedAt: String,
    val keyPoints: List<String>,
    val aiAnalysis: String,
    val isBreaking: Boolean = false,
    val isBookmarked: Boolean = false,
    val url: String = ""
)

enum class NewsCategory(val arabicLabel: String, val englishQuery: String) {
    ALL("كافة الأسلاك", "all"),
    POLITICS("سياسة وعلاقات دولية", "politics diplomacy world news"),
    ECONOMY("مال وأعمال واقتصاد", "economy markets business finance"),
    TECHNOLOGY("تكنولوجيا وذكاء اصطناعي", "technology artificial intelligence innovation"),
    SCIENCE("علوم وصحة وبيئة", "science health climate medicine"),
    SPORTS("رياضة عالمية", "sports football athletics"),
    CULTURE("ثقافة وفنون ومنوعات", "culture arts lifestyle society")
}

enum class DateRangeFilter(val arabicLabel: String, val daysSpan: Int) {
    TODAY("اليوم (24 ساعة)", 1),
    PAST_48_HOURS("آخر 48 ساعة", 2),
    THIS_WEEK("هذا الأسبوع", 7),
    THIS_MONTH("هذا الشهر", 30),
    ANY_TIME("كافة التواريخ", 365)
}

enum class WorldRegion(val arabicLabel: String, val queryKeyword: String) {
    ALL("العالم أجمع", ""),
    MIDDLE_EAST("الشرق الأوسط والعالم العربي", "Middle East Arab world"),
    EUROPE("أوروبا", "Europe"),
    AMERICAS("الأمريكيتان", "Americas United States"),
    ASIA_PACIFIC("آسيا والمحيط الهادئ", "Asia China Japan India"),
    AFRICA("أفريقيا", "Africa")
}
