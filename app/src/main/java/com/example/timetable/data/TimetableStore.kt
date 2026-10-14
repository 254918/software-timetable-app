package com.example.timetable.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * 课表与设置的本地持久化（SharedPreferences + JSON）。
 * 云端 OCR 的百度 Key 只存本地，不上传。
 */
class TimetableStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("timetable", Context.MODE_PRIVATE)

    // ===== 课表 =====

    fun saveCourses(courses: List<Course>) {
        val arr = JSONArray()
        courses.forEach { c ->
            val sArr = JSONArray()
            c.sessions.forEach { s ->
                val so = JSONObject()
                so.put("weeks", s.weeks)
                so.put("start", s.start)
                so.put("end", s.end)
                so.put("makeup", s.makeup)
                if (s.note != null) so.put("note", s.note)
                sArr.put(so)
            }
            val o = JSONObject()
            o.put("name", c.name)
            o.put("day", c.day)
            if (c.classes != null) o.put("classes", c.classes)
            o.put("loc", c.loc)
            o.put("teacher", c.teacher)
            o.put("sessions", sArr)
            arr.put(o)
        }
        prefs.edit().putString("courses_json", arr.toString()).apply()
    }

    fun loadCourses(): List<Course> {
        val raw = prefs.getString("courses_json", null) ?: return emptyList()
        return parseCourses(raw)
    }

    /** 内置默认课表（assets/default_timetable.json），首次安装无课表时使用 */
    fun loadDefaultCourses(): List<Course> {
        // 1) 优先读 assets 打包文件
        try {
            val raw = context.assets.open("default_timetable.json").bufferedReader().use { it.readText() }
            val parsed = parseCourses(raw)
            if (parsed.isNotEmpty()) return parsed
        } catch (e: Exception) {
            // 忽略，走内嵌兜底
        }
        // 2) 内嵌 JSON 兜底（与网页课表数据一致，保证任何情况下初始课表可用）
        return parseCourses(DEFAULT_TIMETABLE_JSON)
    }

    /** 与网页课表（软件开发方向课表全屏版.html）完全一致的内置课表数据，含调课标记 */
    private val DEFAULT_TIMETABLE_JSON = """
[
  {"name":"Web服务器端开发","day":1,"classes":"1、2班","loc":"B2-410 区块链与信息安全实验室","teacher":"金刚波","sessions":[{"weeks":"1-4,6-14,17-18","start":1,"end":4}]},
  {"name":"软件工程综合实践","day":1,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"15-16","start":1,"end":4}]},
  {"name":"Python程序设计","day":1,"classes":"1班","loc":"B2-409 智能硬件与物联网实验室","teacher":"刘欣","sessions":[{"weeks":"1-3","start":6,"end":9},{"weeks":"4-6双,7-14,17-18","start":6,"end":8}]},
  {"name":"软件工程综合实践","day":1,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"16","start":6,"end":9}]},
  {"name":"智能终端应用软件开发","day":2,"classes":"1、2班","loc":"B2-417 程序设计基础实验室","teacher":"徐志强","sessions":[{"weeks":"11-14,17-18","start":1,"end":4},{"weeks":"1-4,6-7,9-10","start":1,"end":5}]},
  {"name":"软件工程综合实践","day":2,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"15-16","start":1,"end":5}]},
  {"name":"软件体系结构与架构技术","day":2,"classes":"1班","loc":"B2-406 生成式AI创新实验室","teacher":"王亚洲","sessions":[{"weeks":"1","start":6,"end":9},{"weeks":"2-3,7","start":6,"end":13},{"weeks":"4","start":6,"end":9,"note":"原周二晚10-13节已调至第8周周四下午"},{"weeks":"6","start":6,"end":9,"note":"原周二晚10-13节已调至第9周周三晚上"}]},
  {"name":"界面设计","day":2,"classes":"1班","loc":"B2-406 生成式AI创新实验室","teacher":"沈伊诺","sessions":[{"weeks":"9-14,17-18","start":6,"end":11}]},
  {"name":"软件工程综合实践","day":3,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"15-16","start":1,"end":9}]},
  {"name":"软件体系结构与架构技术","day":3,"classes":"1班","loc":"B2-406 生成式AI创新实验室","teacher":"王亚洲","sessions":[{"weeks":"1","start":6,"end":9},{"weeks":"9","start":10,"end":13,"makeup":true,"note":"由第6周周二晚调课补上"}]},
  {"name":"Web服务器端开发","day":3,"classes":"1、2班","loc":"B2-410 区块链与信息安全实验室","teacher":"金刚波","sessions":[{"weeks":"18","start":10,"end":13}]},
  {"name":"软件体系结构与架构技术","day":4,"classes":"1班","loc":"B2-406 生成式AI创新实验室","teacher":"王亚洲","sessions":[{"weeks":"8","start":6,"end":9,"makeup":true,"note":"由第4周周二晚调课补上"}]},
  {"name":"形势与政策","day":4,"classes":"1、2班","loc":"B5-1-1楼102合班教室","teacher":"向丽丽","sessions":[{"weeks":"9-13单,17","start":6,"end":7}]},
  {"name":"软件工程综合实践","day":4,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"15-16","start":6,"end":9}]},
  {"name":"软件工程综合实践","day":5,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"16","start":1,"end":4}]},
  {"name":"软件质量保证与测试","day":5,"classes":"1班","loc":"B2-406 生成式AI创新实验室","teacher":"赵煦华","sessions":[{"weeks":"1-2,5-11","start":6,"end":9},{"weeks":"12-14,18","start":6,"end":8}]},
  {"name":"软件工程综合实践","day":5,"classes":"1班","loc":"B2-422 软件技术实验室","teacher":"胡绵发","sessions":[{"weeks":"15-16","start":6,"end":9}]}
]
""".trimIndent()

    private fun parseCourses(raw: String): List<Course> {
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val sArr = o.getJSONArray("sessions")
                val sessions = (0 until sArr.length()).map { j ->
                    val so = sArr.getJSONObject(j)
                    Session(
                        weeks = so.getString("weeks"),
                        start = so.getInt("start"),
                        end = so.getInt("end"),
                        makeup = so.optBoolean("makeup", false),
                        note = so.optString("note", null).ifBlank { null }
                    )
                }
                Course(
                    name = o.getString("name"),
                    day = o.getInt("day"),
                    classes = o.optString("classes", null).ifBlank { null },
                    loc = o.getString("loc"),
                    teacher = o.getString("teacher"),
                    sessions = sessions
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== 设置 =====

    /** 第1周周一日期，默认 2026-09-07 */
    fun termStart(): Calendar {
        val y = prefs.getInt("term_y", 2026)
        val m = prefs.getInt("term_m", 8)  // 0-based，8 = 9月
        val d = prefs.getInt("term_d", 7)
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }

    fun saveTermStart(y: Int, month0: Int, d: Int) {
        prefs.edit().putInt("term_y", y).putInt("term_m", month0).putInt("term_d", d).apply()
    }

    fun baiduApiKey(): String = prefs.getString("baidu_ak", "") ?: ""
    fun baiduSecretKey(): String = prefs.getString("baidu_sk", "") ?: ""
    fun saveBaiduKeys(ak: String, sk: String) {
        prefs.edit().putString("baidu_ak", ak.trim()).putString("baidu_sk", sk.trim()).apply()
    }

    /** 上一次识别时用户选择的专业方向 */
    fun lastMajor(): String = prefs.getString("last_major", "") ?: ""
    fun saveLastMajor(major: String) {
        prefs.edit().putString("last_major", major).apply()
    }
}
