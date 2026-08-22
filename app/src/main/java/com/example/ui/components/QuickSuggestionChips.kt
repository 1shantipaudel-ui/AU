package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LanguageMode
import com.example.ui.theme.CleanCardBorder
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.TextPrimary

@Composable
fun QuickSuggestionChips(
    selectedLanguage: LanguageMode,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = when (selectedLanguage) {
        LanguageMode.NEPALI -> listOf(
            "👋 Hi Alex",
            "🔦 फ्ल्यासलाइट बाल",
            "⏰ बिहान ६ बजेको अलार्म लगाउ",
            "🎵 युट्युबमा नेपाली गीत बजाउ",
            "📝 नोट: भोलि बजार जानु छ",
            "🌐 वाइफाइ सेटिङ खोल",
            "📞 फोन डायलर खोल",
            "💬 ह्वाट्सएप मेसेज पठाउ"
        )
        LanguageMode.HINDI -> listOf(
            "👋 Hi Alex",
            "🔦 फ्लैशलाइट चालू करो",
            "⏰ सुबह ७ बजे का अलार्म लगाओ",
            "🎵 यूट्यूब पर अरिजीत के गाने बजाओ",
            "📝 नोट: दूध और सब्जियां लानी हैं",
            "🌐 वाई-फाई सेटिंग्स खोलो",
            "📞 कॉल डायलर खोलो",
            "💬 व्हाट्सएप मैसेज भेजो"
        )
        else -> listOf(
            "👋 Hi Alex",
            "🔦 Turn on flashlight",
            "⏰ Set alarm for 7 AM",
            "🎵 Play lo-fi beats on YouTube",
            "📝 Note: Buy groceries tomorrow",
            "🌐 Open Wi-Fi settings",
            "📞 Open phone dialer",
            "💬 Send WhatsApp message"
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("quick_suggestions_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.forEachIndexed { index, prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CleanSurface)
                    .border(1.dp, CleanCardBorder, RoundedCornerShape(20.dp))
                    .clickable { onSuggestionClick(prompt.replace(Regex("^[^\\sa-zA-Z0-9\\u0900-\\u097F]+"), "").trim()) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("suggestion_chip_$index")
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }
    }
}

