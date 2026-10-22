package com.kexu.app.importer

enum class EducationSystem(val label: String) {
    AUTO("自动识别"),
    QIANGZHI("强智教务"),
    ZHENGFANG("正方教务"),
    URP("URP 教务"),
    QINGGUO("青果教务"),
    UNKNOWN("其他教务"),
}

data class SectionTime(val startMinutes: Int, val endMinutes: Int)

data class SchoolSpec(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val loginUrl: String,
    val allowedDomainSuffixes: List<String>,
    val system: EducationSystem,
    val sectionTimes: Map<Int, SectionTime> = emptyMap(),
    val verified: Boolean = false,
) {
    fun allows(url: String): Boolean {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return false
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
        return allowedDomainSuffixes.any { rawSuffix ->
            val suffix = rawSuffix.lowercase().trim().trimStart('.').trimEnd('.')
            host == suffix || host.endsWith(".$suffix")
        }
    }
}

object SchoolCatalog {
    val builtIn = listOf(
        SchoolSpec(
            id = "UPC",
            name = "中国石油大学（华东）",
            aliases = listOf("石大", "中石大华东", "UPC", "zhongguoshiyoudaxue"),
            // Match the import flow used by general timetable apps: open only the
            // official university homepage and let the user navigate from there.
            loginUrl = "https://www.upc.edu.cn/",
            allowedDomainSuffixes = listOf("upc.edu.cn"),
            system = EducationSystem.QIANGZHI,
            verified = true,
            sectionTimes = mapOf(
                1 to SectionTime(8 * 60, 8 * 60 + 45),
                2 to SectionTime(8 * 60 + 50, 9 * 60 + 35),
                3 to SectionTime(9 * 60 + 55, 10 * 60 + 40),
                4 to SectionTime(10 * 60 + 45, 11 * 60 + 30),
                5 to SectionTime(11 * 60 + 35, 12 * 60 + 20),
                6 to SectionTime(14 * 60, 14 * 60 + 45),
                7 to SectionTime(14 * 60 + 50, 15 * 60 + 35),
                8 to SectionTime(15 * 60 + 55, 16 * 60 + 40),
                9 to SectionTime(16 * 60 + 45, 17 * 60 + 30),
                10 to SectionTime(19 * 60, 19 * 60 + 45),
                11 to SectionTime(19 * 60 + 50, 20 * 60 + 35),
                12 to SectionTime(20 * 60 + 40, 21 * 60 + 25),
            ),
        ),
        autoSchool("TSINGHUA", "清华大学", listOf("清华", "THU", "tsinghua"), "tsinghua.edu.cn"),
        autoSchool("PKU", "北京大学", listOf("北大", "PKU", "beida"), "pku.edu.cn"),
        autoSchool("RUC", "中国人民大学", listOf("人大", "RUC"), "ruc.edu.cn"),
        autoSchool("BUAA", "北京航空航天大学", listOf("北航", "BUAA"), "buaa.edu.cn"),
        autoSchool("BIT", "北京理工大学", listOf("北理工", "BIT"), "bit.edu.cn"),
        autoSchool("BNU", "北京师范大学", listOf("北师大", "BNU"), "bnu.edu.cn"),
        autoSchool("BUPT", "北京邮电大学", listOf("北邮", "BUPT"), "bupt.edu.cn"),
        autoSchool("NKU", "南开大学", listOf("南开", "NKU"), "nankai.edu.cn"),
        autoSchool("TJU", "天津大学", listOf("天大", "TJU"), "tju.edu.cn"),
        autoSchool("FDU", "复旦大学", listOf("复旦", "FDU"), "fudan.edu.cn"),
        autoSchool("SJTU", "上海交通大学", listOf("上海交大", "SJTU"), "sjtu.edu.cn"),
        autoSchool("TONGJI", "同济大学", listOf("同济", "Tongji"), "tongji.edu.cn"),
        autoSchool("ECNU", "华东师范大学", listOf("华东师大", "ECNU"), "ecnu.edu.cn"),
        autoSchool("ECUST", "华东理工大学", listOf("华理", "ECUST"), "ecust.edu.cn"),
        autoSchool("ZJU", "浙江大学", listOf("浙大", "ZJU"), "zju.edu.cn"),
        autoSchool("NJU", "南京大学", listOf("南大", "NJU"), "nju.edu.cn"),
        autoSchool("SEU", "东南大学", listOf("东大", "SEU"), "seu.edu.cn"),
        autoSchool("USTC", "中国科学技术大学", listOf("中科大", "USTC"), "ustc.edu.cn"),
        autoSchool("XMU", "厦门大学", listOf("厦大", "XMU"), "xmu.edu.cn"),
        autoSchool("SDU", "山东大学", listOf("山大", "SDU"), "sdu.edu.cn"),
        autoSchool("OUC", "中国海洋大学", listOf("海大", "OUC"), "ouc.edu.cn"),
        autoSchool("WHU", "武汉大学", listOf("武大", "WHU"), "whu.edu.cn"),
        autoSchool("HUST", "华中科技大学", listOf("华科", "HUST"), "hust.edu.cn"),
        autoSchool("HNU", "湖南大学", listOf("湖大", "HNU"), "hnu.edu.cn"),
        autoSchool("CSU", "中南大学", listOf("中南", "CSU"), "csu.edu.cn"),
        autoSchool("SYSU", "中山大学", listOf("中大", "SYSU"), "sysu.edu.cn"),
        autoSchool("SCUT", "华南理工大学", listOf("华工", "SCUT"), "scut.edu.cn"),
        autoSchool("SCU", "四川大学", listOf("川大", "SCU"), "scu.edu.cn"),
        autoSchool("UESTC", "电子科技大学", listOf("成电", "电子科大", "UESTC"), "uestc.edu.cn"),
        autoSchool("CQU", "重庆大学", listOf("重大", "CQU"), "cqu.edu.cn"),
        autoSchool("XJTU", "西安交通大学", listOf("西交", "XJTU"), "xjtu.edu.cn"),
        autoSchool("NWPU", "西北工业大学", listOf("西工大", "NWPU"), "nwpu.edu.cn"),
        autoSchool("XIDIAN", "西安电子科技大学", listOf("西电", "XDU"), "xidian.edu.cn"),
        autoSchool("LZU", "兰州大学", listOf("兰大", "LZU"), "lzu.edu.cn"),
        autoSchool("HIT", "哈尔滨工业大学", listOf("哈工大", "HIT"), "hit.edu.cn"),
        autoSchool("JLU", "吉林大学", listOf("吉大", "JLU"), "jlu.edu.cn"),
        autoSchool("NEU", "东北大学", listOf("东大", "NEU"), "neu.edu.cn"),
        autoSchool("DLUT", "大连理工大学", listOf("大工", "DLUT"), "dlut.edu.cn"),
    )

    fun search(query: String): List<SchoolSpec> {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) return builtIn
        return builtIn.filter { school ->
            school.name.lowercase().contains(needle) ||
                school.aliases.any { it.lowercase().contains(needle) }
        }
    }

    fun custom(name: String, url: String, system: EducationSystem): SchoolSpec? {
        val normalized = url.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
        val host = runCatching { java.net.URI(normalized).host }.getOrNull() ?: return null
        return SchoolSpec(
            id = "custom-${host.lowercase()}",
            name = name.trim().ifBlank { host },
            aliases = emptyList(),
            loginUrl = normalized,
            allowedDomainSuffixes = listOf(host),
            system = system,
        )
    }

    private fun autoSchool(
        id: String,
        name: String,
        aliases: List<String>,
        domain: String,
    ) = SchoolSpec(
        id = id,
        name = name,
        aliases = aliases,
        loginUrl = "https://www.$domain/",
        allowedDomainSuffixes = listOf(domain),
        system = EducationSystem.AUTO,
    )
}
