package com.example.timetable.data

import java.util.Calendar

/**
 * 课程数据模型，与网页版课表保持一致：
 * - weeks 支持 "1-4,6-14,17-18" / "4-6双" / "9-13单"
 * - day: 1=周一 ... 5=周五
 * - makeup: 调课补上的课
 */
data class Session(
    val weeks: String,
    val start: Int,
    val end: Int,
    val makeup: Boolean = false,
    val note: String? = null
)

data class Course(
    val name: String,
    val day: Int,
    val classes: String? = null,
    val loc: String,
    val teacher: String,
    val sessions: List<Session>
)

object Timetable {

    const val TOTAL_WEEKS = 18
    val DAY_NAMES = listOf("星期一", "星期二", "星期三", "星期四", "星期五")

    /** 解析周次表达式，如 "1-4,6-14,17-18" / "4-6双" / "9-13单" */
    fun parseWeeks(expr: String): Set<Int> {
        val set = HashSet<Int>()
        expr.split(',').forEach { part ->
            val p = part.trim()
            val m = Regex("^(\\d+)-(\\d+)(单|双)?$").find(p)
            if (m != null) {
                val a = m.groupValues[1].toInt()
                val b = m.groupValues[2].toInt()
                val parity = m.groupValues[3]
                for (i in a..b) {
                    if (parity == "单" && i % 2 == 0) continue
                    if (parity == "双" && i % 2 == 1) continue
                    set.add(i)
                }
            } else if (p.toIntOrNull() != null) {
                set.add(p.toInt())
            }
        }
        return set
    }

    /** 某门课在第 week 周的 session，不在该周则为 null */
    fun sessionInWeek(course: Course, week: Int): Session? {
        return course.sessions.firstOrNull { week in parseWeeks(it.weeks) }
    }

    /** 从周次表达式中移除某一周，返回新的表达式；若移除后为空返回 ""（表示该课不再上课） */
    fun weeksWithout(expr: String, w: Int): String {
        val set = parseWeeks(expr)
        if (w !in set) return expr.trim()
        set.remove(w)
        if (set.isEmpty()) return ""
        val list = set.sorted()
        val parts = ArrayList<String>()
        var i = 0
        while (i < list.size) {
            var j = i
            while (j + 1 < list.size && list[j + 1] == list[j] + 1) j++
            parts.add(if (j == i) "${list[i]}" else "${list[i]}-${list[j]}")
            i = j + 1
        }
        return parts.joinToString(",")
    }

    /** 某周某天要上的课 */
    fun coursesOfDay(courses: List<Course>, week: Int, day: Int): List<Pair<Course, Session>> {
        return courses
            .filter { it.day == day }
            .mapNotNull { c -> sessionInWeek(c, week)?.let { c to it } }
    }

    /** 根据学期开始日期(第1周周一)计算今天是第几周；周末按当前周处理 */
    fun currentWeek(termStart: Calendar, now: Calendar = Calendar.getInstance()): Int {
        val start = termStart.clone() as Calendar
        // 对齐到周一零点
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
        val diff = now.timeInMillis - start.timeInMillis
        val days = (diff / (1000L * 60 * 60 * 24)).toInt()
        val week = days / 7 + 1
        return week.coerceIn(1, TOTAL_WEEKS)
    }

    /** 某周的周一日期（用于显示） */
    fun mondayOfWeek(termStart: Calendar, week: Int): Calendar {
        val c = termStart.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, (week - 1) * 7)
        return c
    }

    /** 课程颜色：按首次出现顺序分配调色板（深色调保证白字对比度，16 色色相分散便于区分） */
    val PALETTE = listOf(
        0xFFB91C1C.toInt(), // 红
        0xFFC2410C.toInt(), // 橙
        0xFFB45309.toInt(), // 琥珀
        0xFF4D7C0F.toInt(), // 橄榄绿
        0xFF15803D.toInt(), // 绿
        0xFF0F766E.toInt(), // 青绿
        0xFF155E75.toInt(), // 青
        0xFF0369A1.toInt(), // 天蓝
        0xFF1D4ED8.toInt(), // 蓝
        0xFF4338CA.toInt(), // 靛
        0xFF6D28D9.toInt(), // 紫
        0xFF86198F.toInt(), // 紫红
        0xFFBE185D.toInt(), // 品红
        0xFFA21CAF.toInt(), // 粉紫
        0xFF9A3412.toInt(), // 栗棕
        0xFF475569.toInt()  // 蓝灰
    )

    /** 课程名 → 颜色：按首次出现顺序分配，保证每门课颜色不同且稳定（同课名同色） */
    private val colorCache = LinkedHashMap<String, Int>()

    fun colorOf(name: String): Int {
        colorCache[name]?.let { return it }
        val c = PALETTE[Math.floorMod(colorCache.size, PALETTE.size)]
        colorCache[name] = c
        return c
    }
}
