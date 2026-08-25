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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seipseip.app.Border
import com.seipseip.app.DeepGreen
import com.seipseip.app.Green
import com.seipseip.app.Orange
import com.seipseip.app.PaleGreen
import com.seipseip.app.Secondary

enum class AppTab(val label: String) {
    Home("홈"),
    Property("매물"),
    Report("리포트"),
    Profile("내 정보"),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppPageScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollable: Boolean = true,
    selectedTab: AppTab? = null,
    onTabSelected: ((AppTab) -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    topTrailingAction: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        floatingActionButton = { floatingActionButton?.invoke() },
        bottomBar = {
            Column {
                bottomAction?.invoke()
                if (selectedTab != null && onTabSelected != null) {
                    AppBottomNavigation(selectedTab, onTabSelected)
                }
            }
        },
    ) { innerPadding ->
        val pageContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFCFBF8))
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onBack != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "뒤로",
                                modifier = Modifier.clickable(onClick = onBack),
                                tint = Green,
                            )
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            color = DeepGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
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
    NavigationBar(modifier = modifier, containerColor = Color.White) {
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
