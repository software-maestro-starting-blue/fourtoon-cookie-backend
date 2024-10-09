package com.startingblue.fourtooncookie.diary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.startingblue.fourtooncookie.diary.listener.DiarySQSMessageListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
public class DiarySQSMessageListenerTest {

    @Mock
    private DiaryService diaryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DiarySQSMessageListener diarySQSMessageListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        diarySQSMessageListener = new DiarySQSMessageListener(diaryService, objectMapper);
    }

    @Test
    @DisplayName("SQS 메시지가 성공적으로 처리되는 경우")
    void testSQSMessageProcessingSuccess() throws Exception {
        // given
        String message = "{\"diaryId\": 1, \"gridPosition\": 2, \"isSuccess\": true}";
        DiarySQSMessageListener.DiaryImageResponseMessage response =
                new DiarySQSMessageListener.DiaryImageResponseMessage(1L, 2, true);

        // when
        diarySQSMessageListener.handleSQSMessage(message);

        // then
        verify(diaryService).processImageGenerationResponse(response);
        verifyNoMoreInteractions(diaryService);
    }

    @Test
    @DisplayName("SQS 메시지의 JSON 포맷이 잘못된 경우")
    void testInvalidMessageFormatHandling() throws Exception {
        // given
        String invalidMessage = "invalid json format";

        // when
        diarySQSMessageListener.handleSQSMessage(invalidMessage);

        // then
        verify(diaryService, never()).processImageGenerationResponse(any());
    }

    @Test
    @DisplayName("SQS 메시지에 diaryId가 없는 경우 예외 처리")
    void testMissingDiaryIdThrowsException() throws Exception {
        // given
        String message = "{\"diaryId\": null, \"gridPosition\": 2, \"isSuccess\": true}";

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diarySQSMessageListener.handleSQSMessage(message);
        });

        assertEquals(exception.getMessage(), "Diary ID is missing in the message.");
        verify(diaryService, never()).processImageGenerationResponse(any());
    }

    @Test
    @DisplayName("예상치 못한 예외가 발생했을 때 처리")
    void testUnexpectedExceptionHandling() throws Exception {
        // given
        String message = "{\"diaryId\": 1, \"gridPosition\": 2, \"isSuccess\": true}";
        DiarySQSMessageListener.DiaryImageResponseMessage response =
                new DiarySQSMessageListener.DiaryImageResponseMessage(1L, 2, true);

        doThrow(new RuntimeException("Unexpected error")).when(diaryService).processImageGenerationResponse(response);

        // when & then
        assertThrows(RuntimeException.class, () -> {
            diarySQSMessageListener.handleSQSMessage(message);
        });

        verify(diaryService).processImageGenerationResponse(response);
    }
}
