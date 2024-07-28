package com.startingblue.fourtooncookie.vision.apply.service;

import com.startingblue.fourtooncookie.character.domain.Character;
import com.startingblue.fourtooncookie.character.domain.CharacterVisionType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
@Service
public class StabilityAIVisionApplyService implements VisionApplyService {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public void processVisionApply(Long diaryId, List<String> contents, Character character) {

        //TODO: 캐릭터에 맞는 시드를 추출

        contents.forEach(content -> {
            //TODO: content를 프롬프트로 수정
            //TODO: 프롬프트를 활용하여 requestBody 구성
            //TODO: requestBody를 이용하여 AI 서버에 요청
            //TODO: 응답을 받아서 event Call
        });
    }

    @Override
    public CharacterVisionType getModelType() {
        return CharacterVisionType.STABILITY_AI;
    }
}
