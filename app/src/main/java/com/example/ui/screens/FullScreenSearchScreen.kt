package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItemData
import com.example.model.Playlist
import com.example.ui.components.ArtworkCover
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.ui.components.getGradientForIndex
import com.example.viewmodel.PlayerViewModel

/**
 * Dedicated Full-Page Search Interface.
 * Searches across:
 * - Song titles
 * - Artists and singers
 * - Albums
 * - Genres
 * - Playlists
 * - Folders and local music files
 * - Audio metadata
 * - Videos (strictly user-controlled via Show Video Files setting or filter)
 */
@Composable
fun FullScreenSearchScreen(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val masterList by viewModel.masterMediaList.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val showVideoFiles by viewModel.showVideoFiles.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Songs", "Artists", "Albums", "Playlists", "Folders", "Videos"

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Request focus and display keyboard on appearance
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler {
        onDismiss()
    }

    // Filter available filters based on showVideoFiles setting
    val availableFilters = remember(showVideoFiles) {
        if (showVideoFiles) {
            listOf("All", "Songs", "Artists", "Albums", "Playlists", "Folders", "Videos")
        } else {
            listOf("All", "Songs", "Artists", "Albums", "Playlists", "Folders")
        }
    }

    // Dynamic search evaluation across library
    val searchResults by remember(searchQuery, masterList, playlists, showVideoFiles) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isBlank()) {
                SearchResults()
            } else {
                // 1. Songs filter
                val matchingSongs = masterList.filter { item ->
                    // Exclude call recordings strictly
                    if (item.isCallRecording()) return@filter false
                    // If video, only include if showVideoFiles is enabled
                    if (item.isVideo && !showVideoFiles) return@filter false

                    item.title.contains(q, ignoreCase = true) ||
                            item.artist.contains(q, ignoreCase = true) ||
                            item.album.contains(q, ignoreCase = true) ||
                            item.genre.contains(q, ignoreCase = true) ||
                            item.codec.contains(q, ignoreCase = true) ||
                            item.container.contains(q, ignoreCase = true)
                }

                // 2. Artists grouping
                val matchingArtists = masterList.filter { !it.isVideo || showVideoFiles }
                    .filter { it.artist.contains(q, ignoreCase = true) }
                    .groupBy { it.artist }

                // 3. Albums grouping
                val matchingAlbums = masterList.filter { !it.isVideo || showVideoFiles }
                    .filter { it.album.contains(q, ignoreCase = true) }
                    .groupBy { it.album }

                // 4. Playlists matching
                val matchingPlaylists = playlists.filter { pl ->
                    pl.name.contains(q, ignoreCase = true) ||
                            pl.description.contains(q, ignoreCase = true)
                }

                // 5. Folders / local files grouping
                val matchingFolders = masterList.filter { !it.isVideo || showVideoFiles }
                    .filter { item ->
                        item.uri.contains(q, ignoreCase = true) || item.title.contains(q, ignoreCase = true)
                    }
                    .groupBy { item ->
                        val path = item.uri
                        if (path.startsWith("file://") || path.contains("/")) {
                            path.substringBeforeLast('/', "Music Storage").substringAfterLast('/')
                        } else {
                            "Local Audio"
                        }
                    }

                // 6. Videos (strictly if showVideoFiles enabled or user chooses videos filter)
                val matchingVideos = masterList.filter { it.isVideo && (it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)) }

                SearchResults(
                    songs = matchingSongs.filter { !it.isVideo },
                    artists = matchingArtists,
                    albums = matchingAlbums,
                    playlists = matchingPlaylists,
                    folders = matchingFolders,
                    videos = matchingVideos
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D17))
            .statusBarsPadding()
    ) {
        // Top Search Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search songs, artists, albums, genres...",
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = accentColor
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotBlank()) {
                        viewModel.addSearchHistory(searchQuery)
                    }
                    keyboardController?.hide()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x1AFFFFFF),
                    unfocusedContainerColor = Color(0x12FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("full_page_search_input")
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("search_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontal Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableFilters) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0x1EFFFFFF),
                        labelColor = Color(0xFF9CA3AF)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = null
                )
            }
        }

        // Content Area: Search Query Active vs Recent Searches / History
        if (searchQuery.isBlank()) {
            // Recent Searches and Discovery
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text("Clear", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                            }
                        }
                    }

                    items(searchHistory) { query ->
                        AuraGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = query
                                    viewModel.addSearchHistory(query)
                                },
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = query,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeSearchHistory(query) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Browse suggestions
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Browse Categories",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                val categories = listOf(
                    "Lossless FLAC" to "Pure 24-bit 96kHz studio masters",
                    "Hi-Res Masters" to "Audiophile grade AAC & Opus",
                    "Acoustic & Lo-Fi" to "Uncompressed spatial soundscapes",
                    "Orchestral Masterworks" to "Classical stereo 2.0 recordings"
                )

                items(categories) { (catTitle, catSub) ->
                    AuraGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = catTitle
                                viewModel.addSearchHistory(catTitle)
                            },
                        cornerRadius = 14.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(catTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(catSub, color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Results matching the query
            val totalResults = searchResults.totalCount(selectedFilter, showVideoFiles)

            if (totalResults == 0) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results for \"$searchQuery\"",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Check the spelling or try searching by title, artist, album, genre, or audio codec.",
                            fontSize = 13.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Surface(
                            onClick = { searchQuery = "" },
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Clear Search",
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: SONGS
                    if ((selectedFilter == "All" || selectedFilter == "Songs") && searchResults.songs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Songs",
                                count = searchResults.songs.size,
                                icon = Icons.Default.MusicNote,
                                accentColor = accentColor
                            )
                        }

                        items(searchResults.songs) { song ->
                            SearchSongRow(
                                song = song,
                                accentColor = accentColor,
                                onPlay = {
                                    viewModel.addSearchHistory(searchQuery)
                                    viewModel.playMedia(song, searchResults.songs)
                                },
                                onAddToQueue = { viewModel.playerController.addToQueue(song) },
                                onAddToPlaylist = { viewModel.setSelectedPlaylistForAdd(song) },
                                onShowCodecInfo = {
                                    viewModel.playerController.playMedia(song, searchResults.songs)
                                    viewModel.setCodecInfoOpen(true)
                                }
                            )
                        }
                    }

                    // SECTION 2: ARTISTS
                    if ((selectedFilter == "All" || selectedFilter == "Artists") && searchResults.artists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Artists",
                                count = searchResults.artists.size,
                                icon = Icons.Default.Person,
                                accentColor = accentColor
                            )
                        }

                        items(searchResults.artists.entries.toList()) { (artist, tracks) ->
                            AuraGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSearchHistory(searchQuery)
                                        tracks.firstOrNull()?.let { viewModel.playMedia(it, tracks) }
                                    },
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tracks.size} tracks in library",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.addSearchHistory(searchQuery)
                                            tracks.firstOrNull()?.let { viewModel.playMedia(it, tracks) }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play all by artist",
                                            tint = accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: ALBUMS
                    if ((selectedFilter == "All" || selectedFilter == "Albums") && searchResults.albums.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Albums",
                                count = searchResults.albums.size,
                                icon = Icons.Default.Album,
                                accentColor = accentColor
                            )
                        }

                        items(searchResults.albums.entries.toList()) { (album, tracks) ->
                            val representative = tracks.firstOrNull()
                            AuraGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSearchHistory(searchQuery)
                                        representative?.let { viewModel.playMedia(it, tracks) }
                                    },
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (representative != null) {
                                        ArtworkCover(
                                            media = representative,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${representative?.artist ?: "Various"} • ${tracks.size} tracks",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.addSearchHistory(searchQuery)
                                            representative?.let { viewModel.playMedia(it, tracks) }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play Album",
                                            tint = accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 4: PLAYLISTS
                    if ((selectedFilter == "All" || selectedFilter == "Playlists") && searchResults.playlists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Playlists",
                                count = searchResults.playlists.size,
                                icon = Icons.Default.QueueMusic,
                                accentColor = accentColor
                            )
                        }

                        items(searchResults.playlists) { pl ->
                            AuraGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSearchHistory(searchQuery)
                                        // Play first playlist track
                                        val plTracks = masterList.filter { pl.mediaIds.contains(it.id) }
                                        plTracks.firstOrNull()?.let { viewModel.playMedia(it, plTracks) }
                                    },
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(getGradientForIndex(pl.coverGradientIndex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.mediaIds.size} songs • ${pl.description}",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 5: FOLDERS / LOCAL PATHS
                    if ((selectedFilter == "All" || selectedFilter == "Folders") && searchResults.folders.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Folders & Storage",
                                count = searchResults.folders.size,
                                icon = Icons.Default.Folder,
                                accentColor = accentColor
                            )
                        }

                        items(searchResults.folders.entries.toList()) { (folder, tracks) ->
                            AuraGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSearchHistory(searchQuery)
                                        tracks.firstOrNull()?.let { viewModel.playMedia(it, tracks) }
                                    },
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tracks.size} tracks in folder",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 6: VIDEOS (Strictly shown only if Show Video Files is enabled OR user selected Videos filter)
                    if ((selectedFilter == "Videos" || (showVideoFiles && selectedFilter == "All")) && searchResults.videos.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Videos (Requires Explicit Tap)",
                                count = searchResults.videos.size,
                                icon = Icons.Default.Movie,
                                accentColor = Color(0xFFEF4444)
                            )
                        }

                        items(searchResults.videos) { video ->
                            AuraGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSearchHistory(searchQuery)
                                        viewModel.playMedia(video, searchResults.videos)
                                    },
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ArtworkCover(
                                        media = video,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${video.artist} • ${video.resolution ?: "HD"} • ${video.formattedDuration}",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        AuraBadge(
                                            text = "VIDEO",
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.addSearchHistory(searchQuery)
                                            viewModel.playMedia(video, searchResults.videos)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play Video",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            fontSize = 13.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}

@Composable
private fun SearchSongRow(
    song: MediaItemData,
    accentColor: Color,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowCodecInfo: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    AuraGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkCover(
                media = song,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.formattedDuration}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraBadge(
                        text = song.codec,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = song.bitrate,
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF9CA3AF)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1E2235))
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor) },
                        onClick = {
                            showMenu = false
                            onPlay()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Codec & Audio Info", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.White) },
                        onClick = {
                            showMenu = false
                            onShowCodecInfo()
                        }
                    )
                }
            }
        }
    }
}

private data class SearchResults(
    val songs: List<MediaItemData> = emptyList(),
    val artists: Map<String, List<MediaItemData>> = emptyMap(),
    val albums: Map<String, List<MediaItemData>> = emptyMap(),
    val playlists: List<Playlist> = emptyList(),
    val folders: Map<String, List<MediaItemData>> = emptyMap(),
    val videos: List<MediaItemData> = emptyList()
) {
    fun totalCount(filter: String, showVideos: Boolean): Int {
        return when (filter) {
            "Songs" -> songs.size
            "Artists" -> artists.size
            "Albums" -> albums.size
            "Playlists" -> playlists.size
            "Folders" -> folders.size
            "Videos" -> videos.size
            else -> songs.size + artists.size + albums.size + playlists.size + folders.size + if (showVideos) videos.size else 0
        }
    }
}
