package com.mrayush.calculator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
import android.os.VibratorManager
import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.mrayush.calculator.databinding.ActivityMainBinding
import com.mrayush.calculator.screens.DataScreen
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var operation: Operation = Operation.EMPTY
    private var firstOperandText = ""
    private var secondOperandText = ""
    private var lastFullResult = ""
    private var is_errored_text = false
    private var is_ans_showed = false
    lateinit var drawerlayout: DrawerLayout
    lateinit var navigationView: NavigationView
    lateinit var toolbar: Toolbar
    private val TAG = "Update_Button"
    private var UPDATE_REQUEST_CODE = 100
    private lateinit var appUpdateManager: AppUpdateManager
    private var player: MediaPlayer? = null
    private var darkmode = false
    private var activeSidebarModal: View? = null
    private var mockTextSizeSyncPending = false
    private val updateInstallStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackBarForCompleteUpdate()
        }
    }

    companion object {
        private const val MAX_OPERAND_LENGTH = 100
        private const val PREVIEW_MAX_LENGTH = 12
        private const val FALLBACK_DISPLAY_LENGTH = 18
        private const val STATE_FIRST_OPERAND = "calculator_first_operand"
        private const val STATE_SECOND_OPERAND = "calculator_second_operand"
        private const val STATE_OPERATION = "calculator_operation"
        private const val STATE_FULL_RESULT = "calculator_full_result"
        private const val STATE_ERROR = "calculator_error"
        private const val STATE_ANSWER_SHOWN = "calculator_answer_shown"

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
        outState.putString(STATE_FIRST_OPERAND, firstOperandText)
        outState.putString(STATE_SECOND_OPERAND, secondOperandText)
        outState.putString(STATE_OPERATION, operation.name)
        outState.putString(STATE_FULL_RESULT, lastFullResult)
        outState.putBoolean(STATE_ERROR, is_errored_text)
        outState.putBoolean(STATE_ANSWER_SHOWN, is_ans_showed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loadDayNight()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        keepDisplayMockTextSizeAligned()
        initListeners()
        restoreCalculatorState(savedInstanceState)

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

    private fun keepDisplayMockTextSizeAligned() {
        binding.calculatorDisplayNonMock.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(text: Editable?) {
                scheduleDisplayMockTextSizeSync()
            }
        })
        scheduleDisplayMockTextSizeSync()
    }

    private fun scheduleDisplayMockTextSizeSync() {
        if (mockTextSizeSyncPending) return
        mockTextSizeSyncPending = true
        val display = binding.calculatorDisplayNonMock
        display.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (display.viewTreeObserver.isAlive) {
                        display.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    mockTextSizeSyncPending = false
                    val textSizePx = display.textSize
                    binding.calculatorDisplayMock.apply {
                        text = if (display.text.length > 16) {
                            "8".repeat(display.text.length)
                        } else {
                            getString(R.string.calculatorDisplayMock)
                        }
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                    }
                    binding.calculatorDisplayMockCommas.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        textSizePx
                    )
                    return true
                }
            }
        )
    }

    private fun ReportBug() {
        vibration()
        onClickSound()
        val recipient = "ayushagnihotri2025@gmail.com"
        val subject = "Report for the Bug - Calculator Application"
        val message = "Email message body"

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
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
        setDayNight(sharedPreferences.getString("My_DayNight", "").orEmpty())
    }


    // load DayNight actually
    private fun daynight() {
//        vibration()
        val swtch: SwitchCompat? = findViewById(R.id.daynight)

        // Load the state of the switch from SharedPreferences
        val sharedPreferences = getSharedPreferences("DayNight", MODE_PRIVATE)
        val switchState = sharedPreferences.getBoolean("My_Switch", false)

        if (swtch != null) {
            swtch.isChecked = switchState
        }

        swtch?.setOnCheckedChangeListener { compoundButton, _ ->
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
        darkmode = daynightMode == "yes"
        getSharedPreferences("DayNight", MODE_PRIVATE).edit {
            putString("My_DayNight", daynightMode)
        }
        if (daynightMode == "yes") {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        }
    }

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

    private fun openPlayStore() {
        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        )
        startActivity(playStoreIntent)
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

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            Log.e(TAG, appUpdateInfo.updateAvailability().toString())
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.registerListener(updateInstallStateListener)
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this@MainActivity,
                        UPDATE_REQUEST_CODE
                    )
                } catch (exception: IntentSender.SendIntentException) {
                    appUpdateManager.unregisterListener(updateInstallStateListener)
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
            setAction(R.string.restart) { appUpdateManager.completeUpdate() }
            setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            Log.e("MY_APP", "Update flow failed! Result code: $resultCode")
            if (resultCode == RESULT_OK) {
                Toast.makeText(
                    this,
                    R.string.updating_app,
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

    private fun restoreCalculatorState(savedState: Bundle?) {
        if (savedState == null) {
            clearDisplay()
            return
        }
        firstOperandText = savedState.getString(STATE_FIRST_OPERAND).orEmpty()
        secondOperandText = savedState.getString(STATE_SECOND_OPERAND).orEmpty()
        operation = runCatching {
            Operation.valueOf(savedState.getString(STATE_OPERATION).orEmpty())
        }.getOrDefault(Operation.EMPTY)
        lastFullResult = savedState.getString(STATE_FULL_RESULT).orEmpty()
        is_errored_text = savedState.getBoolean(STATE_ERROR)
        is_ans_showed = savedState.getBoolean(STATE_ANSWER_SHOWN)
        if (is_errored_text) {
            binding.calculatorDisplayNonMock.setText(R.string.error_display)
        } else if (is_ans_showed && lastFullResult.isNotEmpty()) {
            binding.calculatorDisplayNonMock.text = compactPreview(lastFullResult)
        } else {
            renderInput()
        }
    }

    private fun equalsButtonOnclick() {
        if (operation == Operation.EMPTY) return

        val evaluatedOperation = operation
        val historyFirst = if (isUnary(evaluatedOperation)) {
            secondOperandText.ifEmpty { firstOperandText }
        } else {
            firstOperandText
        }
        val historySecond = if (isUnary(evaluatedOperation)) "" else secondOperandText

        when (
            val outcome = CalculatorEngine.evaluate(
                evaluatedOperation,
                firstOperandText,
                secondOperandText
            )
        ) {
            is CalculationOutcome.Error -> showCalculationError(outcome.type)
            is CalculationOutcome.Success -> {
                val fullResult = CalculatorEngine.format(outcome.value)
                runCatching {
                    CalculatorDatabaseHelper(this).use { helper ->
                        helper.insertCalculation(
                            firstText = historyFirst,
                            operation = getOperatorSymbol(evaluatedOperation),
                            secondText = historySecond,
                            resultText = fullResult
                        )
                    }
                }.onFailure { exception ->
                    Log.e("MainActivity", "Unable to save calculation history", exception)
                }
                firstOperandText = fullResult
                secondOperandText = ""
                operation = Operation.EMPTY
                lastFullResult = fullResult
                is_errored_text = false
                is_ans_showed = true
                presentResult(fullResult)
            }
        }
    }

    private fun showCalculationError(error: CalculationError) {
        val message = when (error) {
            CalculationError.INVALID_NUMBER -> R.string.error_invalid_number
            CalculationError.DIVISION_BY_ZERO -> R.string.error_division_by_zero
            CalculationError.DOMAIN -> R.string.error_domain
            CalculationError.INTEGER_REQUIRED -> R.string.error_integer_required
            CalculationError.RESULT_TOO_LARGE -> R.string.error_result_too_large
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.math_error)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        clearDisplay()
        binding.calculatorDisplayNonMock.setText(R.string.error_display)
        is_errored_text = true
    }

    private fun presentResult(fullResult: String) {
        val display = binding.calculatorDisplayNonMock
        if (display.width > 0 && !resultFitsDisplay(fullResult)) {
            binding.calculatorDisplayNonMock.text = compactPreview(fullResult)
            showFullResultModal(fullResult)
            return
        }

        display.text = fullResult
        display.post {
            if (!resultFitsDisplay(fullResult)) {
                display.text = compactPreview(fullResult)
                showFullResultModal(fullResult)
            }
        }
    }

    private fun resultFitsDisplay(text: String): Boolean {
        val display = binding.calculatorDisplayNonMock
        val availableWidth = display.width - display.paddingLeft - display.paddingRight
        if (availableWidth <= 0) return text.length <= FALLBACK_DISPLAY_LENGTH

        val minimumTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            26f,
            resources.displayMetrics
        )
        val measuringPaint = TextPaint(display.paint).apply { textSize = minimumTextSize }
        return measuringPaint.measureText(text) <= availableWidth
    }

    private fun compactPreview(
        fullResult: String,
        maxLength: Int = PREVIEW_MAX_LENGTH
    ): String {
        val value = fullResult.toBigDecimalOrNull() ?: return fullResult.take(maxLength)
        if (fullResult.length <= maxLength) return fullResult

        val normalized = value.stripTrailingZeros()
        val exponent = normalized.precision() - normalized.scale() - 1
        val digits = normalized.unscaledValue().abs().toString().padEnd(5, '0')
        val sign = if (normalized.signum() < 0) "-" else ""
        val availableFractionDigits =
            (maxLength - sign.length - exponent.toString().length - 3).coerceIn(0, 4)

        val fraction = digits.drop(1).take(availableFractionDigits)
        val coefficient = if (fraction.isEmpty()) digits.take(1) else "${digits[0]}.$fraction"
        return "$sign${coefficient}e$exponent".take(maxLength)
    }

    private fun showFullResultModal(fullResult: String) {
        val modal = createSidebarModal()
        modal.findViewById<TextView>(R.id.sidebarModalTitle)
            .setText(R.string.full_result_title)
        modal.findViewById<TextView>(R.id.sidebarModalMessage).apply {
            text = getString(R.string.full_result_message, fullResult)
            setTextIsSelectable(true)
        }
        modal.findViewById<Button>(R.id.sidebarModalCancel)
            .setText(R.string.close)
        modal.findViewById<Button>(R.id.sidebarModalAction).apply {
            setText(R.string.copy)
            setOnClickListener {
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.calculator_result), fullResult)
                )
                Toast.makeText(this@MainActivity, R.string.result_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearDisplay() {
        operation = Operation.EMPTY
        firstOperandText = ""
        secondOperandText = ""
        lastFullResult = ""
        is_errored_text = false
        is_ans_showed = false
        binding.calculatorDisplayNonMock.text = ""
    }

    private fun getOperatorSymbol(operation: Operation): String {
        return when (operation) {
            Operation.DVD -> "/"
            Operation.MUL -> "*"
            Operation.PERCENT -> "%"
            Operation.MINUS -> "-"
            Operation.PLUS -> "+"
            Operation.log -> "log"
            Operation.ln -> "ln"
            Operation.FACT -> "!"
            Operation.POW -> "^"
            Operation.nCr -> "C"
            Operation.EMPTY -> ""
            Operation.sqrt -> "√"
            Operation.sin -> "sin"
            Operation.cos -> "cos"
            Operation.tan -> "tan"
            Operation.e -> "e^"
            Operation.mod -> "mod"
            Operation.inv -> "^-1"
        }
    }

    private fun selectOperation(selectedOperation: Operation) {
        if (is_errored_text) clearDisplay()
        if (operation != Operation.EMPTY && secondOperandText.isEmpty()) {
            operation = selectedOperation
            renderInput()
            return
        }
        if (operation != Operation.EMPTY) {
            equalsButtonOnclick()
            if (operation != Operation.EMPTY) return
        }
        if (firstOperandText.isEmpty()) {
            if (selectedOperation == Operation.MINUS) {
                firstOperandText = "-"
            } else if (!isUnary(selectedOperation)) {
                return
            }
        }
        operation = selectedOperation
        secondOperandText = ""
        is_ans_showed = false
        renderInput()
    }

    private fun appendDigit(digit: String) {
        prepareForFreshInput()
        if (operation == Operation.EMPTY || isUnary(operation)) {
            val candidate = firstOperandText + digit
            if (candidate.length <= MAX_OPERAND_LENGTH &&
                inputFitsDisplay(candidate, secondOperandText)
            ) {
                firstOperandText = candidate
            }
        } else {
            val candidate = secondOperandText + digit
            if (candidate.length <= MAX_OPERAND_LENGTH &&
                inputFitsDisplay(firstOperandText, candidate)
            ) {
                secondOperandText = candidate
            }
        }
        renderInput()
    }

    private fun appendDecimal() {
        prepareForFreshInput()
        if (operation == Operation.EMPTY || isUnary(operation)) {
            if (!firstOperandText.contains('.')) {
                val candidate = if (firstOperandText.isEmpty() || firstOperandText == "-") {
                    "${firstOperandText}0."
                } else {
                    "$firstOperandText."
                }
                if (inputFitsDisplay(candidate, secondOperandText)) {
                    firstOperandText = candidate
                }
            }
        } else if (!secondOperandText.contains('.')) {
            val candidate = if (secondOperandText.isEmpty() || secondOperandText == "-") {
                "${secondOperandText}0."
            } else {
                "$secondOperandText."
            }
            if (inputFitsDisplay(firstOperandText, candidate)) {
                secondOperandText = candidate
            }
        }
        renderInput()
    }

    private fun prepareForFreshInput() {
        if (is_errored_text || (is_ans_showed && operation == Operation.EMPTY)) {
            clearDisplay()
        }
    }

    private fun toggleSign() {
        prepareForFreshInput()
        if (operation == Operation.EMPTY || isUnary(operation)) {
            val candidate = toggleNumberSign(firstOperandText)
            if (inputFitsDisplay(candidate, secondOperandText)) firstOperandText = candidate
        } else {
            val candidate = toggleNumberSign(secondOperandText)
            if (inputFitsDisplay(firstOperandText, candidate)) secondOperandText = candidate
        }
        renderInput()
    }

    private fun toggleNumberSign(value: String): String = when {
        value.startsWith("-") -> value.drop(1)
        value.isEmpty() -> "-"
        else -> "-$value"
    }

    private fun backspaceInput() {
        if (is_errored_text || is_ans_showed) {
            clearDisplay()
            return
        }
        when {
            secondOperandText.isNotEmpty() -> secondOperandText = secondOperandText.dropLast(1)
            operation != Operation.EMPTY -> operation = Operation.EMPTY
            firstOperandText.isNotEmpty() -> firstOperandText = firstOperandText.dropLast(1)
        }
        renderInput()
    }

    private fun insertPi() {
        prepareForFreshInput()
        val pi = "3.1415926535"
        if (operation == Operation.EMPTY || isUnary(operation)) {
            firstOperandText = pi
        } else {
            secondOperandText = pi
        }
        renderInput()
    }

    private fun renderInput() {
        binding.calculatorDisplayNonMock.text =
            inputExpression(firstOperandText, secondOperandText)
    }

    private fun inputFitsDisplay(first: String, second: String): Boolean {
        val visibleText = if (
            operation != Operation.EMPTY &&
            !isUnary(operation) &&
            second.isNotEmpty()
        ) {
            second
        } else {
            inputExpression(first, second)
        }
        return resultFitsDisplay(visibleText)
    }

    private fun inputExpression(first: String, second: String): String {
        var visibleFirst = if (
            operation != Operation.EMPTY &&
            first == lastFullResult &&
            !resultFitsDisplay(first)
        ) {
            compactPreview(first, 10)
        } else {
            first
        }

        fun buildExpression(): String = when {
            operation == Operation.EMPTY -> visibleFirst
            operation == Operation.FACT -> "$visibleFirst!"
            operation == Operation.inv -> "$visibleFirst^-1"
            isUnary(operation) -> "${getOperatorSymbol(operation)}$visibleFirst"
            else -> "$visibleFirst${getOperatorSymbol(operation)}$second"
        }

        var expression = buildExpression()
        if (
            operation != Operation.EMPTY &&
            !isUnary(operation) &&
            second.isNotEmpty() &&
            !resultFitsDisplay(expression)
        ) {
            return second
        }
        if (
            operation != Operation.EMPTY &&
            !resultFitsDisplay(expression) &&
            first.toBigDecimalOrNull() != null
        ) {
            visibleFirst = compactPreview(first, 8)
            expression = buildExpression()
        }
        return expression
    }

    private fun isUnary(value: Operation): Boolean = value in setOf(
        Operation.log,
        Operation.ln,
        Operation.FACT,
        Operation.sqrt,
        Operation.sin,
        Operation.cos,
        Operation.tan,
        Operation.e,
        Operation.inv
    )

    private fun vibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as Vibrator)
        }
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    private fun onClickSound() {
        try {
            player?.release()
            val soundURI =
                ("android.resource://com.mrayush.calculator/" + R.raw.on_click_sound).toUri()
            player = MediaPlayer.create(applicationContext, soundURI)?.apply {
                isLooping = false
                setOnCompletionListener { completedPlayer ->
                    completedPlayer.release()
                    if (player === completedPlayer) player = null
                }
                start()
            }
        } catch (exception: Exception) {
            Log.w("MainActivity", "Unable to play button sound", exception)
        }
    }

    override fun onDestroy() {
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(updateInstallStateListener)
        }
        player?.release()
        player = null
        activeSidebarModal = null
        super.onDestroy()
    }

    private fun initListeners() {
        val digitButtonIds = intArrayOf(
            R.id.zeroButton,
            R.id.oneButton,
            R.id.twoButton,
            R.id.threeButton,
            R.id.fourButton,
            R.id.fiveButton,
            R.id.sixButton,
            R.id.sevenButton,
            R.id.eightButton,
            R.id.nineButton
        )
        for (id in digitButtonIds) {
            findViewById<View>(id).setOnClickListener {
                vibration()
                appendDigit((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.HistoryButton).setOnClickListener {
            Log.d("MainActivity", "moving to Data section")
            startActivity(Intent(this@MainActivity, DataScreen::class.java))
        }

        binding.acButton.setOnClickListener {
            calculatorFeedback()
            clearDisplay()
        }

        binding.commaButton.setOnClickListener {
            vibration()
            appendDecimal()
        }

        binding.plusAndMinusButton.setOnClickListener {
            calculatorFeedback()
            toggleSign()
        }

        binding.equalsButton.setOnClickListener {
            calculatorFeedback()
            equalsButtonOnclick()
        }

        binding.backspace.setOnClickListener {
            calculatorFeedback()
            backspaceInput()
        }

        binding.piButton.setOnClickListener {
            calculatorFeedback()
            insertPi()
        }

        bindOperationButtons()
    }

    private fun bindOperationButtons() {
        fun bind(view: View, selectedOperation: Operation) {
            view.setOnClickListener {
                calculatorFeedback()
                selectOperation(selectedOperation)
            }
        }

        bind(binding.logButton, Operation.log)
        bind(binding.expButton, Operation.e)
        bind(binding.lnButton, Operation.ln)
        bind(binding.factorialButton, Operation.FACT)
        bind(binding.sqrt, Operation.sqrt)
        bind(binding.sinButton, Operation.sin)
        bind(binding.cosButton, Operation.cos)
        bind(binding.tanButton, Operation.tan)
        bind(binding.permutationButton, Operation.nCr)
        bind(binding.powerButton, Operation.POW)
        bind(binding.divideButton, Operation.DVD)
        bind(binding.multiplyButton, Operation.MUL)
        bind(binding.minusButton, Operation.MINUS)
        bind(binding.plusButton, Operation.PLUS)
        bind(binding.moduloButton, Operation.mod)
        bind(binding.inverseButton, Operation.inv)
        bind(binding.percentButton, Operation.PERCENT)
    }

    private fun calculatorFeedback() {
        onClickSound()
        vibration()
    }
}