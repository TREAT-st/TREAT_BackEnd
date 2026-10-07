package com.example.demo.domain.notification.service;

/** 성공은 FCM의 요청 접수를 뜻하며, 기기 수신이나 사용자의 읽음을 보장하지 않습니다. */
public record FcmSendResult(int successCount, int failureCount) {}
