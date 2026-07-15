package com.mrayush.calculator

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigInteger
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorFlowTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearHistory() {
        CalculatorDatabaseHelper(context).use { helper ->
            helper.writableDatabase.delete("calculations", null, null)
        }
    }

    @After
    fun cleanUpHistory() = clearHistory()

    @Test
    fun longBinaryResultShowsCompleteAnswerPopup() {
        val expectedResult = BigInteger.valueOf(9).pow(99).toString()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nineButton)).perform(click())
            onView(withId(R.id.powerButton)).perform(click())
            repeat(2) { onView(withId(R.id.nineButton)).perform(click()) }
            onView(withId(R.id.equalsButton)).perform(click())

            onView(withId(R.id.sidebarModalMessage)).check(
                matches(withText(containsString(expectedResult)))
            )
        }
    }

    @Test
    fun fittingResultDoesNotShowPopup() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.twoButton)).perform(click())
            onView(withId(R.id.plusButton)).perform(click())
            onView(withId(R.id.twoButton)).perform(click())
            onView(withId(R.id.equalsButton)).perform(click())

            onView(withId(R.id.calculatorDisplayNonMock)).check(matches(withText("4")))
            onView(withId(R.id.sidebarModalOverlay)).check(doesNotExist())
        }
    }

    @Test
    fun recreationPreservesPendingCalculation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.oneButton)).perform(click())
            onView(withId(R.id.plusButton)).perform(click())
            onView(withId(R.id.twoButton)).perform(click())
            scenario.recreate()
            onView(withId(R.id.equalsButton)).perform(click())
            onView(withId(R.id.calculatorDisplayNonMock)).check(matches(withText("3")))
        }
    }

    @Test
    fun unaryEqualsInsertsOneHistoryRow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.fiveButton)).perform(click())
            onView(withId(R.id.factorialButton)).perform(click())
            onView(withId(R.id.equalsButton)).perform(click())
        }

        CalculatorDatabaseHelper(context).use { helper ->
            helper.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM calculations",
                null
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }
}
