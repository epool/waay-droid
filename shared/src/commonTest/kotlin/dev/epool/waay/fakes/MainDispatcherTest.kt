package dev.epool.waay.fakes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base for ViewModel tests: `viewModelScope` uses `Dispatchers.Main.immediate`, so tests swap Main
 * for an [UnconfinedTestDispatcher] (Lackner's testing skill, kotlinx-coroutines-test).
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest {
    protected val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }
}
