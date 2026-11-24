package com.sitara.data.hermes

import com.sitara.data.preferences.AuthPreferences
import com.sitara.data.remote.AgyBridgeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Minimal bridge sync for Hermes policy; detailed activity remains on the bridge journal. */
object HermesBridgeClient {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    suspend fun syncPolicy(enabled: Boolean, continuous: Boolean, backendSync: Boolean, allowlist: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("enabled", enabled).put("continuous", continuous).put("backend_sync", backendSync)
                .put("allowed_commands", JSONArray(allowlist.filter { it == "tests" }))
                .put("allowed_domains", JSONArray(allowlist.filter { it == "network" }))
                .put("allowed_capabilities", JSONArray(allowlist))
            val url = AuthPreferences.currentBridgeHttpUrl.trimEnd('/') + "/api/hermes/policy"
            val request = Request.Builder().url(url).post(body.toString().toRequestBody(jsonType)).build()
            AgyBridgeService.instance.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Hermes bridge returned HTTP ${response.code}")
            }
        }
    }
    suspend fun recordOutcome(summary: String, project: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("kind", "learning").put("summary", summary.take(3900)).put("project", project)
            val url = AuthPreferences.currentBridgeHttpUrl.trimEnd('/') + "/api/hermes/event"
            val request = Request.Builder().url(url).post(body.toString().toRequestBody(jsonType)).build()
            AgyBridgeService.instance.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Hermes bridge returned HTTP ${response.code}")
            }
        }
    }
}
