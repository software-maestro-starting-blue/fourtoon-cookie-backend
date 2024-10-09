package com.startingblue.fourtooncookie.aws.cloudfront;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;

import java.io.IOException;
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

import static software.amazon.awssdk.services.cloudfront.model.Paths.builder;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudFrontService {

    private static final int SIGNED_EXPIRATION = 3600; // 1 시간
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String KEY_FACTORY_ALGORITHM = "RSA";

    public URL generateSignedUrl(String path, String cloudFrontDomainName, String keyPairId, String privateKeyPath) {
        try {
            String resourcePath = buildResourcePath(cloudFrontDomainName, path);
            Instant expirationTime = calculateExpirationTime();
            String policy = createCannedPolicy(resourcePath, expirationTime);
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
            String encodedSignature = signAndEncodePolicy(policy, privateKey);
            String signedUrl = buildSignedUrl(resourcePath, expirationTime, encodedSignature, keyPairId);
            return new URL(signedUrl);
        } catch (Exception e) {
            throw new RuntimeException("Signed URL 생성 중 오류 발생", e);
        }
    }

    public void invalidateCache(String distributionId, String filePath) {
        try (CloudFrontClient cloudFrontClient = CloudFrontClient.builder().build()) {
            builder().build();
            InvalidationBatch invalidationBatch = InvalidationBatch.builder()
                    .paths(builder().items(filePath).quantity(1).build())
                    .callerReference(String.valueOf(System.currentTimeMillis())) // 고유 참조값
                    .build();

            CreateInvalidationRequest invalidationRequest = CreateInvalidationRequest.builder()
                    .distributionId(distributionId)
                    .invalidationBatch(invalidationBatch)
                    .build();

            cloudFrontClient.createInvalidation(invalidationRequest);
            log.info("CloudFront 캐시 무효화 완료: {}", filePath);
        } catch (Exception e) {
            log.error("CloudFront 캐시 무효화 중 오류 발생: {}", e.getMessage());
        }
    }

    private String buildResourcePath(String cloudFrontDomainName, String path) {
        return String.format("https://%s/%s", cloudFrontDomainName, path);
    }

    private Instant calculateExpirationTime() {
        return Instant.now().plus(SIGNED_EXPIRATION, ChronoUnit.SECONDS);
    }

    private String signAndEncodePolicy(String policy, PrivateKey privateKey) throws Exception {
        byte[] signature = signPolicy(policy, privateKey);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private String buildSignedUrl(String resourcePath, Instant expirationTime, String encodedSignature, String keyPairId) {
        return String.format("%s?Expires=%d&Signature=%s&Key-Pair-Id=%s",
                resourcePath, expirationTime.getEpochSecond(), encodedSignature, keyPairId);
    }

    private String createCannedPolicy(String resourceUrl, Instant expirationTime) {
        String policy = String.format("{\"Statement\": [{\"Resource\":\"%s\",\"Condition\": {\"DateLessThan\":{\"AWS:EpochTime\": %d}}}]}",
                resourceUrl, expirationTime.getEpochSecond());
        String policyWithoutSpaces = policy.replaceAll("\\s+", "");
        return Base64.getEncoder().encodeToString(policyWithoutSpaces.getBytes(StandardCharsets.UTF_8));
    }

    private PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        String privateKeyPEM = readPrivateKeyFile(privateKeyPath);
        privateKeyPEM = removeKeyHeadersAndWhitespace(privateKeyPEM);
        byte[] encodedKey = decodePrivateKey(privateKeyPEM);
        return generatePrivateKey(encodedKey);
    }

    private String readPrivateKeyFile(String privateKeyPath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(privateKeyPath)));
    }

    private String removeKeyHeadersAndWhitespace(String privateKeyPEM) {
        return privateKeyPEM
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replaceAll("\\s", "");
    }

    private byte[] decodePrivateKey(String privateKeyPEM) {
        return Base64.getDecoder().decode(privateKeyPEM);
    }

    private PrivateKey generatePrivateKey(byte[] encodedKey) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    private byte[] signPolicy(String policy, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(policy.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }
}
