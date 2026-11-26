package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.localization.ArabicStrings
import com.example.localization.EnglishStrings
import com.example.ui.art.AsyncDrawableImage
import com.example.ui.art.FurnaceVectorIllustration
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    authScope: CoroutineScope? = null
) {
    val context = LocalContext.current
    val localScope = rememberCoroutineScope()
    val effectiveScope = authScope ?: localScope
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isArabic by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSilent by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val str = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    LaunchedEffect(Unit) {
        withFrameNanos { }
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSilent = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSilent = false
            },
            scope = effectiveScope
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SlateAbyss)
        ) {
            AsyncDrawableImage(
                resId = R.drawable.img_egypt_frost_banner,
                contentDescription = str.appTitle,
                contentScale = ContentScale.Crop,
                maxDimPx = 720,
                placeholderColor = SlateAbyss,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.42f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SlateAbyss.copy(alpha = 0.72f),
                                SlateAbyss.copy(alpha = 0.55f),
                                SlateAbyss.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            // Language Switch at top corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCard.copy(alpha = 0.9f))
                    .border(1.dp, WarmGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .clickable { isArabic = !isArabic }
                    .testTag("auth_lang_toggle")
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "EN" else "عربي",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 460.dp)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Glowing Solar Obelisk Emblem
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(SlateCard.copy(alpha = 0.85f))
                        .border(2.5.dp, WarmGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FurnaceVectorIllustration(
                        level = 6,
                        overdrive = true,
                        modifier = Modifier.size(106.dp)
                    )
                }

                Text(
                    text = str.appTitle,
                    style = MaterialTheme.typography.displayLarge,
                    color = WarmGold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isArabic)
                        "قُد مملكة النيل المتجمدة، وأشعل مسلّة الشمس لحماية رعاياك من عواصف الخماسين الجليدية"
                    else
                        "Lead the frozen Nile realm, ignite the Solar Obelisk, and shield your dynasty from the Frost Khamaseen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FrostWhite,
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.92f)),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, IceCyan.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldSafe)
                            Text(
                                text = if (isArabic) "مزامنة سحابية تلقائية لتقدم مدينتك وأبطالك" else "Automatic Cloud Sync for your citadel & heroes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostWhite
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold)
                            Text(
                                text = if (isArabic) "لوحة صدارة حية لزعماء أرض الصقيع الفرعونية" else "Live Pharaoh Chief Power Leaderboard",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostWhite
                            )
                        }
                    }
                }

                errorMsg?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.18f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert)
                    ) {
                        Text(
                            text = msg,
                            color = CrimsonAlert,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMsg = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMsg = msg
                            },
                            scope = effectiveScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading && !isCheckingSilent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmGold,
                        contentColor = SlateAbyss
                    )
                ) {
                    if (isLoading || isCheckingSilent) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SlateAbyss,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "جاري الاتصال بالمعبد..." else "Connecting...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = str.signInWithGoogle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
