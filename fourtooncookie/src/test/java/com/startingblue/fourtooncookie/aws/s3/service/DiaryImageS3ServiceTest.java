package com.startingblue.fourtooncookie.aws.s3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiaryImageS3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private DiaryImageS3Service diaryImageS3Service;

    private final String bucketName = "test-bucket";
    private final int preSignedUrlDurationInMinutes = 60;
    private static final String IMAGE_FORMAT = ".png";
    private static final String CONTENT_TYPE = "image/png";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(diaryImageS3Service, "bucketName", bucketName);
        ReflectionTestUtils.setField(diaryImageS3Service, "preSignedUrlDurationInMinutes", preSignedUrlDurationInMinutes);
    }

    @Test
    @DisplayName("이미지 업로드 성공")
    void testUploadImage() throws IOException {
        Long diaryId = 1L;
        byte[] image = "test-image".getBytes();
        Integer gridPosition = 1;
        String keyName = diaryId + "/" + gridPosition + IMAGE_FORMAT;

        diaryImageS3Service.uploadImage(diaryId, image, gridPosition);

        ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());

        PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();
        RequestBody capturedBody = requestBodyCaptor.getValue();

        assertEquals(bucketName, capturedRequest.bucket());
        assertEquals(CONTENT_TYPE, capturedRequest.contentType());
        assertEquals(keyName, capturedRequest.key());
        assertArrayEquals(image, capturedBody.contentStreamProvider().newStream().readAllBytes());
    }

    @Test
    @DisplayName("이미지 업로드 중 오류 발생 테스트")
    void testUploadImage_Exception() {
        Long diaryId = 1L;
        byte[] image = "test-image".getBytes();
        Integer gridPosition = 1;
        String keyName = diaryId + "/" + gridPosition + IMAGE_FORMAT;

        doThrow(new RuntimeException("S3 error")).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            diaryImageS3Service.uploadImage(diaryId, image, gridPosition);
        });

        assertEquals("S3에 이미지 업로드 중 오류가 발생했습니다. Key: " + keyName, exception.getMessage());
    }

    @Test
    @DisplayName("이미지가 존재할 때 프리사인 URL 생성")
    void testGeneratePreSignedImageUrl() throws MalformedURLException {
        Long diaryId = 1L;
        Integer gridPosition = 1;

        // HeadObjectResponse 모킹
        HeadObjectResponse headObjectResponse = mock(HeadObjectResponse.class);

        // PresignedGetObjectRequest 모킹
        PresignedGetObjectRequest presignedGetObjectRequest = mock(PresignedGetObjectRequest.class);
        when(presignedGetObjectRequest.url()).thenReturn(URI.create("http://example.com").toURL());

        // S3Client 및 S3Presigner 응답 모킹
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        // 테스트 대상 메서드 호출
        String url = diaryImageS3Service.generatePreSignedImageUrl(diaryId, gridPosition);

        // 결과 검증
        assertEquals("http://example.com", url);

        // 상호작용 검증
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("이미지가 존재하지 않을 때 예외 발생")
    void testGeneratePreSignedImageUrl_ImageNotExist() {
        Long diaryId = 1L;
        Integer gridPosition = 1;

        // headObject 요청에 대한 NoSuchKeyException 모킹
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        // 예외가 발생하는지 검증
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            diaryImageS3Service.generatePreSignedImageUrl(diaryId, gridPosition);
        });

        assertEquals("S3에 이미지가 존재하지 않습니다. Key: 1/1.png", exception.getMessage());

        // 상호작용 검증
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("프리사인 URL 생성 중 오류 발생 시 예외 발생")
    void testGeneratePreSignedImageUrl_ExceptionDuringPresign() {
        Long diaryId = 1L;
        Integer gridPosition = 1;

        // HeadObjectResponse 모킹
        HeadObjectResponse headObjectResponse = mock(HeadObjectResponse.class);

        // headObject에 대한 S3Client 응답 모킹
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

        // presignGetObject 중 예외 모킹
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("Presign error"));

        // 예외가 발생하는지 검증
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            diaryImageS3Service.generatePreSignedImageUrl(diaryId, gridPosition);
        });

        assertEquals("S3에서 프리사인 URL 생성 중 오류가 발생했습니다. Key: 1/1.png", exception.getMessage());

        // 상호작용 검증
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }
}
