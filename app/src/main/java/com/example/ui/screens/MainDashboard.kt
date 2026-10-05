package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.SavedAttractionEntity
import com.example.data.database.TravelQueryEntity
import com.example.data.model.Attraction
import com.example.data.network.GeminiRetrofitClient
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttractionsUiState
import com.example.ui.viewmodel.TravelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainDashboard(
    viewModel: TravelViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Explore, 1 = Saved, 2 = History

    val location by viewModel.locationInput.collectAsState()
    val weatherCondition by viewModel.selectedWeatherCondition.collectAsState()
    val temperature by viewModel.selectedTemperature.collectAsState()
    val selectedInterests by viewModel.selectedInterests.collectAsState()
    val attractionsState by viewModel.attractionsState.collectAsState()

    val historyList by viewModel.searchHistory.collectAsState()
    val savedList by viewModel.savedAttractions.collectAsState()

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("dashboard_navigation"),
                containerColor = DeepSlateSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Explore", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        unselectedIconColor = TextSecondary,
                        indicatorColor = SoftCardBorder
                    ),
                    modifier = Modifier.testTag("nav_explore")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Saved") },
                    label = { Text("Saved (${savedList.size})", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        unselectedIconColor = TextSecondary,
                        indicatorColor = SoftCardBorder
                    ),
                    modifier = Modifier.testTag("nav_saved")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        unselectedIconColor = TextSecondary,
                        indicatorColor = SoftCardBorder
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepSlateBackground, Color(0xFF020617))
                    )
                )
                .padding(innerPadding)
        ) {
            // --- Common App Header (Hero Banner) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_travel_hero),
                    contentDescription = "Travel Hero Header",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark overlay gradient to make text readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Travel Weather Companion",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Smart local suggestions perfectly matched to your day",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- Dynamic Tab Content ---
            when (selectedTab) {
                0 -> ExploreTabContent(
                    viewModel = viewModel,
                    location = location,
                    weatherCondition = weatherCondition,
                    temperature = temperature,
                    selectedInterests = selectedInterests,
                    attractionsState = attractionsState,
                    savedList = savedList
                )
                1 -> SavedTabContent(
                    viewModel = viewModel,
                    savedList = savedList
                )
                2 -> HistoryTabContent(
                    viewModel = viewModel,
                    historyList = historyList,
                    onQueryLoaded = { selectedTab = 0 }
                )
            }
        }
    }
}

// ==========================================
//              EXPLORE TAB
// ==========================================

