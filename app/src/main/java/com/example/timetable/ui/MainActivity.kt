package com.example.timetable.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.timetable.data.Course
import com.example.timetable.data.Session
import com.example.timetable.data.Timetable
import com.example.timetable.data.TimetableStore
import com.example.timetable.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: TimetableStore

    private var week = 1
    private var courses: List<Course> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = TimetableStore(this)

        binding.btnImport.setOnClickListener {
            startActivity(Intent(this, ImportActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnPrev.setOnClickListener {
            if (week > 1) { week--; render() }
        }
        binding.btnNext.setOnClickListener {
            if (week < Timetable.TOTAL_WEEKS) { week++; render() }
        }
        binding.btnToday.setOnClickListener {
            week = Timetable.currentWeek(store.termStart())
            render()
        }
        binding.weekLabel.setOnClickListener { showWeekPicker() }

        binding.timetable.onCourseClick = { course, sess ->
            openCourseEdit(course, sess)
        }

        loadAndRender()
    }

    override fun onResume() {
        super.onResume()
        // 导入/设置返回后刷新
        if (::store.isInitialized) loadAndRender()
    }

    private fun loadAndRender() {
        courses = store.loadCourses()
        if (courses.isEmpty()) {
            // 首次使用：加载内置默认课表（当前学期的软件开发方向课表）
            val defaults = store.loadDefaultCourses()
            if (defaults.isNotEmpty()) {
                store.saveCourses(defaults)
                courses = defaults
            }
        }
        week = Timetable.currentWeek(store.termStart())
        render()
    }

    private fun render() {
        binding.weekLabel.text = weekLabelText(week)
        binding.emptyView.visibility = if (courses.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.timetable.visibility = if (courses.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        binding.timetable.setData(courses, week, store.termStart())
        renderWeekPanel()
    }

    private fun weekLabelText(w: Int): String {
        val monday = Timetable.mondayOfWeek(store.termStart(), w)
        val fmt = java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA)
        return "第 $w 周（${fmt.format(monday.time)}起）${if (w == Timetable.currentWeek(store.termStart())) "·本周" else ""}"
    }

    private fun renderWeekPanel() {
        binding.panelTitle.text = "本周课程速览 · 第 $week 周"
        binding.weekPanel.removeAllViews()
        for (d in 1..5) {
            val list = Timetable.coursesOfDay(courses, week, d)
            if (list.isEmpty()) continue
            val head = TextView(this).apply {
                text = "${Timetable.DAY_NAMES[d - 1]}（${list.size}门）"
                setTextColor(0xFF64748B.toInt())
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dp(0), dp(6), dp(0), dp(2))
            }
            binding.weekPanel.addView(head)
            for ((course, sess) in list) {
                binding.weekPanel.addView(weekItemView(course, sess))
            }
        }
        if (binding.weekPanel.childCount == 0) {
            val empty = TextView(this).apply {
                text = "本周无课"
                setTextColor(0xFFB6C2D2.toInt())
                textSize = 12f
                setPadding(dp(0), dp(6), dp(0), dp(0))
            }
            binding.weekPanel.addView(empty)
        }
    }

    private fun weekItemView(course: Course, sess: Session): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(0), dp(3), dp(0), dp(3))
        }
        val dot = TextView(this).apply {
            text = "●"
            textSize = 9f
            setTextColor(Timetable.colorOf(course.name))
            setPadding(dp(0), dp(0), dp(7), dp(0))
        }
        val name = TextView(this).apply {
            text = course.name
            setTextColor(0xFF0F172A.toInt())
            textSize = 13f
        }
        val meta = TextView(this).apply {
            text = buildString {
                append("${sess.start}-${sess.end}节")
                append(" · ${roomCodeOf(course.loc)}")
                if (sess.makeup) append(" · 调课")
            }
            setTextColor(0xFF64748B.toInt())
            textSize = 11.5f
            setPadding(dp(8), dp(0), dp(0), dp(0))
        }
        row.addView(dot)
        row.addView(name)
        row.addView(meta)
        return row
    }

    private fun roomCodeOf(loc: String): String {
        val parts = loc.split(' ')
        return if (parts.size > 1) parts[0] else loc.replace(Regex("合班教室$"), "")
    }

    private fun showWeekPicker() {
        val options = (1..Timetable.TOTAL_WEEKS).map { "第 $it 周" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择周次")
            .setSingleChoiceItems(options, week - 1) { d, which ->
                week = which + 1
                d.dismiss()
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openCourseEdit(course: Course, sess: Session) {
        val intent = Intent(this, CourseEditActivity::class.java)
        intent.putExtra("course_json", courseToJson(course, sess))
        startActivity(intent)
    }

    private fun courseToJson(course: Course, sess: Session): String {
        val o = org.json.JSONObject()
        o.put("name", course.name)
        o.put("day", course.day)
        o.put("loc", course.loc)
        o.put("teacher", course.teacher)
        o.put("weeks", sess.weeks)
        o.put("start", sess.start)
        o.put("end", sess.end)
        return o.toString()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
