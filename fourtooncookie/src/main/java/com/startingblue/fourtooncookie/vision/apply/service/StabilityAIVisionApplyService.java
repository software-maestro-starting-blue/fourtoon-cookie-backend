package com.startingblue.fourtooncookie.vision.apply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startingblue.fourtooncookie.character.domain.Character;
import com.startingblue.fourtooncookie.character.domain.CharacterVisionType;
import com.startingblue.fourtooncookie.vision.reply.dto.VisionReplyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
@Service
public class StabilityAIVisionApplyService implements VisionApplyService {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${stability.api.key}")
    private String API_KEY;

    private final String STABILITY_AI_URL = "https://api.stability.ai/v2beta/stable-image/generate/ultra";

    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public void processVisionApply(Long diaryId, List<String> contents, Character character) {

        Integer seed = getSeed(character);

        for (int i = 0; i < 4; i++) {
            String content = contents.get(i);
            String prompt = generatePrompt(content, character);
            String imageB64Json = getImageOfBase64DataFromStabilityAI(prompt, seed);
            byte[] imageOfBytes = Base64.getDecoder().decode(imageB64Json);
            applicationEventPublisher.publishEvent(new VisionReplyEvent(diaryId, imageOfBytes, i));
        }
    }

    private Integer getSeed(Character character) {
        return null; //TODO: 캐릭터에 맞는 시드 추출
    }

    private String generatePrompt(String content, Character character) {
        return null; //TODO: 캐릭터에 맞는 프롬프트 생성
    }

    private String getImageOfBase64DataFromStabilityAI(String prompt, Integer seed) {
        String responseBody = getResponseBodyFromStabilityAI(prompt, seed);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode.get("image").asText();
        } catch (Exception e) {
            throw new RuntimeException("Stability AI 응답 데이터 파싱 중 오류 발생", e);
        }
    }

    private String getResponseBodyFromStabilityAI(String prompt, Integer seed) {
        HttpHeaders headers = getHeader();
        String requestBody = getRequestBody(prompt, seed);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            return restTemplate
                    .exchange(STABILITY_AI_URL, HttpMethod.POST, requestEntity, String.class)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("Stability AI 통신 중 오류 발생", e);
        }
    }

    private HttpHeaders getHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private String getRequestBody(String prompt, Integer seed) {
        return null; //TODO: 프롬프트를 이용하여 requestBody 생성
    }

    @Override
    public CharacterVisionType getModelType() {
        return CharacterVisionType.STABILITY_AI;
    }
}
