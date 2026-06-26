package pt.ipt.dama.TeachersLog.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipt.dama.TeachersLog.R
import pt.ipt.dama.TeachersLog.ui.theme.AppTheme

data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_1_title, R.string.onboarding_1_desc, R.mipmap.ic_launcher_foreground),
        OnboardingPage(R.string.onboarding_2_title, R.string.onboarding_2_desc, R.mipmap.ic_launcher_foreground),
        OnboardingPage(R.string.onboarding_3_title, R.string.onboarding_3_desc, R.mipmap.ic_launcher_foreground)
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) AppTheme.colors.primary else AppTheme.colors.outlineVariant
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }

                // Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(onClick = onFinished) {
                            Text(stringResource(R.string.skip), color = AppTheme.colors.outline)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }) {
                            Text(stringResource(R.string.next))
                        }
                    } else {
                        Button(onClick = onFinished) {
                            Text(stringResource(R.string.get_started))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { index ->
            OnboardingPageContent(pages[index])
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = page.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .scale(1.6f)
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = stringResource(page.titleRes),
            style = AppTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(page.descriptionRes),
            style = AppTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = AppTheme.colors.onSurfaceVariant
        )
    }
}
