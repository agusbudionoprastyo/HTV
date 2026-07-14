import sys

def main():
    file_path = 'app/src/main/java/com/dafamsemarang/dhtv/HotelInfoScreen.kt'
    
    with open(file_path, 'r') as f:
        lines = f.readlines()
        
    start_line = -1
    end_line = -1
    
    for i, line in enumerate(lines):
        if line.startswith("        Column(") and start_line == -1 and i > 250:
            start_line = i
        if "GLIDING FOCUS BORDER OVERLAY" in line:
            end_line = i
            break
            
    if start_line == -1 or end_line == -1:
        print(f"Error: start={start_line}, end={end_line}")
        return

    # We need to preserve from end_line onwards.
    # The new layout replaces everything in between.
    
    new_content = """        Column(
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
                    .height(250.dp)
                    .padding(horizontal = 58.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val item = debouncedFocusedItem
                if (item != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = item.name,
                            style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.5).sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.description,
                            style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.85f)),
                            maxLines = 4, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. BOTTOM AREA: VERTICAL LAZYCOLUMN OF LAZYROWS
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(categoriesList) { categoryIndex, categoryPair ->
                    val categoryName = categoryPair.first
                    val itemsList = categoryPair.second
                    
                    if (itemsList.isNotEmpty()) {
                        item {
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
                                                selectedItemForModal = item
                                                showModal = true
                                            },
                                            modifier = Modifier.focusProperties {
                                                // Default focus logic is usually fine for LazyRow inside LazyColumn
                                            }
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    focusedItemIndex = itemIndex
                                                    selectedButton = categoryIndex
                                                    isBorderVisible = true
                                                }
                                            },
                                            onBoundsChanged = { rect ->
                                                if (rect != null) {
                                                    targetFocusRect = rect
                                                    targetFocusRadius = 24.dp
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
"""
    
    # Actually `end_line` points to `        // 3. GLIDING FOCUS BORDER OVERLAY`.
    # But right before that there is a closing brace for the old Column.
    # We should keep `end_line` and replace up to `end_line - 1` (the closing brace of the Column).
    # Since our `new_content` doesn't have the closing brace for the new `Column`, we should keep that closing brace!
    # Wait, my new_content doesn't close the `Column(`.
    # Let me ensure I just replace `start_line` to `end_line - 1` inclusive.
    
    with open(file_path, 'w') as f:
        f.writelines(lines[:start_line])
        f.write(new_content)
        f.writelines(lines[end_line-1:])

if __name__ == '__main__':
    main()
