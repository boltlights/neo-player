import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                    if (isImporting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Importing songs...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                                Text(
                                    text = "${(importProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = importProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.2f)
                            )
                        }
                    }''',
'''                    if (isImporting) {
                        Surface(
                            color = surfaceColor,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Importing songs...",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "${(importProgress * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = importProgress,
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }''')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
