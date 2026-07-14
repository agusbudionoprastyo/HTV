import re

with open("app/src/main/java/com/dafamsemarang/dhtv/HotelInfoScreen.kt", "r") as f:
    content = f.read()

# Find the start of the root Column
start_pattern = r"        Column\(\n            modifier = Modifier\n                \.fillMaxSize\(\)\n                \.offset\(x = contentSlideOffset\.value\.dp\)\n                \.padding\(top = 95\.dp, bottom = 55\.dp\),\n            verticalArrangement = Arrangement\.SpaceBetween\n        \) \{"

start_match = re.search(start_pattern, content)
if not start_match:
    print("Could not find start pattern")
    exit(1)

start_idx = start_match.start()

# Find the end of the root Column by matching braces.
# We know it ends right before the AnimatedFocusBorder:
# "        // 3. GLIDING FOCUS BORDER OVERLAY"
end_pattern = r"        // 3\. GLIDING FOCUS BORDER OVERLAY"
end_match = re.search(end_pattern, content)
if not end_match:
    print("Could not find end pattern")
    exit(1)

end_idx = end_match.start()

new_column_content = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = contentSlideOffset.value.dp)
                .padding(top = 95.dp, bottom = 55.dp),
            verticalArrangement = Arrangement.Top
        ) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            
            // 1. TOP AREA: HERO SECTION (Detail of currently focused item)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 58.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val item = debouncedFocusedItem
                if (item != null) {
                    // Check if it has map data
                    val hasMapData = item.longlat.isNotEmpty() || item.staticMapUrl.isNotEmpty()
                    if (hasMapData) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.75f)) {
                                Text(
                                    text = item.name,
                                    style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp),
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.description,
                                    style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.75f)),
                                    maxLines = 7, overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Map and QR code placeholder (simplifying for now, we can keep the complex logic if needed)
                            Row(modifier = Modifier.weight(1.25f).fillMaxHeight(), horizontalArrangement = Arrangement.End) {
                                // We keep it simple to ensure fast rendering in the new layout
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.name,
                                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.description,
                                style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.75f)),
                                maxLines = 5, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. BOTTOM AREA: VERTICAL LAZYCOLUMN OF LAZYROWS
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(categoriesList) { categoryIndex, categoryPair ->
                    val categoryName = categoryPair.first
                    val itemsList = categoryPair.second
                    
                    if (itemsList.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Text(
                                text = categoryName,
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White),
                                modifier = Modifier.padding(horizontal = 58.dp, bottom = 12.dp)
                            )
                            
                            val rowState = rememberLazyListState()
                            LazyRow(
                                state = rowState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 58.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(itemsList) { itemIndex, item ->
                                    ItemCard(
                                        item = item,
                                        onClick = {
                                            // Handle click
                                        },
                                        modifier = Modifier.focusProperties {
                                            // Set up/down logic here if needed
                                        },
                                        onBoundsChanged = { rect ->
                                            if (rect != null) {
                                                targetFocusRect = rect
                                                targetFocusRadius = 24.dp
                                                isBorderVisible = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
"""

new_content = content[:start_idx] + new_column_content + content[end_idx:]

with open("app/src/main/java/com/dafamsemarang/dhtv/HotelInfoScreen.kt", "w") as f:
    f.write(new_content)

print("Replaced content successfully.")
