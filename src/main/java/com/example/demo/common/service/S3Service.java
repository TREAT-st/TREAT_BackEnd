package com.example.demo.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    /**
     * S3에 파일 업로드
     * @param file  업로드할 파일
     * @param s3Uri 저장할 S3 URI (s3://bucket/path/to/file.xlsx)
     */
    public String uploadFile(MultipartFile file, String s3Uri) throws IOException {
        URI uri = URI.create(s3Uri);
        String bucket = uri.getHost();
        String key    = uri.getPath().substring(1);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        log.info("S3 업로드 완료. uri={}", s3Uri);
        return s3Uri;
    }

    /**
     * S3에서 파일 다운로드
     * @param s3Uri 다운로드할 S3 URI (s3://bucket/path/to/file.xlsx)
     */
    public InputStream downloadFile(String s3Uri) {
        URI uri = URI.create(s3Uri);
        String bucket = uri.getHost();
        String key    = uri.getPath().substring(1);

        log.info("S3 다운로드 요청. uri={}", s3Uri);

        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }
}
