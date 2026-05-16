package com.example.demo.infra.S3.service;

import com.example.demo.infra.S3.exception.S3Handler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String getReportUrl(String stockCode, String date) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            String key = response.contents().stream()
                    .map(S3Object::key)
                    .filter(fileName ->
                            fileName.startsWith(stockCode + "_")
                                    && fileName.contains("_" + date + "_analysis.html")
                    )
                    .findFirst()
                    .orElseThrow(() -> S3Handler.FILE_NOT_FOUND);

            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build();

            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(10))
                            .getObjectRequest(getObjectRequest)
                            .build();

            PresignedGetObjectRequest presignedRequest =
                    s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();

        } catch (S3Exception e) {
            if (e.statusCode() == 403) throw S3Handler.ACCESS_DENIED;
            throw S3Handler.INTERNAL_ERROR;
        } catch (SdkClientException e) {
            throw S3Handler.INTERNAL_ERROR;
        }
    }
}
