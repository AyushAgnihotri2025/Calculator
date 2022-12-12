package com.mrayush.calculator

import android.app.ActionBar
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.SyncStateContract.Helpers.update
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity() {

    private var operation: Operation = Operation.EMPTY
    private var firstProcessingNumber = 0.0
    private var secondProcessingNumber = 0.0
    private var is_errored_text = false
    private var is_ans_showed = false
    lateinit var drawerlayout : DrawerLayout
    lateinit var navigationView : NavigationView
    lateinit var toolbar : Toolbar
    private var btnShowDialog: Button? = null

    private var reviewInfo : ReviewInfo? = null
    private var manager : ReviewManager? = null

    private val TAG = "Update_Button"
    private var UPDATE_REQUEST_CODE = 100
    private lateinit var appUpdateManager : AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* appUpdateManager = FakeAppUpdateManager(this)
        (appUpdateManager as FakeAppUpdateManager).setUpdateAvailable(2)
        */

        appUpdateManager = AppUpdateManagerFactory.create(this)

        initListeners()
        activateReviewInfo()
        update(false)

        drawerlayout = findViewById(R.id.menu_drawer)
        navigationView = findViewById(R.id.navigation_menu)
        toolbar = findViewById(R.id.menuButton)
        setSupportActionBar(toolbar)

        val toggle : ActionBarDrawerToggle = ActionBarDrawerToggle(this, drawerlayout, toolbar, R.string.navigation_open, R.string.navigation_close)

        drawerlayout.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()

        navigationView = findViewById<View>(R.id.navigation_menu) as NavigationView

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.developer -> portFolioIntent()
                R.id.share ->  share()
                R.id.feedback -> startReviewFlow()
                R.id.update -> update(true)
                R.id.source_code -> showDialog()
                R.id.about_us -> aboutUsIntent()
            }
            closeDrawer()
            false
        }

    }

    private fun aboutUsIntent(){
        val aboutUsIntent = Intent(this@MainActivity, AboutUs::class.java)
        startActivity(aboutUsIntent)
    }

    private fun portFolioIntent(){
        val portFolioIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mrayush.me/?refer=calculator-"+getString(R.string.appVersion)))
        startActivity(portFolioIntent)
    }

    private fun share() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, getPackageName()))
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
    }

    private fun showDialog() {
        val share: Button
        val close: ImageButton
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.popup)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        val window = dialog.window
        window!!.setGravity(Gravity.CENTER)
        window.attributes.windowAnimations = R.style.DialogAnimation
        share = dialog.findViewById(R.id.popupShareBtn)
        close = dialog.findViewById(R.id.closePopup)
        share.setOnClickListener {
            share()
        }
        close.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setCancelable(true)
        window.setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun closeDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.menu_drawer)
        drawer.closeDrawer(GravityCompat.START)
    }


    private fun openPlayStore() {
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+getPackageName()))
        startActivity(playStoreIntent)
    }

    private fun activateReviewInfo() {
        manager = ReviewManagerFactory.create(this)
        val managerInfoTask: Task<ReviewInfo> = manager!!.requestReviewFlow()
        managerInfoTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewInfo = task.result
            } else {
                Toast.makeText(this, "Failed to Rate the app. Opening PlayStore.", Toast.LENGTH_SHORT).show()
                openPlayStore()
            }
        }
    }

    private fun startReviewFlow() {
        if (reviewInfo != null) {
            val flow: Task<Void> = manager!!.launchReviewFlow(this, reviewInfo!!)
            flow.addOnCompleteListener { task ->
                Toast.makeText(
                    this,
                    "Thank you so much for rating the app.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun update(is_force_update: Boolean) {
        if (is_force_update) {
            Toast.makeText(
                this,
                "Checking for an update.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        val listener = { state: InstallState  ->
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
                } catch(exception: IntentSender.SendIntentException) {
                    appUpdateManager.unregisterListener(listener)
                    Log.e(TAG, "callInUpdate : "+exception.message)
                }
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (is_force_update) {
                    Toast.makeText(
                        this,
                        "Unable to update app from Inside. Opening Google PlayStore.",
                        Toast.LENGTH_SHORT
                    ).show()
                    openPlayStore()
                }
            } else {
                if (is_force_update) {
                    Toast.makeText(
                        this,
                        "App is already updated UP-TO date.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun popupSnackBarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.context_view),
            "An update has just been downloaded.",
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
            } else if (resultCode != RESULT_OK) {
                if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                    Toast.makeText(
                        this,
                        "Unable to update app from Inside. Opening Google PlayStore.",
                        Toast.LENGTH_SHORT
                    ).show()
                    openPlayStore()
                } else {
                    Toast.makeText(
                        this,
                        "Update successfully cancelled.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun equalsButtonOnclick() {
        try {
            Log.d("MM", calculatorDisplayNonMock.text.toString().toBigDecimal().toString())
            secondProcessingNumber =
                calculatorDisplayNonMock.text.toString().replace(',', '.').toDouble()
            if (secondProcessingNumber == 0.0 && operation == Operation.DIVIDE) {
                var alertBuilder = AlertDialog.Builder(this)
                alertBuilder.setTitle("Math Error")
                    .setMessage("Can't divide by zero")
                    .setCancelable(true)
                    .setPositiveButton("Ok"){dialogInterface, it ->
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
                calculatorDisplayNonMock.text =
                    if (ans.length > 9)
                        "OVERFLOW"
                    else
                        ans
                if (ans.length > 9){
                    is_errored_text = true
                    clearDisplay(true)
                    Toast.makeText(
                        this@MainActivity,
                        "Answer is : "+ans,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    firstProcessingNumber = calculateExpression()
                    secondProcessingNumber = 0.0
                }
            }
            operation = Operation.EMPTY
        } catch (e: NumberFormatException) {
            calculatorDisplayNonMock.text = "ERROR"
            is_errored_text = true
            clearDisplay(true)
        }
        is_ans_showed = true
    }

    private fun clearDisplay(
        screen: Boolean = false,
        first_val: Boolean = true,
        second_val: Boolean = true,
        operator: Boolean = true
    ) {
        if (!screen) {
            calculatorDisplayNonMock.text = ""
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
            Operation.DIVIDE -> (firstProcessingNumber / secondProcessingNumber * 100000000).roundToLong()
                .toDouble() / 100000000
            Operation.MULTIPLY -> (firstProcessingNumber * secondProcessingNumber * 100000000).roundToLong()
                .toDouble() / 100000000
            Operation.MINUS -> ((firstProcessingNumber - secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000
            Operation.PLUS -> ((firstProcessingNumber + secondProcessingNumber) * 100000000).roundToLong()
                .toDouble() / 100000000
            Operation.PERCENT -> (firstProcessingNumber / 100 * secondProcessingNumber * 100000000).roundToLong()
                .toDouble() / 100000000
            else -> firstProcessingNumber
        }
    }

    private fun isAvailableToOperate(operation: Operation) {
        if (calculatorDisplayNonMock.text.toString()
                .isNotEmpty() && calculatorDisplayNonMock.text.toString() != "-"
        ) {
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

    private fun onClickOperation(processingOperation: Operation) {
        try {
            if (operation == Operation.EMPTY) {
                if (calculatorDisplayNonMock.text.toString().isNotEmpty()) {
                    firstProcessingNumber =
                        calculatorDisplayNonMock.text.toString().replace(',', '.').toDouble()
                    calculatorDisplayNonMock.text = ""
                    operation = processingOperation
                }
            }
        } catch (e: Exception){
            calculatorDisplayNonMock.text = "ERROR"
            is_errored_text = true
            clearDisplay(true)
        }
    }

    private fun initListeners() {
        val group = groupOfNumbers
        val refIds = group.referencedIds
        for (id in refIds) {
            findViewById<View>(id).setOnClickListener {
                checkOutputScreen(first_val=false, operator = false)
                calculatorDisplayNonMock.text =
                    "${calculatorDisplayNonMock.text.toString()}${(it as? Button)?.text.toString()}"
            }
        }

        clearDisplay()

        acButton.setOnClickListener {
            clearDisplay()
        }

        commaButton.setOnClickListener {
            checkOutputScreen()
            if (calculatorDisplayNonMock.text.toString()
                    .lastIndexOf(".") != calculatorDisplayNonMock.text.toString().length - 1
            )
                calculatorDisplayNonMock.text =
                    "${calculatorDisplayNonMock.text.toString()}."
        }

        divideButton.setOnClickListener {
            checkOutputScreen(first_val=false, check_ans=false)
            isAvailableToOperate(Operation.DIVIDE)
        }

        multiplyButton.setOnClickListener {
            checkOutputScreen(first_val=false, check_ans=false)
            isAvailableToOperate(Operation.MULTIPLY)

        }

        minusButton.setOnClickListener {
            checkOutputScreen(first_val=false, check_ans=false)
            val displayAsString = calculatorDisplayNonMock.text.toString()
            try {
                if (displayAsString.isNotEmpty()) {
                    onClickOperation(Operation.MINUS)
                } else if (displayAsString.isEmpty() && displayAsString != "-"
                ) {
                    calculatorDisplayNonMock.text =
                        "${calculatorDisplayNonMock.text.toString()}-"
                }
            } catch (e: java.lang.NumberFormatException) {
                clearDisplay()
            }
        }

        plusButton.setOnClickListener {
            checkOutputScreen(first_val=false, check_ans=false)
            isAvailableToOperate(Operation.PLUS)

        }

        percentButton.setOnClickListener {
            checkOutputScreen(first_val=false, check_ans=false)
            isAvailableToOperate(Operation.PERCENT)

        }

        plusAndMinusButton.setOnClickListener {
            checkOutputScreen(screen = false, first_val = false, check_ans=false)
            if (calculatorDisplayNonMock.text.toString()
                    .isNotEmpty() && calculatorDisplayNonMock.text.toString() != "-"
            ) {
                firstProcessingNumber =
                    +calculatorDisplayNonMock.text.toString().replace(',', '.').toDouble() * -1
                calculatorDisplayNonMock.text =
                    if ((floor(firstProcessingNumber) == ceil(firstProcessingNumber)))
                        firstProcessingNumber
                            .toString().replace(".0", "")
                    else
                        firstProcessingNumber.toString()
            }
        }

        equalsButton.setOnClickListener {
            equalsButtonOnclick()
        }
    }
}
