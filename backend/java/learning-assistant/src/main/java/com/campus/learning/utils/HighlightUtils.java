package com.campus.learning.utils;

import java.util.regex.Pattern;

public class HighlightUtils {

    public static String highlight(String text, String keyword) {
        if (text == null || keyword == null || keyword.trim().isEmpty()) {
            return text;
        }

        String result = text;
        String[] words = keyword.trim().split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            String escaped = Pattern.quote(word);
            result = result.replaceAll("(?i)(" + escaped + ")", "<mark>$1</mark>");
        }
        return result;
    }
}
