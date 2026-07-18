package io.github.moxisuki.blockprint.cat

import android.content.Intent
import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import io.github.moxisuki.blockprint.cat.app.BlockPrintApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationEventDispatcherOwner {

    override val navigationEventDispatcher = NavigationEventDispatcher()

    private val navigationEventInput by lazy {
        OnBackPressedNavigationEventInput(onBackPressedDispatcher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setViewTreeNavigationEventDispatcherOwner(this)
        navigationEventDispatcher.addInput(navigationEventInput)
        enableEdgeToEdge()

        setContent {
            BlockPrintApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.decorView.setViewTreeNavigationEventDispatcherOwner(null)
        navigationEventDispatcher.removeInput(navigationEventInput)
        navigationEventDispatcher.dispose()
    }
}

private class OnBackPressedNavigationEventInput(
    private val onBackPressedDispatcher: OnBackPressedDispatcher,
) : NavigationEventInput() {

    private val callback = object : OnBackPressedCallback(enabled = false) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            dispatchOnBackStarted(backEvent.toNavigationEvent())
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            dispatchOnBackProgressed(backEvent.toNavigationEvent())
        }

        override fun handleOnBackPressed() {
            dispatchOnBackCompleted()
        }

        override fun handleOnBackCancelled() {
            dispatchOnBackCancelled()
        }
    }

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        onBackPressedDispatcher.addCallback(callback)
    }

    override fun onRemoved() {
        callback.remove()
    }

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        callback.isEnabled = hasEnabledHandlers
    }
}
