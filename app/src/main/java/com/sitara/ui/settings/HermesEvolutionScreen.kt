package com.sitara.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitara.data.hermes.HermesPreferences
import com.sitara.data.hermes.HermesBridgeClient

@Composable
fun HermesEvolutionEntryRow(onClick: () -> Unit) {
    val enabled by HermesPreferences.enabled.collectAsState()
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                Text("Hermes evolution", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(if (enabled) "On · learning and autonomous improvements" else "Off · controlled self-learning", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HermesEvolutionScreen() {
    val enabled by HermesPreferences.enabled.collectAsState(); val continuous by HermesPreferences.continuous.collectAsState()
    val backendSync by HermesPreferences.backendSync.collectAsState(); val allowlist by HermesPreferences.allowlist.collectAsState()
    LaunchedEffect(enabled, continuous, backendSync, allowlist) {
        HermesBridgeClient.syncPolicy(enabled, continuous, backendSync, allowlist)
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Hermes self-evolution", fontWeight = FontWeight.SemiBold); Text("Sitara learns from completed work and errors.", fontSize = 12.sp, color = muted) }; Switch(enabled, HermesPreferences::setEnabled) }
                Text("Every bridge-side change is journaled with its reason. High-risk actions still need an allowlist or approval.", fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (enabled) {
            SettingSwitch("Keep Hermes active", "Review queued outcomes while the app is running.", continuous, HermesPreferences::setContinuous)
            SettingSwitch("Sync learning to agent backend", "Sends structured learning records to your configured backend.", backendSync, HermesPreferences::setBackendSync)
            Text("Auto-allowlist", fontWeight = FontWeight.SemiBold)
            Text("Only actions selected here may run without asking. Payments, credentials, app releases and root access never become automatic.", fontSize = 12.sp, color = muted)
            listOf("Workspace file changes" to "files", "Configured test commands" to "tests", "Configured network domains" to "network", "Git changes without push" to "git").forEach { (label,key) -> SettingSwitch(label, "Allow Hermes to use this configured capability.", key in allowlist) { HermesPreferences.setAllowed(key, it) } }
        }
    }
}

@Composable private fun SettingSwitch(title: String, detail: String, checked: Boolean, update: (Boolean) -> Unit) = Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, update) } }
