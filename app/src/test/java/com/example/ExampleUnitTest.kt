package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSupportedCountriesGroups() {
    val p5 = com.example.data.model.SupportedCountries.list.filter {
      it.group == com.example.data.model.CountryGroup.SECURITY_COUNCIL_P5
    }
    val gcc = com.example.data.model.SupportedCountries.list.filter {
      it.group == com.example.data.model.CountryGroup.GCC
    }

    assertEquals("P5 must have exactly 5 countries", 5, p5.size)
    assertEquals("GCC must have exactly 6 countries", 6, gcc.size)
    assertEquals("Total countries must be 11", 11, com.example.data.model.SupportedCountries.list.size)

    // Verify articles exist for every country
    com.example.data.model.SupportedCountries.list.forEach { country ->
      val articles = com.example.data.remote.CountryCuratedNews.getArticlesForCountry(country)
      assertTrue("Country ${country.nameArabic} must have news articles", articles.isNotEmpty())
    }
  }
}
