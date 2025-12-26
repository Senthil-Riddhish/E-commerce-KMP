package org.example.project

import Data.Product
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.ImageRequest
import coil3.request.crossfade
import kmpapp.composeapp.generated.resources.Res
import kmpapp.composeapp.generated.resources.banner1
import kmpapp.composeapp.generated.resources.banner2
import kmpapp.composeapp.generated.resources.banner3
import kmpapp.composeapp.generated.resources.snowfall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import chaintech.videoplayer.model.PlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerView

enum class SortOption(val displayName: String) {
    NameAsc("Name (A-Z)"),
    NameDesc("Name (Z-A)"),
    PriceAsc("Price (Low-High)"),
    PriceDesc("Price (High-Low)"),
    RatingDesc("Rating (High-Low)")
}

enum class FilterType {
    Sort, Category, Rating
}

// UI State Definition
sealed interface UiState {
    data object Loading : UiState
    data object Success : UiState
    data class Error(val message: String) : UiState
}

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    
    private val _cartItems = MutableStateFlow<Set<Int>>(emptySet())
    val cartItems = _cartItems.asStateFlow()
    
    // Derived state for full Cart Product objects
    val cartProducts = combine(_products, _cartItems) { products, cartIds ->
        products.filter { it.id in cartIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived state for Total Cost
    val totalCost = cartProducts.map { list ->
        list.sumOf { it.price }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    private val repository = HomeRepository()

    // Filter & Sort States
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _minRating = MutableStateFlow<Double?>(null)
    val minRating = _minRating.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NameAsc)
    val sortOption = _sortOption.asStateFlow()

    // Derived State: Unique Categories
    val categories = _products.map { list -> 
        list.map { it.category }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived State: Filtered & Sorted Products
    val filteredProducts = combine(_products, _selectedCategory, _minRating, _sortOption) { products, category, rating, sort ->
        val filtered = products.filter { product ->
            val categoryMatch = category == null || product.category == category
            val ratingMatch = rating == null || product.rating.rate >= rating
            categoryMatch && ratingMatch
        }
        
        when (sort) {
            SortOption.NameAsc -> filtered.sortedBy { it.title }
            SortOption.NameDesc -> filtered.sortedByDescending { it.title }
            SortOption.PriceAsc -> filtered.sortedBy { it.price }
            SortOption.PriceDesc -> filtered.sortedByDescending { it.price }
            SortOption.RatingDesc -> filtered.sortedByDescending { it.rating.rate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repository.getProducts().collect { productList ->
                    _products.update { productList }
                    _uiState.value = UiState.Success
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Failed to load products. Check your internet connection.")
            }
        }
    }

    fun toggleCart(product: Product) {
        _cartItems.update { currentCart ->
            if (currentCart.contains(product.id)) {
                currentCart - product.id
            } else {
                currentCart + product.id
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.update { if (it == category) null else category }
    }

    fun selectMinRating(rating: Double) {
        _minRating.update { if (it == rating) null else rating }
    }
    
    fun selectSortOption(option: SortOption) {
        _sortOption.update { option }
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _minRating.value = null
        _sortOption.value = SortOption.NameAsc
    }
}

sealed interface Screen {
    data object Home : Screen
    data class Detail(val product: Product) : Screen
    data object Cart : Screen
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = viewModel { HomeViewModel() }
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        when (val screen = currentScreen) {
            Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onProductClick = { product ->
                        currentScreen = Screen.Detail(product)
                    },
                    onCartClick = { currentScreen = Screen.Cart }
                )
            }
            is Screen.Detail -> {
                ProductDetailScreen(
                    product = screen.product,
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }
            Screen.Cart -> {
                CartScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (Product) -> Unit,
    onCartClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    
    val cartItems by viewModel.cartItems.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val bannerImages = listOf(
        Res.drawable.banner1,
        Res.drawable.banner2,
        Res.drawable.banner3
    )

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterBottomSheetContent(
                categories = categories,
                selectedCategory = selectedCategory,
                minRating = minRating,
                sortOption = sortOption,
                onCategorySelect = viewModel::selectCategory,
                onRatingSelect = viewModel::selectMinRating,
                onSortSelect = viewModel::selectSortOption,
                onClearFilters = viewModel::clearFilters,
                onClose = { showFilterSheet = false }
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Products",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onCartClick) {
                        BadgedBox(
                            badge = {
                                if (cartItems.isNotEmpty()) {
                                    Badge {
                                        Text(text = cartItems.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading products...")
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.loadProducts() }) {
                        Text("Retry")
                    }
                }
            }
            is UiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Section with Animated Background
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Color.Black)) {
                            // Video Player for background
                            VideoPlayerView(
                                modifier = Modifier.fillMaxSize(),
                                url = "https://videocdn.cdnpk.net/videos/9777558a-fb27-5352-bfda-496b64cf0f83/horizontal/previews/clear/large.mp4?token=exp=1766753576~hmac=2a90a49807fa90f672d673c050f0d4e2c7ec4c67d1b5b8e452d4a3b87c2acd98",
                                playerConfig = PlayerConfig(
                                    isPauseResumeEnabled = false,
                                    isSeekBarVisible = false,
                                    isDurationVisible = false,
                                    isScreenResizeEnabled = false
                                )
                            )
    
                            // Content on top of background
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                            ) {
                                 HomeCarousel(images = bannerImages)
                                 
                                 Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Button(
                                        onClick = { showFilterSheet = true },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.FilterList, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Sort & Filter")
                                    }
                                    
                                    ActiveFiltersRow(
                                        selectedCategory = selectedCategory,
                                        minRating = minRating,
                                        sortOption = sortOption,
                                        onClearCategory = { if (selectedCategory != null) viewModel.selectCategory(selectedCategory!!) },
                                        onClearRating = { if (minRating != null) viewModel.selectMinRating(minRating!!) },
                                        onResetSort = { viewModel.selectSortOption(SortOption.NameAsc) }
                                    )
                                }
                            }
                        }
                    }
    
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
    
                    // Product List
                    if (filteredProducts.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No products found", color = Color.Gray)
                            }
                        }
                    } else {
                        items(filteredProducts) { product ->
                            ProductCard(
                                product = product,
                                isInCart = cartItems.contains(product.id),
                                onClick = { onProductClick(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val cartProducts by viewModel.cartProducts.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (cartProducts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total:", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "$${totalCost.toPrice()}",
                            style = MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* Proceed to Checkout */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Proceed to Checkout", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        if (cartProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Your cart is empty", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartProducts) { product ->
                    CartItemRow(
                        product = product,
                        onRemove = { viewModel.toggleCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(product: Product, onRemove: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ... (Rest of the file remains same: FilterBottomSheetContent, ActiveFiltersRow, HomeCarousel, ProductCard, ProductDetailScreen)

fun Double.toPrice(): String {
    return ((this * 100).roundToInt() / 100.0).toString()
}
@Composable
fun FilterBottomSheetContent(
    categories: List<String>,
    selectedCategory: String?,
    minRating: Double?,
    sortOption: SortOption,
    onCategorySelect: (String) -> Unit,
    onRatingSelect: (Double) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onClose: () -> Unit
) {
    var selectedFilterType by remember { mutableStateOf(FilterType.Sort) }

    Column(
        modifier = Modifier.fillMaxWidth().height(500.dp).padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort & Filter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClearFilters) {
                Text("Clear All")
            }
        }
        
        Divider(Modifier.padding(vertical = 8.dp))
        
        Row(Modifier.fillMaxSize()) {
            // Left Column: Filter Types
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Gray.copy(alpha = 0.1f))
            ) {
                items(FilterType.values()) { type ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFilterType = type }
                            .background(if (selectedFilterType == type) Color.White else Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = type.name,
                            fontWeight = if (selectedFilterType == type) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFilterType == type) MaterialTheme.colorScheme.primary else Color.Black
                        )
                    }
                }
            }
            
            // Right Column: Options
            Box(Modifier.weight(2f).fillMaxHeight().padding(start = 16.dp)) {
                when (selectedFilterType) {
                    FilterType.Sort -> {
                        LazyColumn {
                            items(SortOption.values()) { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = (sortOption == option),
                                            onClick = { onSortSelect(option) }
                                        )
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (sortOption == option),
                                        onClick = null // handled by row
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(option.displayName)
                                }
                            }
                        }
                    }
                    FilterType.Category -> {
                        LazyColumn {
                            items(categories) { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCategorySelect(category) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isSelected = selectedCategory == category
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Close, // Using Close as a placeholder for empty/radio, better to use RadioButton or Checkbox
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(category.replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                    FilterType.Rating -> {
                        Column {
                            Text("Minimum Rating", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            listOf(4.0, 3.0, 2.0).forEach { rating ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onRatingSelect(rating) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (minRating == rating),
                                        onClick = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("$rating+")
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Apply")
        }
    }
}

@Composable
fun ActiveFiltersRow(
    selectedCategory: String?,
    minRating: Double?,
    sortOption: SortOption,
    onClearCategory: () -> Unit,
    onClearRating: () -> Unit,
    onResetSort: () -> Unit
) {
    if (selectedCategory == null && minRating == null && sortOption == SortOption.NameAsc) return

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sortOption != SortOption.NameAsc) {
            item {
                InputChip(
                    selected = true,
                    onClick = onResetSort,
                    label = { Text(sortOption.displayName) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                    colors = InputChipDefaults.inputChipColors(
                         selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                         selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }
        }
        
        if (selectedCategory != null) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearCategory,
                    label = { Text(selectedCategory.replaceFirstChar { it.uppercase() }) },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                )
            }
        }
        
        if (minRating != null) {
            item {
                InputChip(
                    selected = true,
                    onClick = onClearRating,
                    label = { Text("$minRating+ ★") },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun HomeCarousel(images: List<DrawableResource>) {
    val pagerState = rememberPagerState(pageCount = { images.size })

    // Auto-scroll logic
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000) // 3 seconds delay
            val nextPage = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 8.dp,
            modifier = Modifier.height(200.dp).fillMaxWidth()
        ) { page ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .graphicsLayer {
                        // Calculate the absolute offset for the current page from the
                        // scroll position. We use the absolute value which allows us to mirror
                        // any effects for both directions
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState
                                .currentPageOffsetFraction
                        ).absoluteValue

                        // We animate the alpha, between 50% and 100%
                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    }
            ) {
                Image(
                    painter = painterResource(images[page]),
                    contentDescription = "Banner",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Indicators
        Row(
            Modifier
                .height(30.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(images.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}


@Composable
fun ProductCard(
    product: Product,
    isInCart: Boolean,
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product.image,
                    contentDescription = product.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Small Cart Icon in Top Left if Added
                if (isInCart) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "In Cart",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "$${product.price}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = product.category,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Rating Stars in Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                repeat(5) { index ->
                    val isFilled = index < product.rating.rate.roundToInt()
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isFilled) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${product.rating.count})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val cartItems by viewModel.cartItems.collectAsState()
    val isInCart = cartItems.contains(product.id)

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Button(
                onClick = { viewModel.toggleCart(product) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInCart) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary // Green if added
                )
            ) {
                if (isInCart) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Added to Cart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "Add to Cart  $${product.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 80.dp) // Space for FAB
            ) {
                // Unique Header: Large Image with gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(Color.White)
                ) {
                    AsyncImage(
                        model = product.image,
                        contentDescription = product.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp, bottom = 24.dp) 
                    )
                    
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(top = 48.dp, start = 16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                }

                // Content Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        
                        // Rating Stars Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                val isFilled = index < product.rating.rate.roundToInt()
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isFilled) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${product.rating.rate} (${product.rating.count})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}