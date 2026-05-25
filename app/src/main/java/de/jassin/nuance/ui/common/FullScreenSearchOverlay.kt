package de.jassin.nuance.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FullScreenSearchOverlay(
    progress: Float,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    items: List<T>,
    itemKey: (T) -> String,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String? = { null },
    itemSearchText: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No matching results",
    placeholderText: String = "Search ...",
) {
    val filteredItems =
        remember(query, items) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                items
            } else {
                items.filter { item ->
                    itemSearchText(item).contains(normalizedQuery, ignoreCase = true)
                }
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .zIndex(100f)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = progress.coerceIn(0f, 1f)))
                .safeDrawingPadding(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val searchBarHorizontalPadding = lerp(16.dp, 0.dp, progress.coerceIn(0f, 1f))
            val searchBarTopPadding = lerp(12.dp, 8.dp, progress.coerceIn(0f, 1f))
            val searchBarCorner = lerp(28.dp, 0.dp, progress.coerceIn(0f, 1f))
            val searchBarElevation = lerp(1.dp, 0.dp, progress.coerceIn(0f, 1f))
            val resultsAvailableHeight = (maxHeight - 72.dp - 20.dp).coerceAtLeast(0.dp)
            val resultsHeight = lerp(0.dp, resultsAvailableHeight, progress.coerceIn(0f, 1f))

            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = searchBarHorizontalPadding, vertical = searchBarTopPadding),
                    shape = RoundedCornerShape(searchBarCorner),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = searchBarElevation,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(placeholderText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close search",
                                )
                            }
                        },
                        shape = RoundedCornerShape(searchBarCorner),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(resultsHeight)
                            .padding(horizontal = searchBarHorizontalPadding)
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = progress.coerceIn(0f, 1f))),
                ) {
                    if (resultsHeight > 0.dp) {
                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                            ) {
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(filteredItems, key = itemKey) { item ->
                                    SearchResultRow(
                                        title = itemTitle(item),
                                        subtitle = itemSubtitle(item),
                                        onClick = { onItemSelected(item) },
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

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
