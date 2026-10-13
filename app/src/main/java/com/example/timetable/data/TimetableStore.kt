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
        return try {
            val raw = context.assets.open("default_timetable.json").bufferedReader().use { it.readText() }
            parseCourses(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

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
