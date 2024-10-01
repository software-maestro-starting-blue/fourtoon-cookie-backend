package com.startingblue.fourtooncookie.aws.cloudfront;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudFrontService {

    private static final Logger log = LoggerFactory.getLogger(CloudFrontService.class);
    @Value("${aws.cloudfront.domain.name}")
    private String cloudFrontDomainName;

    @Value("${aws.cloudfront.keyPairId}")
    private String keyPairId;

    @Value("${aws.cloudfront.privateKeyPath}")
    private String privateKeyPath;

    @Value("${aws.diaryimage.bucket.name}")
    private String bucketName;


    private static final int SIGNED_EXPIRATION = 3600; // 1 시간
    private static final String CLOUD_FRONT_POLICY = "CloudFront-Policy";
    private static final String CLOUD_FRONT_SIGNATURE = "CloudFront-Signature";
    private static final String CLOUD_FRONT_KEY_PAIR_ID = "CloudFront-Key-Pair-Id";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String KEY_FACTORY_ALGORITHM = "RSA";

    public Map<String, String> generateCloudFrontSignedCookies(String path) {
        try {
            // URL을 만들고자 하는 리소스의 경로
            String resourcePath = String.format("https://%s/%s", cloudFrontDomainName, path);

            // 만료 시간 계산
            Instant expirationTime = Instant.now().plus(SIGNED_EXPIRATION, ChronoUnit.SECONDS);

            // 정책 생성 (리소스와 만료 시간에 대한 정책)
            String policy = createCannedPolicy(resourcePath, expirationTime);

            // RSA 개인 키를 사용하여 정책 서명
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
            String signature = signPolicy(policy, privateKey);

            // Base64 인코딩
            String encodedPolicy = Base64.getUrlEncoder().withoutPadding().encodeToString(policy.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.getBytes(StandardCharsets.UTF_8));

            // 서명된 쿠키 생성
            Map<String, String> cookies = new HashMap<>();
            cookies.put(CLOUD_FRONT_POLICY, encodedPolicy);
            cookies.put(CLOUD_FRONT_SIGNATURE, encodedSignature);
            cookies.put(CLOUD_FRONT_KEY_PAIR_ID, keyPairId);

            return cookies;

        } catch (Exception e) {
            throw new RuntimeException("CloudFront 서명된 쿠키 생성 중 오류 발생", e);
        }
    }

    // 간단한 정책을 생성 (Canned Policy)
    private String createCannedPolicy(String resourceUrl, Instant expirationTime) {
        return String.format("{\"Statement\": [{\"Resource\":\"%s\",\"Condition\": {\"DateLessThan\":{\"AWS:EpochTime\": %d}}}]}",
                resourceUrl, expirationTime.getEpochSecond());
    }

    // RSA 개인 키로 정책에 서명
    private String signPolicy(String policy, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(policy.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return new String(signatureBytes);
    }

    // PEM 형식의 개인 키를 로드
    private PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        // 개인 키 파일 로드 및 파싱
        String privateKeyPEM = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replaceAll("\\s", "");
        byte[] encodedKey = Base64.getDecoder().decode(privateKeyPEM);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    public URL generateSignedUrl(String path) {
        try {
            String resourcePath = String.format("https://%s/%s/%s", cloudFrontDomainName, bucketName, path);
            log.info("resourcePath: {}", resourcePath);
            Instant expirationTime = Instant.now().plus(SIGNED_EXPIRATION, ChronoUnit.SECONDS);
            log.info("expirationTime: {}", expirationTime);
            String policy = createCannedPolicy(resourcePath, expirationTime);
            log.info("policy: {}", policy);
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
            log.info("privateKey: {}", privateKey);
            String signature = signPolicy(policy, privateKey);
            log.info("signature: {}", signature);
            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.getBytes(StandardCharsets.UTF_8));
            log.info("encodedSignature: {}", encodedSignature);
            String signedUrl = String.format("%s?Expires=%d&Signature=%s&Key-Pair-Id=%s",
                    resourcePath, expirationTime.getEpochSecond(), encodedSignature, keyPairId);
            log.info("signedUrl: {}", signedUrl);
            return new URL(signedUrl);
        } catch (Exception e) {
            throw new RuntimeException("Signed URL 생성 중 오류 발생", e);
        }
    }

}
