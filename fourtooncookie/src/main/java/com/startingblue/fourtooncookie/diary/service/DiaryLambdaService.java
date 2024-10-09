package com.startingblue.fourtooncookie.diary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startingblue.fourtooncookie.aws.lambda.service.LambdaService;
import com.startingblue.fourtooncookie.character.domain.Character;
import com.startingblue.fourtooncookie.diary.domain.Diary;
import com.startingblue.fourtooncookie.diary.domain.DiaryRepository;
import com.startingblue.fourtooncookie.diary.domain.DiaryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryLambdaService {

    private static final String IMAGE_GENERATION_FUNCTION_NAME = "fourtooncookie-diaryimage-ai-apply-lambda";
    private static final InvocationType INVOCATION_TYPE = InvocationType.EVENT;

    private final LambdaService lambdaService;
    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;
    private final DiaryRepository diaryRepository;

    @Async
    @Transactional
    public void invokeDiaryImageGenerationLambda(Diary diary, Character character) {
        DiaryStatus status = DiaryStatus.IN_PROGRESS;
        try {
            DiaryImageGenerationLambdaPayload diaryImageGenerationLambdaPayload = buildPayload(diary, character);
            String serializePayload = serializePayload(diaryImageGenerationLambdaPayload);
            lambdaService.invokeLambda(lambdaClient, IMAGE_GENERATION_FUNCTION_NAME, INVOCATION_TYPE, serializePayload);
        } catch (Exception e) {
            log.error("Lambda 호출 중 오류 발생: {}", e.getMessage());
            status = DiaryStatus.FAILED;
        } finally {
            handleLambdaResult(diary, status);
        }
    }

    private String serializePayload(DiaryImageGenerationLambdaPayload payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    private DiaryImageGenerationLambdaPayload buildPayload(Diary diary, Character character) {
        return new DiaryImageGenerationLambdaPayload(
                diary.getId(),
                diary.getContent(),
                new DiaryImageGenerationCharacterPayload(
                        character.getId(),
                        character.getName(),
                        character.getCharacterVisionType().name(),
                        character.getBasePrompt()
                )
        );
    }

    private void handleLambdaResult(Diary diary, DiaryStatus status) {
        diary.updateDiaryStatus(status);
        diaryRepository.save(diary);
    }

    private record DiaryImageGenerationCharacterPayload(Long id, String name, String visionType, String basePrompt) { }

    private record DiaryImageGenerationLambdaPayload(Long diaryId, String content, DiaryImageGenerationCharacterPayload character) { }

}
