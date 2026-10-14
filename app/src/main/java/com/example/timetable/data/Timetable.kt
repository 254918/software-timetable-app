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

    /** 课程颜色：按名称分配调色板（统一加深，保证卡片白字对比度） */
    val PALETTE = listOf(
        0xFF1D4ED8.toInt(), // 蓝（深）
        0xFF155E75.toInt(), // 青（深）
        0xFF15803D.toInt(), // 绿（深）
        0xFFC2410C.toInt(), // 橙（深）
        0xFFBE185D.toInt(), // 品红（深）
        0xFF92400E.toInt(), // 棕（深）
        0xFF475569.toInt(), // 灰（深）
        0xFFB91C1C.toInt()  // 红（深）
    )

    fun colorOf(name: String): Int {
        var h = 0
        for (ch in name) h = h * 31 + ch.code
        return PALETTE[Math.floorMod(h, PALETTE.size)]
    }
}
