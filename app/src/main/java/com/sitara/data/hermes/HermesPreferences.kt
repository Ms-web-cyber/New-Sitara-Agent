package com.sitara.data.hermes

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local controls for Hermes. Secrets remain in the configured agent backend, never in this store. */
object HermesPreferences {
    private var prefs: SharedPreferences? = null
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _continuous = MutableStateFlow(true)
    val continuous: StateFlow<Boolean> = _continuous.asStateFlow()
    private val _backendSync = MutableStateFlow(false)
    val backendSync: StateFlow<Boolean> = _backendSync.asStateFlow()
    private val _allowlist = MutableStateFlow(emptySet<String>())
    val allowlist: StateFlow<Set<String>> = _allowlist.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("hermes_evolution", Context.MODE_PRIVATE)
        prefs = p
        _enabled.value = p.getBoolean("enabled", false)
        _continuous.value = p.getBoolean("continuous", true)
        _backendSync.value = p.getBoolean("backend_sync", false)
        _allowlist.value = p.getStringSet("allowlist", emptySet()).orEmpty()
    }
    fun setEnabled(value: Boolean) = save("enabled", value) { _enabled.value = value }
    fun setContinuous(value: Boolean) = save("continuous", value) { _continuous.value = value }
    fun setBackendSync(value: Boolean) = save("backend_sync", value) { _backendSync.value = value }
    fun setAllowed(action: String, allowed: Boolean) {
        val next = _allowlist.value.toMutableSet().apply { if (allowed) add(action) else remove(action) }
        _allowlist.value = next
        prefs?.edit()?.putStringSet("allowlist", next)?.apply()
    }
    private fun save(key: String, value: Boolean, update: () -> Unit) { update(); prefs?.edit()?.putBoolean(key, value)?.apply() }
}
