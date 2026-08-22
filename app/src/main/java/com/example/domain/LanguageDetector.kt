package com.example.domain

import com.example.data.model.LanguageMode
import java.util.Locale

object LanguageDetector {

    // Nepali specific character/word patterns
    private val NEPALI_INDICATORS = listOf(
        "हुन्छ", "गरौं", "गर्छु", "गर्नुस्", "गर्नुहोस्", "छैन", "होला", "पात्रो", "बत्ती", "खोल्नुस", "खोल्नुहोस्",
        "अहिले", "कति", "बल्ल", "नेपाल", "नमस्ते", "धन्यवाद", "सन्चै", "के छ", "कस्तो", "हेरौं", "हेरौ",
        "बालिदिनु", "बालिदिन्छु", "निभाउनु", "निभाइदिनु", "बजाउनु", "बजाउनुस्", "फोन गर", "खबर", "टिप्पणी",
        "हुन्छ नि", "मिल्छ", "लागि", "कहाँ", "कसरी"
    )

    // Hindi specific character/word patterns
    private val HINDI_INDICATORS = listOf(
        "करो", "कीजिए", "खोलो", "खोलिए", "चालू", "बंद", "नमस्ते", "नमस्कार", "फ्लैशलाइट", "बजाओ",
        "चलाओ", "कॉल", "भेजो", "अलार्म", "कैसा", "कैसी", "बताओ", "चाहिए", "कृपया", "शुक्रिया",
        "अलविदा", "सुनाओ", "लगाओ", "बढ़ाओ", "घटाओ", "जलाओ", "बुझाओ", "सकता", "सकती", "हूँ", "हैं",
        "कैमरा", "गाना"
    )

    fun detectLanguage(text: String, preferredMode: LanguageMode = LanguageMode.AUTO): LanguageMode {
        if (preferredMode != LanguageMode.AUTO) {
            return preferredMode
        }

        val clean = text.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return LanguageMode.ENGLISH

        // Check for Devanagari script (Unicode range \u0900-\u097F)
        val hasDevanagari = clean.any { it in '\u0900'..'\u097F' }

        if (hasDevanagari) {
            // Distinguish Nepali vs Hindi
            var nepaliScore = 0
            var hindiScore = 0

            for (nep in NEPALI_INDICATORS) {
                if (clean.contains(nep)) nepaliScore += 2
            }
            for (hin in HINDI_INDICATORS) {
                if (clean.contains(hin)) hindiScore += 2
            }

            // Nepali specific characters / nuances
            if (clean.contains("छ") || clean.contains("नुस्") || clean.contains("होस्") || clean.contains("थिए") || clean.contains("बाल्")) {
                nepaliScore += 3
            }
            if (clean.contains("हूँ") || clean.contains("रहा") || clean.contains("रही") || clean.contains("रहे") || clean.contains("गया") || clean.contains("गई") || clean.contains("जलाओ")) {
                hindiScore += 3
            }

            return if (nepaliScore > hindiScore) LanguageMode.NEPALI else LanguageMode.HINDI
        }

        // Romanized Hindi / Hinglish and Romanized Nepali indicators
        val nepaliRomanWords = listOf("hunchha", "huncha", "k chha", "k cha", "garidinu", "balko", "kasto", "sanchai", "dhanyabad", "khola na", "bajaideu", "nepali")
        val hindiRomanWords = listOf("karo", "khol do", "chalu", "chalao", "batao", "kaise ho", "kya chal", "shukriya", "gaana", "sunao", "bataiye", "hinglish", "kardo")

        var nepRomanScore = 0
        var hinRomanScore = 0

        for (w in nepaliRomanWords) {
            if (clean.contains(w)) nepRomanScore++
        }
        for (w in hindiRomanWords) {
            if (clean.contains(w)) hinRomanScore++
        }

        if (nepRomanScore > 0 && nepRomanScore >= hinRomanScore) return LanguageMode.NEPALI
        if (hinRomanScore > 0) return LanguageMode.HINDI

        return LanguageMode.ENGLISH
    }
}
