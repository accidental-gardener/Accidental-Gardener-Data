package com.accidentalgardener.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Keeps the admin sign-in going outside the screen. On a phone, GitHub opens on top of the app and
 * Android can close or rebuild the app's screen while you're there. The waiting now lives here and the
 * code is saved, so when you come back it picks up where it left off and finishes signing in.
 */
object AdminSignIn {
    sealed interface State {
        data object Idle : State
        data object Starting : State
        data class Waiting(val code: AdminApi.DeviceCode, val checking: Boolean = false) : State
        data object SignedIn : State
        data class Error(val message: String) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun start(ctx: Context) {
        val app = ctx.applicationContext
        job?.cancel()
        _state.value = State.Starting
        job = scope.launch {
            try {
                val dc = AdminApi.startSignIn()
                AdminApi.savePending(app, dc)
                _state.value = State.Waiting(dc)
                poll(app, dc)
            } catch (e: Exception) {
                _state.value = State.Error("Couldn't reach GitHub. Check your connection.")
            }
        }
    }

    /** Call when the admin screen appears or the app comes back. Picks up a saved sign-in. */
    fun resume(ctx: Context) {
        val app = ctx.applicationContext
        if (AdminApi.login(app) != null) { _state.value = State.SignedIn; return }
        val dc = AdminApi.pending(app) ?: return
        if (job?.isActive == true) { checkNow(app); return }
        _state.value = State.Waiting(dc)
        job = scope.launch { poll(app, dc) }
    }

    /** The "I've approved it" button. Asks GitHub straight away. */
    fun checkNow(ctx: Context) {
        val app = ctx.applicationContext
        val dc = (state.value as? State.Waiting)?.code ?: AdminApi.pending(app) ?: return
        scope.launch {
            _state.value = State.Waiting(dc, checking = true)
            handle(app, runCatching { AdminApi.checkOnce(app, dc) }.getOrElse { AdminApi.Check.Pending }, dc)
        }
    }

    fun cancel(ctx: Context) {
        job?.cancel()
        AdminApi.clearPending(ctx.applicationContext)
        _state.value = State.Idle
    }

    fun signedOut() { _state.value = State.Idle }

    /** Opens GitHub in the phone's own browser, not inside this app, so it can't take the app down with it. */
    fun openGitHub(ctx: Context, code: AdminApi.DeviceCode) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(code.verifyUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
    }

    private suspend fun poll(app: Context, dc: AdminApi.DeviceCode) {
        var wait = dc.intervalSec
        while (System.currentTimeMillis() < dc.expiresAt) {
            delay(wait * 1000L)
            val r = runCatching { AdminApi.checkOnce(app, dc) }.getOrElse { AdminApi.Check.Pending }
            if (r == AdminApi.Check.SlowDown) wait += 5
            if (handle(app, r, dc)) return
        }
        handle(app, AdminApi.Check.Failed("The code ran out. Please start again."), dc)
    }

    /** Returns true when the sign-in is finished one way or the other. */
    private fun handle(app: Context, r: AdminApi.Check, dc: AdminApi.DeviceCode): Boolean = when {
        _state.value is State.SignedIn || AdminApi.login(app) != null -> { _state.value = State.SignedIn; true }
        else -> handleResult(app, r, dc)
    }

    private fun handleResult(app: Context, r: AdminApi.Check, dc: AdminApi.DeviceCode): Boolean = when (r) {
        AdminApi.Check.Done -> { _state.value = State.SignedIn; true }
        is AdminApi.Check.Failed -> { AdminApi.clearPending(app); _state.value = State.Error(r.message); true }
        else -> { _state.value = State.Waiting(dc); false }
    }
}
