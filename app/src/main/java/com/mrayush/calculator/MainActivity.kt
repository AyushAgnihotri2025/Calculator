package com.mrayush.calculator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.tasks.Task
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.mrayush.calculator.databinding.ActivityMainBinding
import com.mrayush.calculator.screens.DataScreen
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var operation: Operation = Operation.EMPTY
    private var firstProcessingNumber = 0.0
    private var secondProcessingNumber = 0.0
    private var is_errored_text = false
    private var is_ans_showed = false
    lateinit var drawerlayout: DrawerLayout
    lateinit var navigationView: NavigationView
    lateinit var toolbar: Toolbar
    private var btnShowDialog: Button? = null
    private var reviewInfo: ReviewInfo? = null
    private var manager: ReviewManager? = null
    private val TAG = "Update_Button"
    private var UPDATE_REQUEST_CODE = 100
    private lateinit var appUpdateManager: AppUpdateManager
    private var player: MediaPlayer? = null
    private var darkmode = false
    private var activeSidebarModal: View? = null
    private val operators = charArrayOf(
        '/',
        '*',
        '%',
        '-',
        '+',
        'l',
        'o',
        'g',
        'n',
        '!',
        '^',
        'C',
        '√',
        's',
        'i',
        'n',
        'c',
        'o',
        't',
        'a',
        'e',
        'm',
        'd'
    )

    companion object {
        // Held across the theme-change recreate() so the new screen can
        // circular-reveal itself over a snapshot of the old theme.
        private var revealBitmap: Bitmap? = null
        private var revealCx = 0
        private var revealCy = 0
        private var pendingThemeReveal = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("darkmode", darkmode)
    }

    /* if (savedInstanceState != null) {
              darkmode = savedInstanceState.getBoolean("darkmode")
          }
  */
    override fun onCreate(savedInstanceState: Bundle?) {
        loadDayNight()
        super.onCreate(savedInstanceState)

        // Initialize binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_main)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        initListeners()
        activateReviewInfo()

        drawerlayout = findViewById(R.id.menu_drawer)
        navigationView = findViewById(R.id.navigation_menu)
        toolbar = findViewById(R.id.menuButton)
        setSupportActionBar(toolbar)


        val toggle = ActionBarDrawerToggle(
            this,
            drawerlayout,
            toolbar,
            R.string.navigation_open,
            R.string.navigation_close
        )

        drawerlayout.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()
        navigationView = findViewById<View>(R.id.navigation_menu) as NavigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerlayout.closeDrawer(GravityCompat.START, false)
            when (menuItem.itemId) {
                // R.id.preferences -> darkmode()
                R.id.developer -> {
                    portFolioIntent(); true
                }

                R.id.share -> {
                    share(); true
                }

                R.id.feedback -> {
                    startReviewFlow(); true
                }

                R.id.update -> {
                    update(true); true
                }

                R.id.source_code -> {
                    showSourceCodeDialog(); true
                }

                R.id.about_us -> {
                    aboutUsIntent(); true
                }

                R.id.appVersionOption -> {
                    AppVersionOption(); true
                }

                R.id.chooseLanguage -> {
                    showChangeLanguage(); true
                }

                R.id.report_bug -> {
                    ReportBug(); true
                }

                else -> false
            }
        }

        daynight()

        // If we just switched theme, animate the new one in from the toggle.
        playThemeReveal()
    }

    private fun ReportBug() {
        vibration()
        onClickSound()
        val recipient = "ayushagnihotri2025@gmail.com"
        val subject = "Report for the Bug - Calculator Application"
        val message = "Email message body"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_TEXT, message)
        }
        startActivity(intent)
    }

    private fun showChangeLanguage() {
        vibration()
        val modal = createSidebarModal()
        modal.findViewById<TextView>(R.id.sidebarModalTitle)
            .setText(R.string.choose_language)
        modal.findViewById<TextView>(R.id.sidebarModalMessage).visibility = View.GONE
        modal.findViewById<Button>(R.id.sidebarModalAction).visibility = View.GONE

        val languageOptions =
            modal.findViewById<RadioGroup>(R.id.sidebarLanguageOptions)
        languageOptions.visibility = View.VISIBLE
        val currentLanguageIsHindi =
            AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith("hi")
        languageOptions.check(
            if (currentLanguageIsHindi) {
                R.id.sidebarLanguageHindi
            } else {
                R.id.sidebarLanguageEnglish
            }
        )
        languageOptions.setOnCheckedChangeListener { _, checkedId ->
            val languageTag = if (checkedId == R.id.sidebarLanguageHindi) "hi-IN" else "en"

            dismissSidebarModal()
            window.decorView.post {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(languageTag)
                )
            }
        }
    }

    private fun restartAfterLocaleChange() {
        val restartIntent = Intent(this, MainActivity::class.java)
        startActivity(restartIntent)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun createSidebarModal(): View {
        dismissSidebarModal()
        val content = findViewById<FrameLayout>(android.R.id.content)
        val modal = layoutInflater.inflate(R.layout.sidebar_modal, content, false)
        content.addView(modal)
        activeSidebarModal = modal
        modal.findViewById<Button>(R.id.sidebarModalCancel).setOnClickListener {
            dismissSidebarModal()
        }
        modal.setOnClickListener {
            dismissSidebarModal()
        }
        return modal
    }

    private fun dismissSidebarModal() {
        val modal = activeSidebarModal ?: return
        (modal.parent as? ViewGroup)?.removeView(modal)
        activeSidebarModal = null
    }

    private fun AppVersionOption() {
        vibration()
        Toast.makeText(
            this@MainActivity,
            getString(R.string.About_version) + getString(R.string.appVersion),
            Toast.LENGTH_LONG
        ).show()
    }

    // load Dark night after open the app
    private fun loadDayNight() {
        val sharedPreferences = getSharedPreferences("DayNight", Activity.MODE_PRIVATE)
        val DayNight = sharedPreferences.getString("My_DayNight", "")
        if (DayNight != null) {
            setDayNight(DayNight)

        }
    }


    // load DayNight actually
    private fun daynight() {
//        vibration()
        val swtch: Switch? = findViewById(R.id.daynight)

        // Load the state of the switch from SharedPreferences
        val sharedPreferences = getSharedPreferences("DayNight", MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean("My_Switch", false)

        if (swtch != null && swtch is Switch) {
            swtch.isChecked = switchState
        }

        swtch?.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton is Switch) {
                // Snapshot the current theme so the recreate can reveal over it.
                prepareThemeReveal(compoundButton)
                if (compoundButton.isChecked) {
                    setDayNight("yes")
                } else {
                    setDayNight("no")
                }
                vibration()

                // Save the state of the switch to SharedPreferences
                sharedPreferences.edit {
                    putBoolean("My_Switch", compoundButton.isChecked)
                }
            }
        }
    }

    // Capture a bitmap of the current screen + the toggle's center, kept until
    // the recreated activity plays the circular reveal.
    private fun prepareThemeReveal(anchor: View) {
        val root = binding.root
        if (root.width <= 0 || root.height <= 0) {
            pendingThemeReveal = false
            return
        }
        try {
            val bmp = createBitmap(root.width, root.height)
            root.draw(Canvas(bmp))

            val anchorLoc = IntArray(2)
            val rootLoc = IntArray(2)
            anchor.getLocationInWindow(anchorLoc)
            root.getLocationInWindow(rootLoc)

            revealCx = anchorLoc[0] - rootLoc[0] + anchor.width / 2
            revealCy = anchorLoc[1] - rootLoc[1] + anchor.height / 2
            revealBitmap = bmp
            pendingThemeReveal = true
        } catch (e: Exception) {
            pendingThemeReveal = false
            revealBitmap = null
        }
    }

    // On the recreated activity, lay the old snapshot behind the new content and
    // grow the new theme out from the toggle in a circular reveal.
    private fun playThemeReveal() {
        if (!pendingThemeReveal) return
        pendingThemeReveal = false

        val bmp = revealBitmap ?: return
        val contentFrame = findViewById<ViewGroup>(android.R.id.content) ?: run {
            revealBitmap = null
            return
        }
        val newRoot = binding.root

        val overlay = ImageView(this)
        overlay.setImageBitmap(bmp)
        overlay.scaleType = ImageView.ScaleType.FIT_XY
        contentFrame.addView(
            overlay,
            0,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val cleanup = {
            contentFrame.removeView(overlay)
            overlay.setImageDrawable(null)
            revealBitmap = null
            if (!bmp.isRecycled) bmp.recycle()
        }

        // Start the reveal during pre-draw so the new theme is already clipped to
        // radius 0 on its first drawn frame (prevents a flash of the full new theme).
        newRoot.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                newRoot.viewTreeObserver.removeOnPreDrawListener(this)
                if (newRoot.width <= 0 || newRoot.height <= 0) {
                    cleanup()
                    return true
                }
                val cx = revealCx.coerceIn(0, newRoot.width)
                val cy = revealCy.coerceIn(0, newRoot.height)
                val radius = hypot(newRoot.width.toFloat(), newRoot.height.toFloat())
                val anim = ViewAnimationUtils.createCircularReveal(newRoot, cx, cy, 0f, radius)
                anim.duration = 500
                anim.interpolator = AccelerateDecelerateInterpolator()
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = cleanup()
                    override fun onAnimationCancel(animation: Animator) = cleanup()
                })
                anim.start()
                return true
            }
        })
    }

    private fun setDayNight(daynightMode: String) {
        getSharedPreferences("DayNight", MODE_PRIVATE).edit {
            putString("My_DayNight", daynightMode)
        }
        if (daynightMode == "yes") {
            getDelegate().localNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            getDelegate().localNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    /* private fun darkmode() {
        vibration()
        val share: Button
        val swtch: Switch
        val close: ImageButton
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.darkmodeprompt)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        val window = dialog.window
        window!!.setGravity(Gravity.CENTER)
        window.attributes.windowAnimations = R.style.DialogAnimation
        swtch = dialog.findViewById(R.id.modebtn)
        close = dialog.findViewById(R.id.closePopup)
        swtch.isChecked = darkmode
        swtch.setOnCheckedChangeListener { compoundButton, b ->
            darkmode = !darkmode
            if(darkmode) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(this@MainActivity, "Its on", Toast.LENGTH_SHORT).show()
            }
            else {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(this@MainActivity, "Its off", Toast.LENGTH_SHORT).show()
            }
        }

        close.setOnClickListener {
            vibration()
            dialog.dismiss()
        }
        dialog.setCancelable(true)
        window.setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        dialog.show()

    }
*/
    private fun aboutUsIntent() {
        onClickSound()
        vibration()
        val aboutUsIntent = Intent(this@MainActivity, AboutUs::class.java)
        startActivity(aboutUsIntent)
    }


    private fun portFolioIntent() {
        onClickSound()
        vibration()
        val portFolioIntent = Intent(
            Intent.ACTION_VIEW,
            ("https://mrayush.me/?refer=calculator-" + getString(R.string.appVersion)).toUri()
        )
        startActivity(portFolioIntent)
    }

    private fun share() {
        vibration()
        val i: ImageView = ImageView(applicationContext)
        i.setImageResource(R.drawable.banner)
        val bitmapDrawable = i.drawable as BitmapDrawable
        val bitmap = bitmapDrawable.bitmap
        val uri: Uri = getImageToShare(bitmap)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("image/*")
        shareIntent.putExtra(EXTRA_SUBJECT, getString(R.string.share_subject))
        shareIntent.putExtra(EXTRA_TEXT, getString(R.string.share_text, getPackageName()))
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
    }

    private fun getImageToShare(bitmap: Bitmap): Uri {
        val folder: File = File(cacheDir, "images")

        folder.mkdirs()
        val file: File = File(folder, "shared_image.jpg")
        val fileOutputStream: FileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()

        val uri: Uri =
            FileProvider.getUriForFile(applicationContext, "com.mrayush.calculator", file)
        return uri

    }

    private fun showSourceCodeDialog() {
        vibration()
        val modal = createSidebarModal()
        modal.findViewById<ImageView>(R.id.sidebarModalImage).visibility = View.VISIBLE
        modal.findViewById<TextView>(R.id.sidebarModalTitle)
            .setText(R.string.Source_Code)
        modal.findViewById<TextView>(R.id.sidebarModalMessage)
            .setText(R.string.popup_text)
        modal.findViewById<Button>(R.id.sidebarModalAction).apply {
            setText(R.string.Source_Code)
            setOnClickListener {
                dismissSidebarModal()
                val sourceCodeUrl =
                    "https://github.com/AyushAgnihotri2025/Calculator".toUri()
                startActivity(Intent(Intent.ACTION_VIEW, sourceCodeUrl))
            }
        }
    }

    private fun closeDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.menu_drawer)
        drawer.closeDrawer(GravityCompat.START)
    }


    private fun openPlayStore() {
        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())
        )
        startActivity(playStoreIntent)
    }

    private fun activateReviewInfo() {
        manager = ReviewManagerFactory.create(this)
        val managerInfoTask: Task<ReviewInfo> = manager!!.requestReviewFlow()
        managerInfoTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewInfo = task.result
            } else {
                Toast.makeText(this, getString(R.string.Error_In_Rating), Toast.LENGTH_SHORT).show()
                openPlayStore()
            }
        }
    }

    private fun startReviewFlow() {
        onClickSound()
        vibration()
        Toast.makeText(
            this@MainActivity,
            getString(R.string.scroll_down_for_rating),
            Toast.LENGTH_LONG
        ).show()

        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            ("https://play.google.com/store/apps/details?id=" + getPackageName()).toUri()
        )
        startActivity(playStoreIntent)

        // For future reference

