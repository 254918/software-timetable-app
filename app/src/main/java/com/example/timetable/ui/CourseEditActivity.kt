package com.example.timetable.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.timetable.data.Session
import com.example.timetable.data.Timetable
import com.example.timetable.data.TimetableStore
import com.example.timetable.databinding.ActivityCourseEditBinding
import org.json.JSONObject

/**
 * 单门课程校正：识别结果或课表中的课程点开后可修改
 * 课程名 / 星期 / 节次 / 周次 / 教室 / 教师，保存后写回本地课表。
 */
class CourseEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseEditBinding
    private lateinit var store: TimetableStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = TimetableStore(this)

        val raw = intent.getStringExtra("course_json") ?: "{}"
        val o = JSONObject(raw)

        binding.spDay.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, Timetable.DAY_NAMES
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val day = o.optInt("day", 1)
        binding.spDay.setSelection(day.coerceIn(1, 5) - 1)
        binding.etName.setText(o.optString("name"))
        binding.etStart.setText(o.optInt("start", 1).toString())
        binding.etEnd.setText(o.optInt("end", 1).toString())
        binding.etWeeks.setText(o.optString("weeks"))
        binding.etLoc.setText(o.optString("loc"))
        binding.etTeacher.setText(o.optString("teacher"))

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val weeks = binding.etWeeks.text.toString().trim()
            val loc = binding.etLoc.text.toString().trim()
            val teacher = binding.etTeacher.text.toString().trim()
            val start = binding.etStart.text.toString().toIntOrNull()
            val end = binding.etEnd.text.toString().toIntOrNull()
            if (name.isEmpty() || start == null || end == null || weeks.isEmpty()) {
                Toast.makeText(this, "请填写课程名、节次与周次", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newDay = binding.spDay.selectedItemPosition + 1

            // 编辑模式（commit_store=false）：返回结果由主界面更新草稿，不直接写存储
            if (!intent.getBooleanExtra("commit_store", false)) {
                val result = JSONObject()
                result.put("name", name)
                result.put("day", newDay)
                result.put("loc", loc)
                result.put("teacher", teacher)
                result.put("weeks", weeks)
                result.put("start", start)
                result.put("end", end)
                result.put("old_name", o.optString("name"))
                result.put("old_day", o.optInt("day", 1))
                result.put("old_weeks", o.optString("weeks"))
                result.put("old_start", o.optInt("start", 1))
                result.put("old_end", o.optInt("end", 1))
                setResult(RESULT_OK, Intent().putExtra("course_json", result.toString()))
                finish()
                return@setOnClickListener
            }

            val courses = store.loadCourses().toMutableList()
            // 匹配同课程（名称+原星期）更新；否则新增
            val idx = courses.indexOfFirst { it.name == o.optString("name") && it.day == o.optInt("day", 1) }
            if (idx >= 0) {
                val old = courses[idx]
                val newSessions = old.sessions.map { s ->
                    if (s.weeks == o.optString("weeks") && s.start == o.optInt("start") && s.end == o.optInt("end")) {
                        Session(weeks, start, end, s.makeup, s.note)
                    } else s
                }
                courses[idx] = old.copy(
                    name = name, day = newDay, loc = loc, teacher = teacher, sessions = newSessions
                )
            } else {
                courses.add(
                    com.example.timetable.data.Course(
                        name = name, day = newDay, loc = loc, teacher = teacher,
                        sessions = listOf(Session(weeks, start, end))
                    )
                )
            }
            store.saveCourses(courses)
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnDelete.setOnClickListener {
            // 新课程（表单是空的）没有可删除的本节课
            if (o.optString("name").isEmpty() && o.optInt("start", 1) == 1 && o.optString("weeks").isEmpty()) {
                Toast.makeText(this, "新课程尚未创建，无可删除", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 编辑模式（commit_store=false）：返回删除标志，由主界面更新草稿
            if (!intent.getBooleanExtra("commit_store", false)) {
                val result = JSONObject()
                result.put("delete_session", true)
                result.put("old_name", o.optString("name"))
                result.put("old_day", o.optInt("day", 1))
                result.put("old_weeks", o.optString("weeks"))
                result.put("old_start", o.optInt("start", 1))
                result.put("old_end", o.optInt("end", 1))
                setResult(RESULT_OK, Intent().putExtra("course_json", result.toString()))
                finish()
                return@setOnClickListener
            }
            // 非编辑模式：直接删除本节课并保存（只删当前周，其余周保留）
            val currentWeek = intent.getIntExtra("current_week", 1)
            val courses = store.loadCourses().toMutableList()
            val idx = courses.indexOfFirst { it.name == o.optString("name") && it.day == o.optInt("day", 1) }
            if (idx >= 0) {
                val c = courses[idx]
                val newWeeks = Timetable.weeksWithout(o.optString("weeks"), currentWeek)
                val remain = c.sessions.map { s ->
                    if (s.weeks == o.optString("weeks") && s.start == o.optInt("start") && s.end == o.optInt("end")) {
                        if (newWeeks.isEmpty()) null else s.copy(weeks = newWeeks)
                    } else s
                }.filterNotNull()
                if (remain.isEmpty()) courses.removeAt(idx) else courses[idx] = c.copy(sessions = remain)
                store.saveCourses(courses)
            }
            Toast.makeText(this, "已删除本周的这节课", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
