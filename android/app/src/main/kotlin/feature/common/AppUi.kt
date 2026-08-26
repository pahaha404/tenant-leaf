package com.seipseip.app.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.Border
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PageBackground
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary

enum class AppTab(val label: String) {
    Home("홈"),
    Property("매물"),
    Report("리포트"),
    Profile("내 정보"),
}

@Composable
fun Modifier.swipeToChangeTab(
    currentTab: AppTab?,
    onTabSelected: ((AppTab) -> Unit)?,
): Modifier {
    if (currentTab == null || onTabSelected == null) return this
    val density = LocalDensity.current
    return this.pointerInput(currentTab) {
        var totalDragX = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDragX = 0f },
            onDragEnd = {
                val threshold = with(density) { 100.dp.toPx() }
                if (totalDragX < -threshold) {
                    val nextTab = when (currentTab) {
                        AppTab.Home -> AppTab.Property
                        AppTab.Property -> AppTab.Report
                        AppTab.Report -> AppTab.Profile
                        AppTab.Profile -> null
                    }
                    nextTab?.let { onTabSelected(it) }
                } else if (totalDragX > threshold) {
                    val prevTab = when (currentTab) {
                        AppTab.Profile -> AppTab.Report
                        AppTab.Report -> AppTab.Property
                        AppTab.Property -> AppTab.Home
                        AppTab.Home -> null
                    }
                    prevTab?.let { onTabSelected(it) }
                }
            },
            onHorizontalDrag = { _, dragAmount ->
                totalDragX += dragAmount
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppPageScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    scrollable: Boolean = true,
    selectedTab: AppTab? = null,
    onTabSelected: ((AppTab) -> Unit)? = null,
    showBottomBar: Boolean = true,
    bottomAction: (@Composable () -> Unit)? = null,
    topTrailingAction: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = PageBackground,
        floatingActionButton = { floatingActionButton?.invoke() },
        bottomBar = {
            Column {
                bottomAction?.invoke()
                if (showBottomBar && selectedTab != null && onTabSelected != null) {
                    AppBottomNavigation(selectedTab, onTabSelected)
                }
            }
        },
    ) { innerPadding ->
        val pageContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PageBackground)
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .imePadding()
                    .padding(
                        start = 20.dp,
                        top = if (onBack == null) 36.dp else 12.dp,
                        end = 20.dp,
                        bottom = 12.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(if (onBack == null) 16.dp else 10.dp),
            ) {
                if (onBack != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .shadow(1.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clickable(onClick = onBack)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    contentDescription = "뒤로",
                                    tint = DeepGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = title,
                                modifier = Modifier.padding(start = 12.dp),
                                color = DeepGreen,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        topTrailingAction?.invoke()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    color = Secondary,
                                    fontSize = 14.3.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                            Text(
                                text = title,
                                color = DeepGreen,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        topTrailingAction?.invoke()
                    }
                }
                content()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (onRefresh != null) {
                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = Color.White,
                            color = Green,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    },
                ) {
                    pageContent()
                }
            } else {
                pageContent()
            }
        }
    }
}

@Composable
fun AppBottomNavigation(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.shadow(3.dp), containerColor = Color.White) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = {
                    if (tab != selectedTab) onTabSelected(tab)
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            AppTab.Home -> Icons.Outlined.Home
                            AppTab.Property -> Icons.Outlined.RealEstateAgent
                            AppTab.Report -> Icons.Outlined.Article
                            AppTab.Profile -> Icons.Outlined.Person
                        },
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Orange,
                    selectedTextColor = Orange,
                    unselectedIconColor = Secondary,
                    unselectedTextColor = Secondary,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            contentColor = Color.White,
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Green,
        ),
        border = BorderStroke(1.dp, Border),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    accent: Color = PaleGreen,
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        colors = CardDefaults.cardColors(containerColor = accent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, color = DeepGreen, fontWeight = FontWeight.Bold)
            Text(description, color = Secondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionTitle(title: String, description: String? = null) {
    Text(title, color = DeepGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    if (description != null) {
        Text(description, color = Secondary, fontSize = 12.sp)
    }
}

@Composable
fun StateBadge(label: String, color: Color = Green) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.RealEstateAgent,
            contentDescription = null,
            tint = Green,
            modifier = Modifier.size(42.dp),
        )
        Text(title, color = DeepGreen, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        Text(description, color = Secondary, fontSize = 12.sp, textAlign = TextAlign.Center)
        PrimaryButton(actionLabel, onAction)
    }
}