//        if (reviewInfo != null) {
//            val flow: Task<Void> = manager!!.launchReviewFlow(this, reviewInfo!!)
//            flow.addOnCompleteListener { task ->
//                Toast.makeText(
//                    this,
//                    getString(R.string.Rating_successfull_message),
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
    }

    private fun update(is_force_update: Boolean) {
        if (is_force_update) {
            onClickSound()
            vibration()
            Toast.makeText(
                this,
                getString(R.string.Update_Check),
                Toast.LENGTH_SHORT
            ).show()
        }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        val listener = { state: InstallState ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackBarForCompleteUpdate()
            }
        }

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            Log.e(TAG, appUpdateInfo.updateAvailability().toString())
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.registerListener(listener)
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this@MainActivity,
                        UPDATE_REQUEST_CODE
                    )
                } catch (exception: IntentSender.SendIntentException) {
                    appUpdateManager.unregisterListener(listener)
                    Log.e(TAG, "callInUpdate : " + exception.message)
                }
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (is_force_update) {
                    Toast.makeText(
                        this,
                        getString(R.string.Open_Play_store),
                        Toast.LENGTH_SHORT
                    ).show()
                    openPlayStore()
                }
            } else {
                if (is_force_update) {
                    Toast.makeText(
                        this,
                        getString(R.string.Already_update),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun popupSnackBarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.context_view),
            getString(R.string.Update_downloading),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            setActionTextColor(resources.getColor(R.color.black))
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        if (requestCode == UPDATE_REQUEST_CODE) {
            Log.e("MY_APP", "Update flow failed! Result code: $resultCode")
            if (resultCode == RESULT_OK) {
                Toast.makeText(
                    this,
                    "Updating the app.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                    Toast.makeText(
                        this,
                        getString(R.string.Open_Play_store),
                        Toast.LENGTH_SHORT
                    ).show()
                    openPlayStore()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.Update_completed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun extractOperands(expression: String, operators: CharArray): List<String> {
        val operands = mutableListOf<String>()
        var currentOperand = ""
        for (char in expression) {
            if (char in operators) {
                // Add the current operand to the list
                if (currentOperand.isNotEmpty()) {
                    operands.add(currentOperand)
                    currentOperand = ""
                }
            } else {
                // Add the current character to the current operand
                currentOperand += char
            }
        }
        // Add the final operand to the list
        if (currentOperand.isNotEmpty()) {
            operands.add(currentOperand)
        }
        return operands
    }

    private fun equalsButtonOnclick() {
        try {

            val dbHelper = CalculatorDatabaseHelper(this)
            val db = dbHelper.writableDatabase



            if (operation == Operation.log || operation == Operation.sqrt || operation == Operation.sin || operation == Operation.cos
                || operation == Operation.tan || operation == Operation.ln || operation == Operation.e || operation == Operation.inv
            ) {
                val ans =
                    if ((floor(calculateExpression()) == ceil(calculateExpression())))
                        calculateExpression()
                            .toString().replace(".0", "")
                    else
                        calculateExpression().toString()
                binding.calculatorDisplayNonMock.text = ans

                val contentValues = ContentValues().apply {
                    put("first_number", firstProcessingNumber)
                    put("operation", getOperatorSymbol(operation.name))
                    put("second_number", secondProcessingNumber)
                    put("equals", "=")
                    put("result", ans.toDouble())
                }
                db.insert("calculations", null, contentValues)

                firstProcessingNumber = calculateExpression()
                secondProcessingNumber = 0.0

                operation = Operation.EMPTY
            }

            if (operation == Operation.FACT) {
                val ans =
                    if ((floor(calculateExpression()) == ceil(calculateExpression())))
                        calculateExpression()
                            .toString().replace(".0", "")
                    else
                        calculateExpression().toString()
                binding.calculatorDisplayNonMock.text = ans
                val contentValues = ContentValues().apply {
                    put("first_number", firstProcessingNumber)
                    put("operation", getOperatorSymbol(operation.name))
                    put("second_number", secondProcessingNumber)
                    put("equals", "=")
                    put("result", ans.toDouble())
                }
                db.insert("calculations", null, contentValues)
                firstProcessingNumber = calculateExpression()
                secondProcessingNumber = 0.0
                operation = Operation.EMPTY
            }

            if (operation == Operation.DVD || operation == Operation.MUL || operation == Operation.POW || operation == Operation.PLUS
                || operation == Operation.MINUS || operation == Operation.nCr || operation == Operation.PERCENT || operation == Operation.mod
            ) {
                if (secondProcessingNumber == 0.0 && operation == Operation.DVD) {
                    val alertBuilder = AlertDialog.Builder(this)
                    alertBuilder.setTitle("Math Error")
                        .setMessage("Can't divide by zero")
                        .setCancelable(true)
                        .setPositiveButton("Ok") { dialogInterface, it ->
                            dialogInterface.cancel()
                        }
                        .show()
                    clearDisplay()

                } else {
                    val ans =
                        if ((floor(calculateExpression()) == ceil(calculateExpression())))
                            calculateExpression()
                                .toString().replace(".0", "")
                        else
                            calculateExpression().toString()
                    // val rnd = ans.toInt()
                    // val ans2 = (round(rnd.toDouble() * 1000.0)/1000.0).toString()
                    binding.calculatorDisplayNonMock.text = ans
                    val contentValues = ContentValues().apply {
                        put("first_number", firstProcessingNumber)
                        put("operation", getOperatorSymbol(operation.name))
                        put("second_number", secondProcessingNumber)
                        put("equals", "=")
                        put("result", ans.toDouble())
                    }
                    db.insert("calculations", null, contentValues)
                    firstProcessingNumber = calculateExpression()
                    secondProcessingNumber = 0.0
                    operation = Operation.EMPTY
                }
            } else {
                val ans =
                    if ((floor(calculateExpression()) == ceil(calculateExpression())))
                        calculateExpression()
                            .toString().replace(".0", "")
                    else
                        calculateExpression().toString()

                binding.calculatorDisplayNonMock.text = calculateExpression().toString()
                Log.d("overflow", calculateExpression().toString())

                if (ans.length > 12) {

                    val number: Double = ans.toDouble()

                    val alertBuilder = AlertDialog.Builder(this)
                    alertBuilder.setTitle("Answer Overflowed")
                        .setMessage("Full answer = $ans")
                        .setCancelable(true)
                        .setPositiveButton("Ok") { dialogInterface, it ->
                            dialogInterface.cancel()
                        }
                        .show()
                    val contentValues = ContentValues().apply {
                        put("first_number", firstProcessingNumber)
                        put("operation", getOperatorSymbol(operation.name))
                        put("second_number", secondProcessingNumber)
                        put("equals", "=")
                        put("result", ans.toDouble())
                    }
                    db.insert("calculations", null, contentValues)
                    val scientificNotation = String.format("%.2e", number)
                    binding.calculatorDisplayNonMock.text = scientificNotation

                } else {
                    val contentValues = ContentValues().apply {
                        put("first_number", firstProcessingNumber)
                        put("operation", getOperatorSymbol(operation.name))
                        put("second_number", secondProcessingNumber)
                        put("equals", "=")
                        put("result", ans.toDouble())
                    }
                    db.insert("calculations", null, contentValues)
                    firstProcessingNumber = calculateExpression()
                    secondProcessingNumber = 0.0
                    operation = Operation.EMPTY
                }

            }

        } catch (e: NumberFormatException) {
            Log.d("ERROR", "Error is" + e)
            binding.calculatorDisplayNonMock.text = "ERROR"
            is_errored_text = true
            clearDisplay(true)
        }
        is_ans_showed = true
    }

    fun extractDouble(input: String): Double? {
        val regex = Regex("-?\\d+\\.?\\d*") // regular expression for matching numerical values
        val matchResult = regex.find(input)
        return matchResult?.value?.toDoubleOrNull()
    }

    fun extractFirstNumberAfterOperator(str: String, searchStr: String): Double? {
        val escapedSearchStr =
            Regex.escape(searchStr) // escape search string to avoid regex syntax errors
        val regex =
            Regex("$escapedSearchStr.*?(\\d+\\.?\\d*)") // regular expression for matching a numerical value after a search string
        val matchResult = regex.find(str)
        return matchResult?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    private fun clearDisplay(
        screen: Boolean = false,
        first_val: Boolean = true,
        second_val: Boolean = true,
        operator: Boolean = true
    ) {
        if (!screen) {
            binding.calculatorDisplayNonMock.text = ""
        }
        if (operator)
            operation = Operation.EMPTY
        if (first_val)
            firstProcessingNumber = 0.0
        if (second_val)
            secondProcessingNumber = 0.0
    }


    private fun calculateExpression(): Double {
        return when (operation) {
            Operation.DVD -> (firstProcessingNumber / secondProcessingNumber * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.MUL -> (firstProcessingNumber * secondProcessingNumber * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.MINUS -> ((firstProcessingNumber - secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.PLUS -> ((firstProcessingNumber + secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.PERCENT -> (firstProcessingNumber / 100 * secondProcessingNumber * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.POW -> (firstProcessingNumber.pow(secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.nCr -> (factorial(firstProcessingNumber) / (factorial(secondProcessingNumber) * factorial(
                firstProcessingNumber - secondProcessingNumber
            )) * 100000000).roundToLong().toDouble() / 100000000

            Operation.FACT -> (factorial(firstProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.log -> (log10(secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.sqrt -> (sqrt(secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.ln -> (log(secondProcessingNumber, 2.7182818284) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.sin -> (Math.round((sin(Math.toRadians(secondProcessingNumber))) * 100000000)
                .toDouble() / 100000000)

            Operation.cos -> (Math.round((cos(Math.toRadians(secondProcessingNumber))) * 100000000)
                .toDouble() / 100000000)

            Operation.tan -> (Math.round((tan(Math.toRadians(secondProcessingNumber))) * 100000000)
                .toDouble() / 100000000)

            Operation.e -> (2.7182818284.pow(secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000

            Operation.mod -> (firstProcessingNumber % secondProcessingNumber)
            Operation.inv -> {
                1.0 / firstProcessingNumber
            }


            else -> firstProcessingNumber
        }
    }

    private fun isAvailableToOperate(operation: Operation) {
        if (binding.calculatorDisplayNonMock.text.toString()
                .isNotEmpty() && binding.calculatorDisplayNonMock.text.toString() != "-"
        ) {
            onClickOperation(operation)
        } else {
            onClickOperation(operation)
        }

    }

    private fun checkOutputScreen(
        screen: Boolean = false,
        first_val: Boolean = true,
        second_val: Boolean = true,
        operator: Boolean = true,
        check_ans: Boolean = true

    ) {
        if (check_ans) {
            if (is_errored_text || is_ans_showed) {
                clearDisplay(
                    screen = screen,
                    first_val = first_val,
                    second_val = second_val,
                    operator = operator
                )
                is_errored_text = false
                is_ans_showed = false
            }
        } else {
            if (is_errored_text) {
                clearDisplay(
                    screen = screen,
                    first_val = first_val,
                    second_val = second_val,
                    operator = operator
                )
                is_errored_text = false
                is_ans_showed = false
            }
        }
    }

    private fun getOperatorSymbol(operation: String): String {
        return when (operation) {
            "DVD" -> "/"
            "MUL" -> "*"
            "PERCENT" -> "%"
            "MINUS" -> "-"
            "PLUS" -> "+"
            "log" -> "log"
            "ln" -> "ln"
            "FACT" -> "!"
            "POW" -> "^"
            "nCr" -> "C"
            "EMPTY" -> ""
            "sqrt" -> "√"
            "sin" -> "sin"
            "cos" -> "cos"
            "tan" -> "tan"
            "e" -> "e^"
            "mod" -> "mod"
            "inv" -> "^-1"
            else -> throw IllegalArgumentException("Invalid operation: $operation")
        }
    }

    private fun convertValue(value: Double): Any {
        return if (value % 1 == 0.0) {
            value.toInt()
        } else {
            value
        }
    };

    private fun onClickOperation(processingOperation: Operation) {
        try {
            if (operation == Operation.EMPTY) {
                if (binding.calculatorDisplayNonMock.text.toString().isNotEmpty()) {
                    firstProcessingNumber =
                        binding.calculatorDisplayNonMock.text.toString().replace(',', '.')
                            .toDouble()
                    operation = processingOperation
                    binding.calculatorDisplayNonMock.text =
                        convertValue(firstProcessingNumber).toString() + getOperatorSymbol(operation.toString())
                } else {
                    operation = processingOperation
                    binding.calculatorDisplayNonMock.text = getOperatorSymbol(operation.toString())
                }
            }
        } catch (e: Exception) {
            Log.d("ERROR", "Error is" + e)
            binding.calculatorDisplayNonMock.text = "ERROR"
            is_errored_text = true
            clearDisplay(true)
        }
    }


    fun factorial(n: Double): Double {
        if (n < 0) {
            val errorMessage = "ERROR"
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            return 0.0
        } else {
            return if (n == 1.00 || n == 0.00) 1.00 else n * factorial(n - 1)
        }

    }

    private fun vibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) { // Vibrator availability checking
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        50,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                ) // New vibrate method for API Level 26 or higher
            } else {
                vibrator.vibrate(140) // Vibrate method for below API Level 26
            }
        }
    }

    private fun onClickSound() {
        try {
            val soundURI =
                ("android.resource://com.mrayush.calculator/" + R.raw.on_click_sound).toUri()
            player = MediaPlayer.create(applicationContext, soundURI)
            player?.isLooping = false
            player?.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initListeners() {
        val group = binding.groupOfNumbers
        val refIds = group.referencedIds
        for (id in refIds) {
            findViewById<View>(id).setOnClickListener {
                vibration()
                checkOutputScreen(first_val = false, operator = false)
                if (operation == Operation.EMPTY) {
                    binding.calculatorDisplayNonMock.text =
                        "${binding.calculatorDisplayNonMock.text.toString()}${(it as? Button)?.text.toString()}"
                } else {
                    binding.calculatorDisplayNonMock.text =
                        "${binding.calculatorDisplayNonMock.text.toString()}${(it as? Button)?.text.toString()}"
                    val extractedText =
                        extractOperands(binding.calculatorDisplayNonMock.text.toString(), operators)
                    if (extractedText.size == 2)
                        secondProcessingNumber = extractedText[1].toDouble()
                    else
                        secondProcessingNumber = extractedText[0].toDouble()
                }
            }
        }

        clearDisplay()
        val history = findViewById<Button>(R.id.HistoryButton)
        history.setOnClickListener {
            Log.d("MainActivity", "moving to Data section")
            val intent = Intent(this@MainActivity, DataScreen::class.java)
            //    dataScreen()
            startActivity(intent)

        }
        binding.logButton.setOnClickListener {
            vibration()
            onClickSound()
            Log.d("log", binding.calculatorDisplayNonMock.text.toString())
            checkOutputScreen(second_val = false, check_ans = false)
            isAvailableToOperate(Operation.log)
        }

        binding.expButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(second_val = false, check_ans = false)
            isAvailableToOperate(Operation.e)
        }

        binding.lnButton.setOnClickListener {
            vibration()
            onClickSound()
            Log.d("ln", binding.calculatorDisplayNonMock.text.toString())
            checkOutputScreen(second_val = false, check_ans = false)
            isAvailableToOperate(Operation.ln)
        }

        binding.factorialButton.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            isAvailableToOperate(Operation.FACT)
        }

        binding.sqrt.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            isAvailableToOperate(Operation.sqrt)

        }

        binding.sinButton.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            isAvailableToOperate(Operation.sin)
        }

        binding.cosButton.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            isAvailableToOperate(Operation.cos)
        }

        binding.tanButton.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            isAvailableToOperate(Operation.tan)
        }

        binding.acButton.setOnClickListener {
            onClickSound()
            vibration()
            clearDisplay()
        }

        binding.permutationButton.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.nCr)
        }

        binding.powerButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.POW)
        }

        binding.commaButton.setOnClickListener {
            vibration()
            checkOutputScreen()
            if (binding.calculatorDisplayNonMock.text.toString()
                    .lastIndexOf(".") != binding.calculatorDisplayNonMock.text.toString().length - 1
            ) {
                if (operation == Operation.EMPTY) {
                    binding.calculatorDisplayNonMock.text =
                        "${binding.calculatorDisplayNonMock.text.toString()}."
                } else {
                    binding.calculatorDisplayNonMock.text =
                        "${binding.calculatorDisplayNonMock.text.toString()}."
                    val extractedText =
                        extractOperands(binding.calculatorDisplayNonMock.text.toString(), operators)
                    if (extractedText.size == 2)
                        secondProcessingNumber = extractedText[1].toDouble()
                    else
                        secondProcessingNumber = extractedText[0].toDouble()
                }
            }
        }

        binding.divideButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.DVD)
        }

        binding.multiplyButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.MUL)
        }

        binding.minusButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            val displayAsString = binding.calculatorDisplayNonMock.text.toString()
            try {
                if (displayAsString.isNotEmpty()) {
                    onClickOperation(Operation.MINUS)
                } else if (displayAsString.isEmpty() && displayAsString != "-"
                ) {
                    binding.calculatorDisplayNonMock.text =
                        "${binding.calculatorDisplayNonMock.text.toString()}-"
                }
            } catch (e: java.lang.NumberFormatException) {
                clearDisplay()
            }
        }

        binding.plusButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.PLUS)
        }
        binding.moduloButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.mod)
        }
        binding.inverseButton.setOnClickListener {
            onClickSound()
            vibration()
            checkOutputScreen(first_val = false, second_val = false, check_ans = false)
            isAvailableToOperate(Operation.inv)
        }

        binding.percentButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(first_val = false, check_ans = false)
            isAvailableToOperate(Operation.PERCENT)
        }

        binding.plusAndMinusButton.setOnClickListener {
            vibration()
            onClickSound()
            checkOutputScreen(screen = false, first_val = false, check_ans = false)



            if (binding.calculatorDisplayNonMock.text.toString().isNotEmpty()) {
                var value: String = binding.calculatorDisplayNonMock.text.toString()
                var lastElement: String = value.substring(value.length - 1, value.length)
                if (lastElement == "+"
                    || lastElement == "^"
                    || lastElement == "/"
                    || lastElement == "*"
                    || lastElement == "-"
                    || lastElement == "%"
                    || lastElement == "!"
                    || lastElement == "C"
                    || lastElement == "e"
                    || lastElement == "√"
                ) {
                    clearDisplay()
                } else if (value.length >= 3 && !binding.calculatorDisplayNonMock.text.isDigitsOnly()) {
                    Log.d("Check", value.length.toString())
                    var value2: String = value.substring(value.length - 3, value.length)
                    Log.d("Check", value2)
                    if (value2 == "sin" || value2 == "cos" || value2 == "tan" || value2 == "3.14159"
                    ) {


                    }
                } else if (binding.calculatorDisplayNonMock.text.isDigitsOnly()) {
                    if (binding.calculatorDisplayNonMock.text.toString()
                            .isNotEmpty() && binding.calculatorDisplayNonMock.text.toString() != "-"
                    ) {
                        firstProcessingNumber =
                            +binding.calculatorDisplayNonMock.text.toString().replace(',', '.')
                                .toDouble() * -1
                        binding.calculatorDisplayNonMock.text =
                            if ((floor(firstProcessingNumber) == ceil(firstProcessingNumber)))
                                firstProcessingNumber
                                    .toString().replace(".0", "")
                            else
                                firstProcessingNumber.toString()
                    }
                } else if (value.substring(value.length - 2, value.length).equals("ln"))
                    clearDisplay()
            }
        }

        binding.equalsButton.setOnClickListener {
            onClickSound()
            vibration()
            equalsButtonOnclick()
        }

        binding.backspace.setOnClickListener {
            onClickSound()
            vibration()
            var value: String = binding.calculatorDisplayNonMock.text.toString()
            if (value.length == 1)
                clearDisplay()
            if (value.length == 3 && (value.substring(value.length - 3, value.length) == "sin" ||
                        value.substring(value.length - 3, value.length) == "cos" ||
                        value.substring(value.length - 3, value.length) == "tan")
            ) {

                value = value.substring(0, value.length - 3)
                binding.calculatorDisplayNonMock.setText(value)
                if (value.isEmpty())
                    clearDisplay()

            } else if (value.length == 7 && value.substring(
                    value.length - 7,
                    value.length
                ) == "3.14159"
            ) {
                value = value.substring(0, value.length - 7)
                binding.calculatorDisplayNonMock.setText(value)
                if (value.isEmpty())
                    clearDisplay()
            } else {
                if (value.isNotEmpty()) {
                    value = value.substring(0, value.length - 1)
                    binding.calculatorDisplayNonMock.text = value
                }
            }
        }

        binding.piButton.setOnClickListener {
            vibration()
            onClickSound()
            if (operation != Operation.EMPTY) {
                if (firstProcessingNumber == 0.0) {
                    binding.calculatorDisplayNonMock.text =
                        getOperatorSymbol(operation.toString()) + "3.14159"
                    secondProcessingNumber = 3.14159
                } else {
                    binding.calculatorDisplayNonMock.text =
                        convertValue(firstProcessingNumber).toString() + getOperatorSymbol(
                            operation.toString()
                        ) + "3.14159"
                    secondProcessingNumber = 3.14159
                }
            } else {
                binding.calculatorDisplayNonMock.text = "3.14159"
            }
        }
    }
}