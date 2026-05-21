package com.example.demo.common.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    /**
     * S3에 파일 업로드
     * @param file      업로드할 파일
     * @param s3Uri     저장할 S3 URI (s3://bucket/path/to/file.xlsx)
     */
    public String uploadFile(MultipartFile file, String s3Uri) throws IOException {
        URI uri = URI.create(s3Uri);
        String bucket = uri.getHost();
        String key    = uri.getPath().substring(1);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), metadata));
        log.info("S3 업로드 완료. uri={}", s3Uri);

        return s3Uri;
    }

    /**
     * S3에서 파일 다운로드
     * @param s3Uri     다운로드할 S3 URI (s3://bucket/path/to/file.xlsx)
     */
    public InputStream downloadFile(String s3Uri) {
        URI uri = URI.create(s3Uri);
        String bucket = uri.getHost();
        String key    = uri.getPath().substring(1);

        log.info("S3 다운로드 요청. uri={}", s3Uri);
        return amazonS3.getObject(bucket, key).getObjectContent();
    }
}
