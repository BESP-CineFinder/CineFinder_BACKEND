package com.cinefinder.global.util.service;

import lombok.extern.slf4j.Slf4j;
import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer.KoreanToken;
import org.springframework.stereotype.Service;
import scala.collection.Seq;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BadWordFilterService {

    private final Set<String> badWords;

    public BadWordFilterService() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("bad-words.txt")), StandardCharsets.UTF_8))) {
            this.badWords = reader.lines()
                    .map(line -> new String(java.util.Base64.getDecoder().decode(line), StandardCharsets.UTF_8))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("비속어 사전을 불러오는 데 실패했습니다", e);
        }
    }

    public String addBadWord(String badWord) {
        String encodedWord = java.util.Base64.getEncoder().encodeToString(badWord.getBytes(StandardCharsets.UTF_8));
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Objects.requireNonNull(getClass().getClassLoader().getResource("bad-words.txt")).getFile(), true), StandardCharsets.UTF_8))) {
            writer.write(encodedWord);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("비속어를 추가하는 데 실패했습니다", e);
        }
        badWords.add(badWord);
        return badWord;
    }

    public String normalize(String text) {
        CharSequence cleaned = OpenKoreanTextProcessorJava.normalize(text);
        return cleaned.toString()
                .replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣ]", "")
                .replaceAll("\\s+", "");
    }

    public boolean containsBadWord(String input) {
        String normalizedInput = normalize(input);

        Seq<KoreanToken> tokens = OpenKoreanTextProcessorJava.tokenize(normalizedInput);
        List<String> morphemes = scala.collection.JavaConverters
                .seqAsJavaList(tokens)
                .stream()
                .map(KoreanToken::text)
                .toList();

        String combined = String.join("", morphemes);

        return badWords.stream().anyMatch(combined::contains);
    }

    public String maskBadWords(String input, String mask) {
        if (!containsBadWord(input)) {
            return input;
        }

        for (String bad : badWords) {
            if (input.contains(bad)) {
                input = input.replaceAll(bad, mask.repeat(bad.length()));
            }
        }
        return input;
    }
}