@Composable
fun ExploreTabContent(
    viewModel: TravelViewModel,
    location: String,
    weatherCondition: String,
    temperature: String,
    selectedInterests: Set<String>,
    attractionsState: AttractionsUiState,
    savedList: List<SavedAttractionEntity>
) {
    val predefinedInterests = listOf("Art", "Historical", "Nature", "Food", "Adventure", "Shopping", "Family")
    val weatherTypes = listOf(
        WeatherType("Sunny", "78°F / 26°C", Icons.Default.WbSunny, TerracottaPrimary),
        WeatherType("Rainy", "52°F / 11°C", Icons.Default.Umbrella, AccentLight),
        WeatherType("Snowy", "28°F / -2°C", Icons.Default.AcUnit, Color.White),
        WeatherType("Cloudy", "62°F / 17°C", Icons.Default.Cloud, TextSecondary),
        WeatherType("Windy", "58°F / 14°C", Icons.Default.Air, Color(0xFFA1A1AA)),
        WeatherType("Stormy", "65°F / 18°C", Icons.Default.Thunderstorm, Color(0xFFC084FC))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 1. Location Input & Action Button
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Where are you exploring?",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { viewModel.updateLocationInput(it) },
                        placeholder = { Text("Enter City Name (e.g. Kyoto, Rome)", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = "Location", tint = TerracottaPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = SoftCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { viewModel.getSuggestions() },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("discover_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = "Search", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Weather Recommendations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 2. Weather & Temperature Simulator Widget
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simulate Local Weather",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Badge(
                            containerColor = SageTealSecondary,
                            contentColor = Color.White,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(temperature, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 12.sp)
                        }
                    }

                    // Horizontal Weather Options Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(weatherTypes) { item ->
                            val isSelected = item.name.equals(weatherCondition, ignoreCase = true)
                            val containerColor = if (isSelected) TerracottaPrimary.copy(alpha = 0.25f) else SoftCardBorder.copy(alpha = 0.4f)
                            val borderColor = if (isSelected) TerracottaPrimary else SoftCardBorder

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateWeather(item.name, item.temp) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("weather_chip_${item.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.name,
                                        tint = if (isSelected) TerracottaPrimary else item.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. User Interests Selector Widget
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SoftCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select Your Travel Interests",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Wrap-style display for interests chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Multi-line chips layout made with simple wrap rows
                        val chunked = predefinedInterests.chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunked.forEach { chunk ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    chunk.forEach { interest ->
                                        val isSelected = selectedInterests.contains(interest)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.toggleInterest(interest) },
                                            label = { Text(interest, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SageTealSecondary,
                                                selectedLabelColor = Color.White,
                                                containerColor = SoftCardBorder.copy(alpha = 0.3f),
                                                labelColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag("interest_chip_${interest.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Suggestions Title / Divider
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "Compass", tint = TerracottaPrimary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Recommended for $location",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = weatherCondition,
                    color = TerracottaPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Dynamic State Views (Attractions List, Empty State, Loading, Error)
        when (val state = attractionsState) {
            is AttractionsUiState.Idle -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = "Search Placeholder",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Find your next adventure!",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Enter a location above and tap Discover to get AI-powered local recommendations based on current simulated weather.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
            is AttractionsUiState.Loading -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = TerracottaPrimary)
                        Text(
                            text = "Consulting Gemini AI companion...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            is AttractionsUiState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.05f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(36.dp))
                        Text(
                            text = "Recommendation Failed",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.message,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.getSuggestions() },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCardBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            is AttractionsUiState.Success -> {
                if (state.attractions.isEmpty()) {
                    item {
                        Text(
                            text = "No attractions matching your interests were found. Try toggling other categories or location.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(state.attractions) { attraction ->
                        val isSaved = savedList.any {
                            it.name.equals(attraction.name, ignoreCase = true) && it.location.equals(location, ignoreCase = true)
                        }

                        AttractionItemCard(
                            attraction = attraction,
                            isSaved = isSaved,
                            onSaveToggle = {
                                viewModel.toggleSaveAttraction(attraction, location, weatherCondition)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
//              SAVED TAB
// ==========================================

@Composable
fun SavedTabContent(
    viewModel: TravelViewModel,
    savedList: List<SavedAttractionEntity>
) {
    if (savedList.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "No Saved Items",
                tint = TextSecondary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Bookmarked Places",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "When you discover attractions on the Explore tab, bookmark them to view here at any time.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Attractions (${savedList.size})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(savedList) { saved ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SoftCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(saved.category),
                                        contentDescription = saved.category,
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = saved.category,
                                        color = TerracottaPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "•  ${saved.location}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = saved.name,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleSaveAttractionEntity(saved) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Unsave",
                                    tint = TerracottaPrimary
                                )
                            }
                        }

                        Text(
                            text = saved.description,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Weather reason badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SageTealSecondary.copy(alpha = 0.12f))
                                .border(1.dp, SageTealSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Weather condition info",
                                    tint = SunsetGoldTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = saved.whyForWeather,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Text(saved.recommendedTime, color = TextSecondary, fontSize = 12.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (saved.isIndoor) Icons.Default.HomeWork else Icons.Default.Landscape,
                                    contentDescription = if (saved.isIndoor) "Indoor" else "Outdoor",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(if (saved.isIndoor) "Indoor Venue" else "Outdoor Activity", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
//              HISTORY TAB
// ==========================================

@Composable
fun HistoryTabContent(
    viewModel: TravelViewModel,
    historyList: List<TravelQueryEntity>,
    onQueryLoaded: () -> Unit
) {
    if (historyList.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "No History",
                tint = TextSecondary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search History",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All successful travel queries will be cached here locally. You can reload them even when offline!",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Travels",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { viewModel.clearAllHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All")
                        }
                    }
                }
            }

            items(historyList) { query ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SoftCardBorder),
                    modifier = Modifier.clickable {
                        // Attempt to reload this search query
                        val parsed = GeminiRetrofitClient.parseAttractionsJson(query.suggestionsJson)
                        if (parsed != null) {
                            viewModel.loadPastQuery(query, parsed)
                            onQueryLoaded()
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Place, contentDescription = "Location", tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = query.location,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteHistoryItem(query.id) }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Weather Indicator
                            Badge(
                                containerColor = TerracottaPrimary.copy(alpha = 0.15f),
                                contentColor = TerracottaPrimary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = getWeatherIcon(query.weatherCondition),
                                        contentDescription = "Weather",
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("${query.weatherCondition} • ${query.temperature}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Timestamp Indicator
                            Text(
                                text = formatTimestamp(query.timestamp),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        if (query.interests.isNotEmpty()) {
                            Text(
                                text = "Interests: ${query.interests}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
//            SUPPORTING COMPOSABLES
// ==========================================

@Composable
fun AttractionItemCard(
    attraction: Attraction,
    isSaved: Boolean,
    onSaveToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SoftCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category, Name, Rating & Bookmark button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(attraction.category),
                            contentDescription = attraction.category,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = attraction.category,
                            color = TerracottaPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        // Rating Indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = SunsetGoldTertiary, modifier = Modifier.size(12.dp))
                            Text(attraction.rating.toString(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = attraction.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Bookmark Icon
                IconButton(
                    onClick = onSaveToggle,
                    modifier = Modifier.testTag("bookmark_button_${attraction.name.replace(" ", "_").lowercase()}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isSaved) "Unsave Place" else "Save Place",
                        tint = TerracottaPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Description
            Text(
                text = attraction.description,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Weather recommendation why-box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SageTealSecondary.copy(alpha = 0.12f))
                    .border(1.dp, SageTealSecondary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Weather reason icon",
                        tint = SunsetGoldTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = attraction.whyForWeather,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text(attraction.recommendedTime, color = TextSecondary, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (attraction.isIndoor) Icons.Default.HomeWork else Icons.Default.Landscape,
                        contentDescription = if (attraction.isIndoor) "Indoor" else "Outdoor",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(if (attraction.isIndoor) "Indoor Venue" else "Outdoor Activity", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

// Helpers

data class WeatherType(
    val name: String,
    val temp: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "historical" -> Icons.Default.Museum
        "nature" -> Icons.Default.Landscape
        "art" -> Icons.Default.Palette
        "food" -> Icons.Default.Restaurant
        "adventure" -> Icons.Default.Explore
        "shopping" -> Icons.Default.ShoppingBag
        "family" -> Icons.Default.ChildCare
        else -> Icons.Default.Place
    }
}

fun getWeatherIcon(weather: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (weather.lowercase(Locale.ROOT)) {
        "sunny" -> Icons.Default.WbSunny
        "rainy" -> Icons.Default.Umbrella
        "snowy" -> Icons.Default.AcUnit
        "cloudy" -> Icons.Default.Cloud
        "windy" -> Icons.Default.Air
        "stormy" -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbCloudy
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
