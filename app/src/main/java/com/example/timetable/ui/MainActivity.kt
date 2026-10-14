package com.example.timetable.ui

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.timetable.data.Course
import com.example.timetable.data.Session
import com.example.timetable.data.Timetable
import com.example.timetable.data.TimetableStore
import com.example.timetable.R
import com.example.timetable.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: TimetableStore

    private var week = 1
    private var courses: List<Course> = emptyList()
    private var editMode = false

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
        binding.btnEdit.setOnClickListener { toggleEditMode() }

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
        binding.timetable.onDropAdd = { name, day, start, end ->
            lastDroppedName = name
            showDropDialog(day, start, end)
        }
        binding.timetable.onResizeCommit = { course, sess, newStart, newEnd ->
            commitResize(course, sess, newStart, newEnd)
        }
        binding.btnNewCourse.setOnClickListener {
            openCourseEdit(null, null)
        }
        binding.btnLoadDefault.setOnClickListener {
            val defaults = store.loadDefaultCourses()
            if (defaults.isNotEmpty()) {
                store.saveCourses(defaults)
                courses = defaults
                render()
                rebuildCourseList()
                Toast.makeText(this, "已加载内置课表（软件开发方向，共 ${defaults.map { it.name }.distinct().size} 门课）", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "内置课表加载失败", Toast.LENGTH_SHORT).show()
            }
        }

        loadAndRender()
    }

    override fun onResume() {
        super.onResume()
        // 导入/设置/编辑返回后刷新
        if (::store.isInitialized) {
            loadAndRender()
            rebuildCourseList()
        }
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
        binding.emptyView.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
        binding.timetable.visibility = if (courses.isEmpty()) View.GONE else View.VISIBLE
        binding.timetable.setData(courses, week, store.termStart())
        renderWeekPanel()
    }

    // ===== 编辑模式 =====

    private fun toggleEditMode() {
        editMode = !editMode
        binding.btnEdit.setTextColor(if (editMode) 0xFFFFD54F.toInt() else 0xFFFFFFFF.toInt())
        binding.editPanel.visibility = if (editMode) View.VISIBLE else View.GONE
        binding.timetable.editMode = editMode
        if (editMode) rebuildCourseList()
    }

    /** 课程拖拽列表：去重后的课程名，长按拖到课表 */
    private fun rebuildCourseList() {
        binding.courseList.removeAllViews()
        val names = courses.map { it.name }.distinct()
        if (names.isEmpty()) {
            val tv = TextView(this).apply {
                text = "暂无课程"
                setTextColor(0xFF64748B.toInt())
                textSize = 12f
                setPadding(0, dp(4), 0, dp(4))
            }
            binding.courseList.addView(tv)
            return
        }
        names.forEach { name ->
            val proto = courses.first { it.name == name }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = resources.getDrawable(R.drawable.bg_chip, theme)
                setOnLongClickListener {
                    val data = ClipData.newPlainText("course", name)
                    startDragAndDrop(data, View.DragShadowBuilder(it), it, 0)
                    true
                }
            }
            val dot = TextView(this).apply {
                text = "●"
                textSize = 10f
                setTextColor(Timetable.colorOf(name))
                setPadding(0, 0, dp(8), 0)
            }
            val nameTv = TextView(this).apply {
                text = name
                setTextColor(0xFF0F172A.toInt())
                textSize = 13.5f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val meta = TextView(this).apply {
                text = " ${proto.teacher} · ${roomCodeOf(proto.loc)}"
                setTextColor(0xFF64748B.toInt())
                textSize = 11f
                setPadding(dp(6), 0, 0, 0)
            }
            row.addView(dot)
            row.addView(nameTv)
            row.addView(meta)
            binding.courseList.addView(row)
        }
    }

    /** 拖入课表后的周次/节次确认弹窗 */
    private fun showDropDialog(day: Int, start: Int, end: Int) {
        val etWeeks = EditText(this).apply {
            setText("1-18")
            hint = "如 1-18 / 4,6 / 9-13单"
        }
        val etStart = EditText(this).apply {
            setText("$start")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val etEnd = EditText(this).apply {
            setText("$end")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        form.addView(fieldRow("周次", etWeeks))
        form.addView(fieldRow("开始节次", etStart))
        form.addView(fieldRow("结束节次", etEnd))

        AlertDialog.Builder(this)
            .setTitle("添加课程 · ${Timetable.DAY_NAMES[day - 1]}")
            .setMessage("拖入的课程：${lastDroppedName ?: ""}\n确认周次与节次：")
            .setView(form)
            .setPositiveButton("添加") { _, _ ->
                val weeks = etWeeks.text.toString().trim()
                val s = etStart.text.toString().toIntOrNull()
                val e = etEnd.text.toString().toIntOrNull()
                if (weeks.isEmpty() || s == null || e == null || e < s) {
                    Toast.makeText(this, "周次或节次不合法", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                addDroppedCourse(day, s, e, weeks)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun fieldRow(label: String, et: EditText): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val lv = TextView(this).apply {
            text = label
            setTextColor(0xFF0F172A.toInt())
            textSize = 14f
            setPadding(0, 0, dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        row.addView(lv)
        row.addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    /** 把拖入的课程写入课表：同名同天课程追加 Session，否则新建课程 */
    private fun addDroppedCourse(day: Int, start: Int, end: Int, weeks: String) {
        val name = lastDroppedName ?: return
        lastDroppedName = null
        val proto = courses.firstOrNull { it.name == name }
        val loc = proto?.loc ?: ""
        val teacher = proto?.teacher ?: "未知教师"
        val classes = proto?.classes

        val list = store.loadCourses().toMutableList()
        val idx = list.indexOfFirst { it.name == name && it.day == day }
        val newSess = Session(weeks, start, end)
        if (idx >= 0) {
            val old = list[idx]
            val exists = old.sessions.any { it.weeks == weeks }
            val newSessions = if (exists) {
                old.sessions.map { if (it.weeks == weeks) newSess else it }
            } else old.sessions + newSess
            list[idx] = old.copy(sessions = newSessions)
        } else {
            list.add(Course(name, day, classes, loc, teacher, listOf(newSess)))
        }
        store.saveCourses(list)
        courses = list
        rebuildCourseList()
        render()
        Toast.makeText(this, "已添加：${Timetable.DAY_NAMES[day - 1]} $start-$end 节", Toast.LENGTH_SHORT).show()
    }

    /** 记录最近一次拖入的课程名（onDropAdd 弹窗确认前暂存） */
    private var lastDroppedName: String? = null

    /** 卡片上下沿拖拽调整节次完成 */
    private fun commitResize(course: Course, sess: Session, newStart: Int, newEnd: Int) {
        if (newStart == sess.start && newEnd == sess.end) return
        val list = store.loadCourses().toMutableList()
        var changed = false
        for (i in list.indices) {
            val c = list[i]
            if (c.name != course.name || c.day != course.day) continue
            val ns = c.sessions.map { s ->
                if (s.weeks == sess.weeks && s.start == sess.start && s.end == sess.end) {
                    changed = true
                    s.copy(start = newStart, end = newEnd)
                } else s
            }
            list[i] = c.copy(sessions = ns)
        }
        if (changed) {
            store.saveCourses(list)
            courses = list
            render()
            Toast.makeText(this, "已调整为 $newStart-$newEnd 节", Toast.LENGTH_SHORT).show()
        }
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
                setTextColor(0xFF64748B.toInt())
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

    private fun openCourseEdit(course: Course?, sess: Session?) {
        val intent = Intent(this, CourseEditActivity::class.java)
        intent.putExtra("course_json", courseToJson(course, sess))
        startActivity(intent)
    }

    private fun courseToJson(course: Course?, sess: Session?): String {
        val o = org.json.JSONObject()
        if (course != null && sess != null) {
            o.put("name", course.name)
            o.put("day", course.day)
            o.put("loc", course.loc)
            o.put("teacher", course.teacher)
            o.put("weeks", sess.weeks)
            o.put("start", sess.start)
            o.put("end", sess.end)
        }
        return o.toString()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
