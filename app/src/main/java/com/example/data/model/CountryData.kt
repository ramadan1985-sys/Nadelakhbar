package com.example.data.model

enum class CountryGroup(val titleArabic: String, val subtitleArabic: String) {
    SECURITY_COUNCIL_P5(
        "الدول دائمة العضوية في مجلس الأمن الدولي",
        "الدول الخمس الكبرى ذات حق النقض (الفيتو)"
    ),
    GCC(
        "دول مجلس التعاون لدول الخليج العربية",
        "الدول الست الأعضاء في مجلس التعاون"
    )
}

data class CountryItem(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val flagEmoji: String,
    val group: CountryGroup,
    val capital: String,
    val searchTerms: List<String>
)

object SupportedCountries {
    val list: List<CountryItem> = listOf(
        // الخمس دول الأعضاء الدائمين بمجلس الأمن
        CountryItem(
            id = "usa",
            nameArabic = "الولايات المتحدة الأمريكية",
            nameEnglish = "United States",
            flagEmoji = "🇺🇸",
            group = CountryGroup.SECURITY_COUNCIL_P5,
            capital = "واشنطن",
            searchTerms = listOf("أمريكا", "الولايات المتحدة", "واشنطن", "البيت الأبيض", "United States", "US")
        ),
        CountryItem(
            id = "china",
            nameArabic = "جمهورية الصين الشعبية",
            nameEnglish = "China",
            flagEmoji = "🇨🇳",
            group = CountryGroup.SECURITY_COUNCIL_P5,
            capital = "بكين",
            searchTerms = listOf("الصين", "بكين", "China", "Beijing")
        ),
        CountryItem(
            id = "russia",
            nameArabic = "روسيا الاتحادية",
            nameEnglish = "Russia",
            flagEmoji = "🇷🇺",
            group = CountryGroup.SECURITY_COUNCIL_P5,
            capital = "موسكو",
            searchTerms = listOf("روسيا", "موسكو", "الكرملين", "Russia", "Moscow")
        ),
        CountryItem(
            id = "uk",
            nameArabic = "المملكة المتحدة (بريطانيا)",
            nameEnglish = "United Kingdom",
            flagEmoji = "🇬🇧",
            group = CountryGroup.SECURITY_COUNCIL_P5,
            capital = "لندن",
            searchTerms = listOf("بريطانيا", "المملكة المتحدة", "لندن", "UK", "Britain", "London")
        ),
        CountryItem(
            id = "france",
            nameArabic = "الجمهورية الفرنسية",
            nameEnglish = "France",
            flagEmoji = "🇫🇷",
            group = CountryGroup.SECURITY_COUNCIL_P5,
            capital = "باريس",
            searchTerms = listOf("فرنسا", "باريس", "الإليزيه", "France", "Paris")
        ),

        // دول مجلس التعاون الخليجي الست
        CountryItem(
            id = "saudi",
            nameArabic = "المملكة العربية السعودية",
            nameEnglish = "Saudi Arabia",
            flagEmoji = "🇸🇦",
            group = CountryGroup.GCC,
            capital = "الرياض",
            searchTerms = listOf("السعودية", "المملكة العربية السعودية", "الرياض", "Saudi Arabia", "Riyadh")
        ),
        CountryItem(
            id = "uae",
            nameArabic = "الإمارات العربية المتحدة",
            nameEnglish = "United Arab Emirates",
            flagEmoji = "🇦🇪",
            group = CountryGroup.GCC,
            capital = "أبوظبي",
            searchTerms = listOf("الإمارات", "أبوظبي", "دبي", "UAE", "Emirates", "Abu Dhabi", "Dubai")
        ),
        CountryItem(
            id = "qatar",
            nameArabic = "دولة قطر",
            nameEnglish = "Qatar",
            flagEmoji = "🇶🇦",
            group = CountryGroup.GCC,
            capital = "الدوحة",
            searchTerms = listOf("قطر", "الدوحة", "Qatar", "Doha")
        ),
        CountryItem(
            id = "kuwait",
            nameArabic = "دولة الكويت",
            nameEnglish = "Kuwait",
            flagEmoji = "🇰🇼",
            group = CountryGroup.GCC,
            capital = "مدينة الكويت",
            searchTerms = listOf("الكويت", "Kuwait")
        ),
        CountryItem(
            id = "oman",
            nameArabic = "سلطنة عُمان",
            nameEnglish = "Oman",
            flagEmoji = "🇴🇲",
            group = CountryGroup.GCC,
            capital = "مسقط",
            searchTerms = listOf("عمان", "سلطنة عمان", "مسقط", "Oman", "Muscat")
        ),
        CountryItem(
            id = "bahrain",
            nameArabic = "مملكة البحرين",
            nameEnglish = "Bahrain",
            flagEmoji = "🇧🇭",
            group = CountryGroup.GCC,
            capital = "المنامة",
            searchTerms = listOf("البحرين", "المنامة", "Bahrain", "Manama")
        )
    )

    fun findCountryById(id: String): CountryItem? = list.firstOrNull { it.id == id }
}
