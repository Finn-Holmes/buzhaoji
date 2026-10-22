package com.kexu.app.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolCatalogTest {
    @Test
    fun `search supports Chinese alias and English abbreviation`() {
        assertEquals("UPC", SchoolCatalog.search("石大").single().id)
        assertEquals("UPC", SchoolCatalog.search("upc").single().id)
    }

    @Test
    fun `catalog distinguishes verified schools from auto detection entries`() {
        val upc = SchoolCatalog.builtIn.single { it.id == "UPC" }
        val zju = SchoolCatalog.search("浙大").single()

        assertTrue(SchoolCatalog.builtIn.size >= 30)
        assertTrue(upc.verified)
        assertEquals(EducationSystem.QIANGZHI, upc.system)
        assertFalse(zju.verified)
        assertEquals(EducationSystem.AUTO, zju.system)
    }

    @Test
    fun `custom school normalizes address and restricts navigation to its host`() {
        val school = requireNotNull(SchoolCatalog.custom("示例大学", "jw.example.edu.cn/login", EducationSystem.ZHENGFANG))

        assertEquals("https://jw.example.edu.cn/login", school.loginUrl)
        assertTrue(school.allows("https://jw.example.edu.cn/xskb"))
        assertFalse(school.allows("https://evil-example.edu.cn/xskb"))
        assertFalse(school.allows("javascript:alert(1)"))
    }

    @Test
    fun `built in school allows sibling services under reviewed suffix`() {
        val upc = SchoolCatalog.builtIn.single { it.id == "UPC" }

        assertEquals("https://www.upc.edu.cn/", upc.loginUrl)
        assertTrue(upc.allows("https://jwxt.upc.edu.cn/jsxsd/xskb/xskb_list.do"))
        assertTrue(upc.allows("https://jwxt-443.webvpn.upc.edu.cn/jsxsd/xskb/xskb_list.do"))
        assertFalse(upc.allows("https://upc.edu.cn.attacker.example/login"))
    }

    @Test
    fun `each known education system has a dedicated parser plus generic fallback`() {
        val expectedMarkers = mapOf(
            EducationSystem.QIANGZHI to "kbcontent",
            EducationSystem.ZHENGFANG to "kbgrid_table_0",
            EducationSystem.URP to "class_div",
            EducationSystem.QINGGUO to "qingguo-table-v1",
        )

        expectedMarkers.forEach { (system, marker) ->
            val scripts = AcademicWebAdapter.scriptsFor(system)
            assertEquals(2, scripts.size)
            assertTrue(scripts.first().contains(marker))
            assertTrue(scripts.last().contains("generic-table-v2"))
        }
        val automaticScripts = AcademicWebAdapter.scriptsFor(EducationSystem.AUTO)
        assertEquals(5, automaticScripts.size)
        expectedMarkers.values.forEach { marker -> assertTrue(automaticScripts.any { it.contains(marker) }) }
        assertTrue(automaticScripts.last().contains("generic-table-v2"))
        assertEquals(1, AcademicWebAdapter.scriptsFor(EducationSystem.UNKNOWN).size)
    }

    @Test
    fun `qiangzhi parser reads repeated font field groups and removes duplicates`() {
        val script = AcademicWebAdapter.qiangzhiExtractionScript

        assertTrue(script.contains("qiangzhi-candidate-ranking-v3"))
        assertTrue(script.contains("weekFont.getAttribute('title')"))
        assertTrue(script.contains("const seen = new Set()"))
        assertTrue(script.contains("weekInfo.includes('单周')"))
        assertTrue(script.contains("weekInfo.includes('双周')"))
        assertTrue(script.contains("candidates.sort"))
        assertTrue(script.contains("timetableGrid"))
    }

}
