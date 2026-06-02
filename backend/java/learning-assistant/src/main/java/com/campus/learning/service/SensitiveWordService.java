package com.campus.learning.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class SensitiveWordService {

    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "暴力", "恐怖", "毒品", "枪支", "爆炸", "自杀", "杀人", "谋杀",
            "色情", "淫秽", "赌博", "诈骗", "传销", "邪教", "反动", "颠覆",
            "分裂", "台独", "疆独", "藏独", "港独", "极端", "仇恨", "歧视"
    ));

    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
