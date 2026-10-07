package com.example.petfeeding

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petfeeding.data.FeedingRepository
import com.example.petfeeding.data.FeedingTime
import com.example.petfeeding.notifications.FeedingAlarmReceiver
import com.example.petfeeding.notifications.FeedingAlarmScheduler
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.listitem.ListItemCardView
import com.google.android.material.listitem.ListItemLayout
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var content: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var repository: FeedingRepository
    private lateinit var scheduler: FeedingAlarmScheduler
    private var items = emptyList<FeedingTime>()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val density get() = resources.displayMetrics.density

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repository = FeedingRepository(this)
        scheduler = FeedingAlarmScheduler(this)
        FeedingAlarmReceiver.createChannel(this)
        requestNotificationPermission()
        buildShell()
        showFood()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }
        toolbar = MaterialToolbar(this).apply {
            title = "Feeding times"
            setTitleCentered(false)
            navigationIcon = DrawableCompat.wrap(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_pets)!!).apply { DrawableCompat.setTint(this, resolveColor(com.google.android.material.R.attr.colorOnSurface)) }
            navigationContentDescription = "Pet"
            overflowIcon = DrawableCompat.wrap(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_more_vert)!!).apply { DrawableCompat.setTint(this, resolveColor(com.google.android.material.R.attr.colorOnSurface)) }
            inflateMenu(R.menu.top_app_bar)
            setOnMenuItemClickListener { true }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        bottomNavigation = BottomNavigationView(this).apply {
            inflateMenu(R.menu.bottom_navigation)
            selectedItemId = R.id.nav_food
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.nav_food -> { toolbar.title = "Feeding times"; showFood() }
                    R.id.nav_profile -> { toolbar.title = "Profile"; showProfile() }
                }
                true
            }
        }
        root.addView(bottomNavigation, LinearLayout.LayoutParams(-1, dp(80)))
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(0, bars.top, 0, 0)
            bottomNavigation.layoutParams = bottomNavigation.layoutParams.apply { height = dp(80) + bars.bottom }
            bottomNavigation.setPadding(0, 0, 0, bars.bottom)
            insets
        }
        setContentView(root)
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
    }

    private fun showFood() {
        content.removeAllViews()
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(100))
        }
        val grid = android.widget.GridLayout(this).apply {
            columnCount = 2
            alignmentMode = android.widget.GridLayout.ALIGN_MARGINS
            useDefaultMargins = false
        }
        items = repository.load()
        if (items.isEmpty()) {
            body.addView(TextView(this).apply {
                text = getString(R.string.no_times)
                setTextAppearance(R.style.TextAppearance_PetFeeding_Body)
                setFontVariationSettings("'wght' 420, 'wdth' 100, 'opsz' 16")
                setPadding(dp(20), dp(48), dp(20), dp(24))
            })
        } else {
            items.forEach { item ->
                grid.addView(timeCard(item), android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(220)
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                })
            }
        }
        body.addView(grid)
        scroll.addView(body)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val fab = layoutInflater.inflate(R.layout.fab_add, null) as FloatingActionButton
        fab.apply {
            elevation = 0f
            compatElevation = 0f
            stateListAnimator = null
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener { openTimePicker() }
        }
        addContentView(fab, ViewGroup.LayoutParams(dp(64), dp(64)))
        fab.x = resources.displayMetrics.widthPixels - dp(88).toFloat()
        fab.y = resources.displayMetrics.heightPixels - dp(176).toFloat()
    }

    private fun timeCard(item: FeedingTime): View {
        val card = MaterialCardView(this).apply {
            setCardBackgroundColor(resolveColor(if (item.enabled) com.google.android.material.R.attr.colorPrimaryContainer else com.google.android.material.R.attr.colorSurfaceContainer))
            strokeWidth = dp(1)
            strokeColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant)
            radius = dp(24).toFloat()
            isLongClickable = true
            setOnLongClickListener { showDeleteDialog(item); true }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(8))
        }
        val timeRow = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val hour = TextView(this).apply {
            text = "%02d".format(if (item.hour % 12 == 0) 12 else item.hour % 12)
            setTextAppearance(R.style.TextAppearance_PetFeeding_Headline)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 76f)
            setFontVariationSettings("'wght' 820, 'wdth' 45, 'opsz' 90")
            setTextColor(resolveColor(if (item.enabled) com.google.android.material.R.attr.colorOnPrimaryContainer else androidx.appcompat.R.attr.colorPrimary))
            isSingleLine = true
            maxLines = 1
            includeFontPadding = false
        }
        val minute = TextView(this).apply {
            text = "%02d".format(item.minute)
            setTextAppearance(R.style.TextAppearance_PetFeeding_Headline)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 68f)
            setFontVariationSettings("'wght' 300, 'wdth' 88, 'opsz' 72")
            setTextColor(resolveColor(if (item.enabled) com.google.android.material.R.attr.colorOnPrimaryContainer else androidx.appcompat.R.attr.colorPrimary))
            isSingleLine = true
            maxLines = 1
            includeFontPadding = false
        }
        timeRow.addView(hour, LinearLayout.LayoutParams(-2, -1))
        timeRow.addView(minute, LinearLayout.LayoutParams(-2, -1))
        val period = TextView(this).apply {
            text = if (item.hour < 12) "AM" else "PM"
            setTextAppearance(R.style.TextAppearance_PetFeeding_Label)
            setFontVariationSettings("'wght' 560, 'wdth' 96, 'opsz' 12")
            setTextColor(resolveColor(if (item.enabled) com.google.android.material.R.attr.colorOnPrimaryContainer else androidx.appcompat.R.attr.colorPrimary))
            gravity = Gravity.CENTER
        }
        val sw = MaterialSwitch(this).apply {
            isChecked = item.enabled
            text = ""
            contentDescription = "Reminder for %02d:%02d".format(item.hour, item.minute)
            setOnCheckedChangeListener { _, checked ->
                val updated = item.withEnabled(checked)
                items = items.map { if (it.id == item.id) updated else it }
                repository.save(items)
                if (checked) scheduler.schedule(updated) else scheduler.cancel(updated)
                showFood()
            }
        }
        box.addView(timeRow, LinearLayout.LayoutParams(-1, 0, 1f))
        box.addView(period, LinearLayout.LayoutParams(-1, dp(24)))
        box.addView(sw, LinearLayout.LayoutParams(-2, dp(44)))
        card.addView(box)
        return card
    }

    private fun showDeleteDialog(item: FeedingTime) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete feeding time?")
            .setMessage("%02d:%02d will be removed.".format(item.hour, item.minute))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                scheduler.cancel(item)
                items = items.filterNot { it.id == item.id }
                repository.save(items)
                showFood()
            }
            .show()
    }

    private fun openTimePicker() {
        val now = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(now.get(Calendar.HOUR_OF_DAY))
            .setMinute(now.get(Calendar.MINUTE))
            .setTitleText(getString(R.string.add_feeding_time))
            .build()
        picker.addOnPositiveButtonClickListener {
            val newItem = FeedingTime(System.currentTimeMillis(), picker.hour, picker.minute, true)
            items = (items + newItem).sortedWith(compareBy({ it.hour }, { it.minute }))
            repository.save(items)
            scheduler.schedule(newItem)
            showFood()
        }
        picker.show(supportFragmentManager, "feeding_time_picker")
    }

    private fun showProfile() {
        content.removeAllViews()
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
        }
        val petName = getPreferences(0).getString("pet_name", "Pet") ?: "Pet"
        val petType = getPreferences(0).getString("pet_type", "Dog or cat") ?: "Dog or cat"
        val permission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val listItems = listOf(
            settingCard("Pet name", petName, R.drawable.ic_profile) { editPetName() },
            settingCard("Pet type", petType, R.drawable.ic_pets) { editPetType() },
            settingCard("Notifications", if (permission) "Allowed" else "Permission required", R.drawable.ic_more_vert) { requestNotificationPermission() }
        )
        listItems.forEachIndexed { index, view ->
            (view as ListItemLayout).updateAppearance(index, listItems.size)
            list.addView(view)
        }
        scroll.addView(list)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun settingCard(title: String, summary: String, icon: Int, action: () -> Unit): View {
        val listItem = layoutInflater.inflate(R.layout.preference_list_item, null)
        val card = listItem.findViewById<ListItemCardView>(R.id.preference_card)
        listItem.findViewById<ImageView>(R.id.preference_icon).setImageResource(icon)
        listItem.findViewById<TextView>(R.id.preference_title).apply {
            text = title
            setFontVariationSettings("'wght' 420, 'wdth' 100, 'opsz' 16")
        }
        listItem.findViewById<TextView>(R.id.preference_summary).apply {
            text = summary
            setFontVariationSettings("'wght' 560, 'wdth' 96, 'opsz' 12")
        }
        card.setOnClickListener { action() }
        listItem.layoutParams = LinearLayout.LayoutParams(-1, -2)
        return listItem
    }

    private fun editPetName() {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT; hint = "Pet name"; setText(getPreferences(0).getString("pet_name", "Pet")) }
        MaterialAlertDialogBuilder(this).setTitle("Pet name").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ -> getPreferences(0).edit().putString("pet_name", input.text.toString()).apply(); showProfile() }.show()
    }

    private fun editPetType() {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT; hint = "Dog or cat"; setText(getPreferences(0).getString("pet_type", "Dog or cat")) }
        MaterialAlertDialogBuilder(this).setTitle("Pet type").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ -> getPreferences(0).edit().putString("pet_type", input.text.toString()).apply(); showProfile() }.show()
    }

    private fun dp(value: Int) = (value * density).toInt()
    private fun resolveColor(attr: Int) = com.google.android.material.color.MaterialColors.getColor(this, attr, 0)
}
