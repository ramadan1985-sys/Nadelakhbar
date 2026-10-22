package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CountryItem
import com.example.data.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiNewsService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchTodayWires(category: String? = null, region: String? = null): List<NewsArticle> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No valid GEMINI_API_KEY, returning initial curated wire feed.")
            return@withContext getCuratedSampleWires(category, region)
        }

        val prompt = buildString {
            append("أنت وكيل أخبار استقصائي ذكي يعمل كوكالة أنباء عالمية مفتوحة (مثل OpenNewsWire). ")
            append("ابحث في الأخبار والتقارير العالمية العاجلة المنشورة اليوم حول العالم. ")
            if (!category.isNullOrBlank() && category != "كافة الأسلاك") {
                append("ركز على فئة: $category. ")
            }
            if (!region.isNullOrBlank() && region != "العالم أجمع") {
                append("ركز على منطقة أو نطاق: $region. ")
            }
            append("قم بجمع 6 إلى 8 مقالات إخبارية حصرية ومهمة جداً من وكالات الأنباء الدولية (مثل رويترز، أسوشيتد برس، فرانس برس، بلومبرغ، بي بي سي، شينخوا، دويتشه فيله، لوموند). ")
            append("ترجم كل الأخبار بالكامل إلى لغة عربية صحفية رصينة ودقيقة. ")
            append("الأهم: اكتب نص الخبر والمقال بشكل كامل ومفصل (بين 250 إلى 400 كلمة لكل مقال مع تفاصيل وتصريحات وخلفيات) حتى يتمكن القارئ من قراءة التقرير الشامل كاملاً داخل التطبيق بدون الحاجة للانتقال إلى أي موقع خارجي أو مواجهة جدران دفع (paywalls). ")
            append("قم بصياغة الرد على شكل JSON مطابق تماماً للنموذج التالي بدون أي نصوص قبل أو بعد كود الـ JSON:\n")
            append("""
                [
                  {
                    "title": "عنوان رئيسي جذاب ودقيق بالعربية",
                    "summary": "موجز شامل للخبر في فقرة موجزة",
                    "content": "النص الكامل للمقال والتقرير الإخباري المفصل بالعربية مع كافة التصريحات والتفاصيل والخلفيات...",
                    "sourceName": "اسم الوكالة الأصلية (مثل: رويترز / لندن)",
                    "originalLanguage": "اللغة الأصلية للمصدر (مثل: الإنجليزية)",
                    "country": "الدولة أو المنطقة المعنية",
                    "category": "الفئة (سياسة / اقتصاد / تكنولوجيا / رياضة / علوم وصحة)",
                    "publishedAt": "اليوم 15:30",
                    "keyPoints": ["نقطة رئيسية 1", "نقطة رئيسية 2", "نقطة رئيسية 3"],
                    "aiAnalysis": "تحليل وسياق الوكيل الذكي لتداعيات هذا الحدث عالمياً",
                    "isBreaking": true
                  }
                ]
            """.trimIndent())
        }

        try {
            val responseText = executeGeminiRequest(prompt, apiKey, enableSearch = true)
            val articles = parseArticlesFromJson(responseText)
            if (articles.isNotEmpty()) {
                articles
            } else {
                getCuratedSampleWires(category, region)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live wires: ${e.message}", e)
            getCuratedSampleWires(category, region)
        }
    }

    suspend fun searchWiresAdvanced(
        keywords: String,
        category: String,
        dateRange: String,
        region: String
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Using fallback search due to missing API key")
            return@withContext filterCuratedWires(keywords, category, region)
        }

        val prompt = buildString {
            append("أنت وكيل بحث استقصائي في وكالات الأخبار العالمية المفتوحة. ")
            append("المستخدم يجري بحثاً متقدماً عن مقالات وتقارير إخبارية بالمعايير التالية:\n")
            if (keywords.isNotBlank()) append("- الكلمات المفتاحية: \"$keywords\"\n")
            if (category.isNotBlank() && category != "كافة الأسلاك") append("- فئة الخبر: $category\n")
            if (dateRange.isNotBlank() && dateRange != "كافة التواريخ") append("- النطاق الزمني للتغطية: $dateRange\n")
            if (region.isNotBlank() && region != "العالم أجمع") append("- النطاق الجغرافي / الدولة: $region\n")
            append("\nقم بالبحث في وكالات الأنباء المعتمدة حول العالم عن أكثر 5 إلى 7 تقارير مطابقة لهذه المعايير بدقة. ")
            append("ترجم كل الأخبار بالكامل إلى اللغة العربية. ")
            append("اكتب نص المقال الكامل والشامل (300+ كلمة) حتى يتمكن المستخدم من قراءته بالكامل داخل التطبيق بدون توجيه لمصادر مقفلة أو اشتراكات. ")
            append("قم بالرد بصيغة مصفوفة JSON حصراً:\n")
            append("""
                [
                  {
                    "title": "العنوان بالعربية",
                    "summary": "ملخص الخبر",
                    "content": "النص الكامل للمقال بالتفصيل...",
                    "sourceName": "اسم الوكالة الأصلية والدولة",
                    "originalLanguage": "اللغة الأصلية",
                    "country": "الدولة",
                    "category": "الفئة",
                    "publishedAt": "تاريخ وتوقيت النشر",
                    "keyPoints": ["نقطة 1", "نقطة 2", "نقطة 3"],
                    "aiAnalysis": "تحليل الوكيل الذكي لأبعاد البحث والنتائج",
                    "isBreaking": false
                  }
                ]
            """.trimIndent())
        }

        try {
            val responseText = executeGeminiRequest(prompt, apiKey, enableSearch = true)
            val articles = parseArticlesFromJson(responseText)
            if (articles.isNotEmpty()) {
                articles
            } else {
                filterCuratedWires(keywords, category, region)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing advanced search: ${e.message}", e)
            filterCuratedWires(keywords, category, region)
        }
    }

    suspend fun fetchCountryNews(country: CountryItem): List<NewsArticle> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Using curated country articles for ${country.nameArabic}")
            return@withContext CountryCuratedNews.getArticlesForCountry(country)
        }

        val prompt = buildString {
            append("أنت وكيل أخبار استقصائي ومترجم فوري لوكالات الأنباء المفتوحة (OpenNewsWire). ")
            append("المستخدم اختار قراءة آخر الأخبار والتقارير المنشورة اليوم الخاصة بدولة: ${country.nameArabic} (${country.nameEnglish} - العاصمة: ${country.capital}). ")
            append("ابحث في وكالات الأنباء المعتمدة حول العالم (مثل رويترز، أسوشيتد برس، فرانس برس، بلومبرغ، شينخوا، بي بي سي، ووكالات الأنباء الرسمية للدولة) ")
            append("عن أهم 5 إلى 6 مقالات إخبارية حصرية منشورة اليوم تدور حول شؤون هذه الدولة وسياستها واقتصادها ومشاريعها وعلاقاتها الدولية. ")
            append("ترجم كل الأخبار بالكامل إلى لغة عربية صحفية رصينة ومحترفة. ")
            append("واكتب نص كل تقرير بالكامل وبشكل مفصل (300+ كلمة لكل مقال مع تصريحات وخلفيات) حتى يتمكن القارئ من قراءته بالكامل داخل التطبيق بدون أي توجيه خارجي أو جدران دفع. ")
            append("قم بصياغة الرد على شكل مصفوفة JSON حصراً:\n")
            append("""
                [
                  {
                    "title": "عنوان الخبر بالعربية",
                    "summary": "موجز التقرير",
                    "content": "النص الكامل للمقال والتقرير بالتفصيل بالعربية...",
                    "sourceName": "اسم الوكالة الأصلية والدولة",
                    "originalLanguage": "اللغة الأصلية",
                    "country": "${country.nameArabic}",
                    "category": "الفئة (سياسة / اقتصاد / تكنولوجيا / ثقافة / رياضة)",
                    "publishedAt": "اليوم",
                    "keyPoints": ["نقطة رئيسية 1", "نقطة 2", "نقطة 3"],
                    "aiAnalysis": "تحليل وسياق الوكيل الذكي لأهمية هذا الخبر لدولة ${country.nameArabic}",
                    "isBreaking": false
                  }
                ]
            """.trimIndent())
        }

        try {
            val responseText = executeGeminiRequest(prompt, apiKey, enableSearch = true)
            val articles = parseArticlesFromJson(responseText)
            if (articles.isNotEmpty()) {
                articles
            } else {
                CountryCuratedNews.getArticlesForCountry(country)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching country wires for ${country.nameArabic}: ${e.message}", e)
            CountryCuratedNews.getArticlesForCountry(country)
        }
    }

    suspend fun askAgentAboutArticle(
        articleTitle: String,
        articleContent: String,
        userQuestion: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "وفقاً لمحتوى التقرير، فإن هذا الحدث يرتبط بتحولات جيوسياسية واقتصادية راهنة. (يمكنك ربط مفتاح Gemini API في إعدادات التطبيق للحصول على إجابات فورية عميقة من الوكيل الذكي)."
        }

        val prompt = buildString {
            append("أنت وكيل الأخبار الذكي المتخصص في شؤون وكالات الأنباء الدولية والتحليل الجيوسياسي والاقتصادي. ")
            append("القارئ يقرأ المقال التالي:\n")
            append("العنوان: $articleTitle\n")
            append("محتوى التقرير: $articleContent\n\n")
            append("سؤال القارئ حول هذا المقال هو:\n")
            append("\"$userQuestion\"\n\n")
            append("قدم إجابة باللغة العربية واضحة، موضوعية، متعمقة ومبنية على الحقائق الصحفية والسياق الدولي.")
        }

        try {
            val response = executeGeminiRequest(prompt, apiKey, enableSearch = false)
            response.trim()
        } catch (e: Exception) {
            "تعذر الحصول على إجابة الوكيل حالياً: ${e.localizedMessage}"
        }
    }

    private fun executeGeminiRequest(prompt: String, apiKey: String, enableSearch: Boolean): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            if (enableSearch) {
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                put("tools", toolsArray)
            }
        }

        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw IllegalStateException("API error ${response.code}: $responseBody")
        }

        val parsed = JSONObject(responseBody)
        val candidates = parsed.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            sb.append(part.optString("text"))
        }
        return sb.toString()
    }

    private fun parseArticlesFromJson(rawResponse: String): List<NewsArticle> {
        val list = mutableListOf<NewsArticle>()
        try {
            var cleanText = rawResponse.trim()
            if (cleanText.contains("```json")) {
                cleanText = cleanText.substringAfter("```json").substringBefore("```").trim()
            } else if (cleanText.contains("```")) {
                cleanText = cleanText.substringAfter("```").substringBefore("```").trim()
            }

            val startIndex = cleanText.indexOf('[')
            val endIndex = cleanText.lastIndexOf(']')
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleanText = cleanText.substring(startIndex, endIndex + 1)
            }

            val jsonArray = JSONArray(cleanText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val keyPointsList = mutableListOf<String>()
                val pointsArray = obj.optJSONArray("keyPoints")
                if (pointsArray != null) {
                    for (j in 0 until pointsArray.length()) {
                        keyPointsList.add(pointsArray.getString(j))
                    }
                }

                list.add(
                    NewsArticle(
                        id = UUID.randomUUID().toString(),
                        title = obj.optString("title", "خبر عاجل"),
                        summary = obj.optString("summary", ""),
                        content = obj.optString("content", ""),
                        sourceName = obj.optString("sourceName", "وكالة أنباء عالمية"),
                        originalLanguage = obj.optString("originalLanguage", "الإنجليزية"),
                        country = obj.optString("country", "عالمي"),
                        category = obj.optString("category", "سياسة وعلاقات دولية"),
                        publishedAt = obj.optString("publishedAt", "اليوم"),
                        keyPoints = keyPointsList,
                        aiAnalysis = obj.optString("aiAnalysis", ""),
                        isBreaking = obj.optBoolean("isBreaking", false),
                        isBookmarked = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON articles: ${e.message}")
        }
        return list
    }

    private fun filterCuratedWires(keywords: String, category: String, region: String): List<NewsArticle> {
        val all = getCuratedSampleWires()
        return all.filter { article ->
            val matchKeywords = keywords.isBlank() ||
                    article.title.contains(keywords, ignoreCase = true) ||
                    article.summary.contains(keywords, ignoreCase = true) ||
                    article.content.contains(keywords, ignoreCase = true)
            val matchCategory = category.isBlank() || category == "كافة الأسلاك" ||
                    article.category.contains(category, ignoreCase = true)
            val matchRegion = region.isBlank() || region == "العالم أجمع" ||
                    article.country.contains(region, ignoreCase = true)
            matchKeywords && matchCategory && matchRegion
        }
    }

    private fun getCuratedSampleWires(categoryFilter: String? = null, regionFilter: String? = null): List<NewsArticle> {
        val list = listOf(
            NewsArticle(
                id = "wire-1",
                title = "قمة عالمية للمناخ تطلق اتفاقية جديدة لحماية مصادر الطاقة النظيفة والتنوع البيولوجي",
                summary = "اتفقت أكثر من 60 دولة اليوم في باريس على خطة تمويلية عاجلة لتسريع التحول نحو شبكات الطاقة المتجددة وحماية الغابات الاستوائية.",
                content = """
                    باريس - اتفقت أكثر من 60 دولة من مختلف قارات العالم اليوم الأربعاء على حزمة تمويل استثنائية بقيمة 85 مليار دولار تهدف لدعم البنية التحتية للطاقة النظيفة في الدول النامية وحماية التنوع البيولوجي.

                    وشهدت الجلسات الافتتاحية للمؤتمر الدولي نقاشات موسعة حول ضرورة إلزام الشركات الصناعية الكبرى بخفض الانبعاثات الكربونية بنسبة لا تقل عن 40% بحلول عام 2030. وجاء في البيان الختامي الصادر عن الرئاسة المشتركة للمؤتمر أن العالم لم يعد يملك رفاهية الانتظار أمام التسارع غير المسبوق في درجات الحرارة العالمية وظواهر الجفاف والفيضانات التي تضرب سلاسل التوريد الزراعية العالمية.

                    وقال ممثل الاتحاد الأوروبي في كلمته إن الاستثمار في شبكات الكهرباء الذكية والهيدروجين الأخضر يشكل ركيزة للأمن القومي الاقتصادي، مشيراً إلى أن الدول النامية ستحصل على تسهيلات بنكية وضمانات ائتمانية مباشرة من الصناديق السيادية لتنفيذ مشروعات الطاقة الشمسية والرياح.

                    من جانبها، أكدت وفود الدول المشاركة من آسيا وأفريقيا على ضرورة الوفاء بالتعهدات التمويلية دون فرض شروط تقيد النمو الاقتصادي للأسواق الناشئة، مع التركيز على نقل التكنولوجيا الصناعية الصديقة للبيئة.
                """.trimIndent(),
                sourceName = "رويترز (Reuters) - باريس",
                originalLanguage = "الإنجليزية والفرنسية",
                country = "فرنسا / الاتحاد الأوروبي",
                category = "علوم وصحة وبيئة",
                publishedAt = "اليوم 14:15",
                keyPoints = listOf(
                    "تخصيص 85 مليار دولار لصناديق الطاقة المتجددة العالمية",
                    "التزام بخفض الانبعاثات الصناعية 40% قبل 2030",
                    "دعم مشروعات الهيدروجين الأخضر وشبكات الربط الكهربائي القاري"
                ),
                aiAnalysis = "يعكس هذا الاتفاق تحولاً نوعياً من مجرد التعهدات النظرية إلى حزم تمويلية مرتبطة بضمانات سيادية، مما يخفف من مخاوف الأسواق الناشئة حيال كلفة التحول الأخضر.",
                isBreaking = true
            ),
            NewsArticle(
                id = "wire-2",
                title = "قفزة نوعية في معالجات الذكاء الاصطناعي من الجيل الجديد تخفض استهلاك الطاقة بنسبة 60%",
                summary = "كشف تحالف من مراكز الأبحاث في طوكيو وسيليكون فالي عن بنية جديدة للرقائق الإلكترونية تعتمد على الحوسبة الضوئية متناهية الصغر.",
                content = """
                    طوكيو - أعلن تحالف تقني دولي يضم مهندسين من معهد طوكيو للتكنولوجيا وشركات وادي السيليكون اليوم عن تصنيع أول شريحة معالجة ضوئية متكاملة قادرة على تدريب نماذج الذكاء الاصطناعي الفائقة باستهلاك طاقة يقل بنسبة 60% مقارنة بالمعالجات السيليكونية التقليدية.

                    وتعتمد التقنية الثورية على نقل البيانات عبر نبضات فوتونية متناهية السرعة داخل قلب المعالج بدلاً من الأسلاك النحاسية، مما يزيل الاختناقات الحرارية التي كانت تمثل العائق الأكبر أمام مراكز البيانات العملاقة التي تستهلك حالياً نسباً متزايدة من شبكات الطاقة الوطنية.

                    وصرح البروفيسور كينجي ساتو، المشرف على الفريق البحثي، بأن هذا الإنجاز سيمكن الهواتف والأجهزة الذكية المحمولة من تشغيل نماذج الذكاء الاصطناعي التوليدي المعقدة محلياً دون الحاجة إلى الاتصال المستمر بالسحابة، مما يوفر خصوصية تامة للمستخدمين وسرعة استجابة فائقة.

                    وتوقعت مصادر اقتصادية في بورصة طوكيو أن يبدأ الإنتاج التجاري التجريبي للشرائح الجديدة خلال النصف الأول من العام القادم، مما قد يعيد رسم خريطة المنافسة في صناعة أشباه الموصلات بين آسيا وأمريكا الشمالية.
                """.trimIndent(),
                sourceName = "بلومبرغ (Bloomberg) / نيكي اليابانية",
                originalLanguage = "اليابانية والإنجليزية",
                country = "اليابان والولايات المتحدة",
                category = "تكنولوجيا وذكاء اصطناعي",
                publishedAt = "اليوم 13:40",
                keyPoints = listOf(
                    "استبدال النواقل النحاسية بنبضات فوتونية عالية السرعة",
                    "خفض استهلاك الطاقة في مراكز البيانات بنسبة 60%",
                    "إمكانية تشغيل النماذج الضخمة على الأجهزة المحلية بدون اتصال سحابي"
                ),
                aiAnalysis = "تحل هذه الرقائق معضلة أزمة الطاقة الخانقة التي تواجه مراكز بيانات الذكاء الاصطناعي عالمياً، وتفتح الباب لجيل جديد من الأجهزة الذكية المستقلة.",
                isBreaking = false
            ),
            NewsArticle(
                id = "wire-3",
                title = "البنوك المركزية الكبرى تلمح لتثبيت أسعار الفائدة وسط تراجع التضخم الأساسي واستقرار أسواق العمل",
                summary = "أشارت تقارير صادرة عن البنك المركزي الأوروبي ومجلس الاحتياطي الفيدرالي إلى اقتراب دورة التيسير النقدي بعد تباطؤ وتيرة ارتفاع الأسعار.",
                content = """
                    فرانكفورت - عززت البيانات الاقتصادية الصادرة اليوم الأربعاء التوقعات باقتراب مرحلة جديدة في السياسات النقدية العالمية، حيث أظهرت مؤشرات مديري المشتريات ومعدلات التضخم الأساسي في منطقة اليورو والولايات المتحدة تراجعاً مطرداً نحو المستهدف البالغ 2%.

                    وفي مؤتمر صحفي في فرانكفورت، أكد كبار مسؤولي البنك المركزي أن الضغوط السعرية في قطاع الخدمات بدأت تشهد انحساراً ملحوظاً، بالتوازي مع تحسن ملحوظ في سلاسل الإمداد العالمية واستقرار أسعار الشحن البحري عبر الممرات الرئيسية.

                    وسجلت أسواق الأسهم العالمية مكاسب متباينة عقب صدور التقرير، إذ ارتفع مؤشر داو جونز الصناعي ومؤشر ستوكس 600 الأوروبي، بينما شهدت عوائد سندات الخزانة لأجل عشر سنوات انخفاضاً انعكس إيجاباً على تكلفة الاقتراض للشركات والرهون العقارية.

                    من جهتهم، حذر خبراء صندوق النقد الدولي من أن التوترات التجارية العالمية قد تظل تشكل عاملاً مفاجئاً قد يفرض على البنوك المركزية التريث قبل اتخاذ قرارات متتالية لخفض الفائدة لحماية قيمة العملات واستقرار التدفقات المالية.
                """.trimIndent(),
                sourceName = "فاينانشال تايمز (FT) - لندن",
                originalLanguage = "الإنجليزية",
                country = "المملكة المتحدة والاتحاد الأوروبي",
                category = "مال وأعمال واقتصاد",
                publishedAt = "اليوم 12:20",
                keyPoints = listOf(
                    "اقتراب التضخم الأساسي من مستهدف 2% في الاقتصادات المتقدمة",
                    "انخفاض عوائد السندات وتراجع تكلفة الائتمان التجاري",
                    "تحذيرات من تداعيات اضطرابات سلاسل الشحن على القرارات المستقبلية"
                ),
                aiAnalysis = "التيسير النقدي المتوقع سيمنح الأسواق الناشئة متنفساً مالياً عبر تخفيف أعباء خدمة الديون الدولارية وتشجيع تدفقات الاستثمار الأجنبي المباشر.",
                isBreaking = false
            ),
            NewsArticle(
                id = "wire-4",
                title = "مباحثات دبلوماسية مكثفة لتأمين ممرات التجارة البحرية واستقرار خطوط الشحن الدولية",
                summary = "عقد ممثلون عن 25 دولة ومنظمة بحرية اجتماعاً طارئاً لتعزيز أمن السفن التجارية وضمان سلاسة تدفق السلع الاستراتيجية.",
                content = """
                    جنيف - انطلقت اليوم في مقر المنظمة البحرية الدولية مباحثات أمنية ودبلوماسية موسعة بمشاركة كبرى شركات الملاحة العالمية وممثلي وزارات النقل والخارجية لمناقشة خطة حماية موحدة للممرات المائية الحيوية وسلاسل نقل الحبوب والنفط.

                    وركزت المداولات على ضرورة تحديث بروتوكولات التأمين البحري وتسيير دوريات استطلاع دولية مشتركة لضمان عدم توقف حركة الملاحة في المضائق الاستراتيجية، الأمر الذي كلف التجارة الدولية مليارات الدولارات على مدار الشهور الماضية نتيجة تغيير مسارات السفن عبر رأس الرجاء الصالح.

                    وأكد البيان الصادر عن الاجتماع أن حرية الملاحة وسلامة الطواقم البحرية تمثل مبدأً دولياً غير قابل للتفاوض، مشدداً على الالتزام باتفاقيات الأمم المتحدة لقانون البحار وتفعيل آليات الإنذار المبكر والتنسيق اللوجستي الفوري بين الموانئ والمراكز البحرية.
                """.trimIndent(),
                sourceName = "أسوشيتد برس (Associated Press) - جنيف",
                originalLanguage = "الإنجليزية",
                country = "سويسرا والمياه الدولية",
                category = "سياسة وعلاقات دولية",
                publishedAt = "اليوم 11:05",
                keyPoints = listOf(
                    "خطة دولية موحدة لتأمين خطوط الشحن والناقلات التجارية",
                    "تحديث بروتوكولات التأمين وتفعيل التنسيق الاستخباري الملاحي",
                    "تفادي تكاليف الالتفاف البحري التي ترفع أسعار السلع الاستهلاكية"
                ),
                aiAnalysis = "يعتبر استقرار الممرات البحرية حجر الزاوية في ضبط معدلات التضخم العالمية، وأي تعطيل فيها ينعكس فوراً على أسعار المواد الغذائية والوقود في الأسواق المحلية.",
                isBreaking = true
            ),
            NewsArticle(
                id = "wire-5",
                title = "اكتشاف طبي واعد لعلاج مناعي موجه يقضي على الخلايا السرطانية في مراحل مبكرة دون إتلاف الأنسجة السليمة",
                summary = "أظهرت نتائج المرحلة السريرية الثانية لعقار جيني جديد في جامعة أكسفورد نسب استجابة إيجابية تجاوزت 80% في الأورام الصلبة.",
                content = """
                    لندن - نشرت المجلة الطبية البريطانية اليوم نتائج تجارب سريرية وصفت بالتاريخية لعقار مناعي جديد يعتمد على تعديل مستقبلات الخلايا التائية (T-Cells) بتقنية النانو لتمييز البروتينات الشاذة التي تفرزها الأورام السرطانية بدقة جزيئية متناهية.

                    وأظهرت النتائج التي أجريت على 320 مريضاً في مراحل متقدمة بمستشفيات جامعة أكسفورد ومعهد كارولينسكا بالسويد أن العلاج نجح في تحفيز الجهاز المناعي لتدمير الخلايا الخبيثة بنسبة استجابة كاملة وجزئية بلغت 82%، مع انعدام الآثار الجانبية الخطيرة المرتبطة عادة بالعلاج الكيميائي والإشعاعي التقليدي.

                    وقالت الدكتورة إيلينا مارتينيز، رئيسة الفريق الطبي: 'نحن نقترب من حقبة جديدة يتحول فيها علاج الأورام الخبيثة إلى جرعات مناعية مبرمجة تستهدف الخلل الجزيئي فقط وتترك الجسم في كامل نشاطه ومناعته الطبيعية'. ومن المتوقع تقديم ملف الدواء لاعتماد هيئة الغذاء والدواء الأمريكية ووكالة الأدوية الأوروبية قبل نهاية العام الجاري.
                """.trimIndent(),
                sourceName = "فرانس برس (AFP) / بي بي سي الطبية",
                originalLanguage = "الإنجليزية والفرنسية",
                country = "بريطانيا والسويد",
                category = "علوم وصحة وبيئة",
                publishedAt = "اليوم 09:50",
                keyPoints = listOf(
                    "نسبة استجابة تجاوزت 82% في المرحلة السريرية المتقدمة",
                    "استهداف الخلايا المصابة بدقة جزيئية متناهية بتقنية النانو",
                    "بدء إجراءات الاعتماد الدولي السريع لدى الهيئات الدوائية"
                ),
                aiAnalysis = "يمثل هذا الابتكار نقطة تحول كبرى في الطب الدقيق وعلم الأورام، وقد يسهم في خفض تكاليف الرعاية الصحية الطويلة للأمراض المزمنة بنسب عالية.",
                isBreaking = false
            ),
            NewsArticle(
                id = "wire-6",
                title = "استعدادات مكثفة لانطلاق بطولة كأس العالم للأندية الموسعة بنظامها الجديد بمشاركة 32 فريقاً",
                summary = "الاتحاد الدولي لكرة القدم يضع اللمسات الأخيرة للملاعب ومراكز البث العالمية قبيل انطلاق الحدث الرياضي الأضخم للأندية.",
                content = """
                    ميامي - تتسارع وتيرة التحضيرات في المدن الأمريكية المستضيفة للنسخة الأولى الموسعة من بطولة كأس العالم للأندية المقررة بمشاركة 32 نادياً يمثلون أبطال القارات الست، وسط إقبال جماهيري قياسي على التذاكر واهتمام إعلامي واسع النطاق.

                    وأعلن مسؤولو الاتحاد الدولي لكرة القدم (فيفا) خلال جولة تفقدية لملعب 'هارد روك' في ميامي عن استكمال التجهيزات اللوجستية وتطبيق تقنيات تحكيمية متطورة تشمل كاميرات التسلل شبه الآلي بزوايا 360 درجة، إضافة إلى اختبار شاشات العرض التفاعلية داخل الملاعب ومناطق المشجعين.

                    وتشهد البطولة مشاركة بارزة للأندية العربية وفي مقدمتها الأهلي المصري والهلال السعودي والترجي التونسي والوداد المغربي، إلى جانب عمالقة أوروبا وأمريكا الجنوبية، مما يمنح المسابقة زخماً جماهيرياً كبيراً وترقباً لمواجهات تاريخية بين نخبة لاعبي العالم.
                """.trimIndent(),
                sourceName = "أسوشيتد برس (AP Sports) - ميامي",
                originalLanguage = "الإنجليزية والإسبانية",
                country = "الولايات المتحدة",
                category = "رياضة عالمية",
                publishedAt = "اليوم 08:30",
                keyPoints = listOf(
                    "مشاركة 32 نادياً يمثلون نخبة أبطال القارات للمرة الأولى",
                    "تطبيق تقنيات التسلل شبه الآلي والشاشات التفاعلية للجمهور",
                    "حضور بارز وتنافس مرتقب لكبرى الأندية العربية في البطولة"
                ),
                aiAnalysis = "يعكس النظام الجديد للبطولة رغبة في عولمة المنافسة الكروية للأندية وجذب استثمارات البث وحقوق الرعاية العالمية إلى مستويات غير مسبوقة.",
                isBreaking = false
            )
        )

        return list.filter {
            val matchCat = categoryFilter.isNullOrBlank() || categoryFilter == "كافة الأسلاك" || it.category == categoryFilter
            val matchReg = regionFilter.isNullOrBlank() || regionFilter == "العالم أجمع" || it.country.contains(regionFilter, ignoreCase = true)
            matchCat && matchReg
        }
    }

    companion object {
        private const val TAG = "GeminiNewsService"
    }
}
