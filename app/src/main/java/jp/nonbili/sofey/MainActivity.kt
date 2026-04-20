package jp.nonbili.sofey

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SofeyApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SofeyApp() {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic && !dark -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colors) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Sofey",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { inner -> SofeyScreen(inner) }
        }
    }
}

@Composable
fun SofeyScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, LockAdminReceiver::class.java) }
    
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    var isAccessibilityActive by remember { mutableStateOf(SofeyAccessibilityService.instance != null) }

    // Observe lifecycle to refresh status when returning from settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminActive = dpm.isAdminActive(adminComponent)
                isAccessibilityActive = SofeyAccessibilityService.instance != null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { isAdminActive = dpm.isAdminActive(adminComponent) }

    val cs = MaterialTheme.colorScheme
    val bgBrush = Brush.verticalGradient(
        listOf(cs.surface, cs.surfaceVariant.copy(alpha = 0.6f), cs.surface)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VolumePad(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.VolumeUp,
                    label = "Up",
                    container = cs.primaryContainer,
                    content = cs.onPrimaryContainer
                ) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
                VolumePad(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.VolumeDown,
                    label = "Down",
                    container = cs.secondaryContainer,
                    content = cs.onSecondaryContainer
                ) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                }
            }

            Spacer(Modifier.height(8.dp))

            PowerButton(
                enabled = isAdminActive || isAccessibilityActive,
                isAccessibility = isAccessibilityActive,
                container = cs.tertiaryContainer,
                content = cs.onTertiaryContainer
            ) {
                when {
                    isAccessibilityActive -> {
                        SofeyAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                    }
                    isAdminActive -> {
                        dpm.lockNow()
                    }
                    else -> {
                        // Default to requesting Accessibility as it's what the user likely wants
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        isAccessibilityActive -> "Using Accessibility Service\n(Follows system lock delay)"
                        isAdminActive -> "Using Device Admin\n(Locks immediately)"
                        else -> "Enable a service to lock your screen"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                if (!isAccessibilityActive) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text("Switch to Accessibility (Recommended)")
                    }
                } else {
                    Text(
                        "Tip: Disable 'Power button instantly locks' in System Security settings to stay unlocked longer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                if (!isAdminActive && !isAccessibilityActive) {
                    TextButton(onClick = {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Sofey needs this permission to lock the screen.")
                        }
                        adminLauncher.launch(intent)
                    }) {
                        Text("Use Device Admin instead")
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumePad(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "vpad-scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(180.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = content, modifier = Modifier.size(48.dp))
            Text(text = label, color = content, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PowerButton(
    enabled: Boolean,
    isAccessibility: Boolean,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "power-scale"
    )

    val ring = Brush.radialGradient(listOf(container, container.copy(alpha = 0.35f)))

    Box(
        modifier = Modifier
            .scale(scale)
            .size(220.dp)
            .clip(CircleShape)
            .background(ring)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isAccessibility) Icons.Filled.PowerSettingsNew else Icons.Filled.Lock,
                    contentDescription = if (isAccessibility) "Power" else "Lock",
                    tint = content,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = if (enabled) (if (isAccessibility) "Power" else "Lock") else "Enable",
                    color = content,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
