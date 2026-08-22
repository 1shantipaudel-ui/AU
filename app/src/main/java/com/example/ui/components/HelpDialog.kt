package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CleanAmber
import com.example.ui.theme.CleanCardBorder
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class CommandExample(
    val category: String,
    val command: String,
    val description: String
)

@Composable
fun HelpDialog(
    onDismiss: () -> Unit,
    onSampleCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("🇳🇵 नेपाली", "🇮🇳 हिन्दी", "🇬🇧 English")

    val nepaliCommands = listOf(
        CommandExample("वेक वर्ड", "वेक वर्ड साथी राख", "आफ्नो अनुकूल वेक वर्ड सेट गर्न"),
        CommandExample("वेक वर्ड", "साथी, फ्ल्यासलाइट बाल", "वेक वर्डबाट सिधै आदेश दिन"),
        CommandExample("उपकरण सेटिङ", "फ्ल्यासलाइट बाल / बन्द गर", "टर्च अन/अफ गर्न"),
        CommandExample("उत्पादकता", "बिहान ६:३० को अलार्म लगाउ", "अलार्म सेट गर्न"),
        CommandExample("मनोरञ्जन", "युट्युबमा नेपाली गीत बजाउ", "युट्युबमा सर्च तथा प्ले"),
        CommandExample("सञ्चार", "ह्वाट्सएप मेसेज पठाउ", "ह्वाट्सएप च्याट खोल्न"),
        CommandExample("सञ्चार", "फोन डायलर खोल", "फोन कल गर्न"),
        CommandExample("नोट", "नोट लेख: भोलि काम छ", "भ्वाइस नोट सुरक्षित गर्न"),
        CommandExample("सेटिङ", "वाइफाइ सेटिङ खोल", "वाइफाइ कन्फिगर गर्न"),
        CommandExample("सेटिङ", "आवाज बढाउ / घटाउ", "भोल्युम मिलाउन")
    )

    val hindiCommands = listOf(
        CommandExample("वेक वर्ड", "वेक वर्ड जार्विस रखो", "कस्टम वेक वर्ड सेट करें"),
        CommandExample("वेक वर्ड", "जार्विस, फ्लैशलाइट चालू करो", "वेक वर्ड से कमांड चलाएं"),
        CommandExample("सिस्टम सेटिंग्स", "फ्लैशलाइट चालू करो / बंद करो", "टॉर्च टॉगल करें"),
        CommandExample("अलार्म एवं टाइमर", "सुबह ७ बजे का अलार्म लगाओ", "अलार्म सेट करें"),
        CommandExample("संगीत एवं वीडियो", "यूट्यूब पर अरिजीत के गाने चलाओ", "यूट्यूब प्लेबैक"),
        CommandExample("कम्युनिकेशन", "व्हाट्सएप मैसेज भेजो", "व्हाट्सएप ओपन करें"),
        CommandExample("कम्युनिकेशन", "कॉल डायलर खोलो", "फोन कॉल डायल करें"),
        CommandExample("नोट्स", "नोट बनाओ: ज़रूरी मीटिंग है", "क्विक नोट सेव करें"),
        CommandExample("सिस्टम सेटिंग्स", "वाई-फाई सेटिंग्स खोलो", "वाई-फाई स्क्रीन"),
        CommandExample("वॉल्यूम", "आवाज़ बढ़ाओ / कम करो", "वॉल्यूम एडजस्ट करें")
    )

    val englishCommands = listOf(
        CommandExample("Wake Word", "Set wake word to Jarvis", "Personalize custom wake trigger"),
        CommandExample("Wake Word", "Jarvis, turn on flashlight", "Activate and command hands-free"),
        CommandExample("System Control", "Turn on / off flashlight", "Toggle device torch"),
        CommandExample("Productivity", "Set alarm for 7:00 AM", "Create native alarm"),
        CommandExample("Media", "Play lo-fi songs on YouTube", "Launch YouTube music"),
        CommandExample("Messaging", "Send WhatsApp message", "Open WhatsApp ready"),
        CommandExample("Calling", "Open dialer / Call phone", "Launch native phone app"),
        CommandExample("Quick Notes", "Save note: Meeting tomorrow", "Store voice note"),
        CommandExample("Settings", "Open Wi-Fi / Bluetooth", "Open system settings"),
        CommandExample("Audio", "Increase volume / Mute", "Adjust speaker volume")
    )

    val currentCommands = when (selectedTab) {
        0 -> nepaliCommands
        1 -> hindiCommands
        else -> englishCommands
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CleanSurface)
                .border(1.dp, CleanCardBorder, RoundedCornerShape(22.dp))
                .padding(18.dp)
                .testTag("help_guide_dialog")
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = CleanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice Commands Guide",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CleanSurfaceVariant,
                    contentColor = CleanPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CleanPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) CleanPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(currentCommands) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CleanSurfaceVariant)
                                .border(1.dp, CleanCardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    onSampleCommandClick(item.command)
                                    onDismiss()
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.command,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CleanPrimary
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 10.sp,
                                        color = CleanAmber
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

