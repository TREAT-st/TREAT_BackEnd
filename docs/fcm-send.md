# 사용자 기기 알림 발송

`FcmService.sendToUser`는 사용자의 등록 기기를 조회하고 각 기기에 제목, 본문, 화면 이동 데이터를 보냅니다.
`firebase.enabled=true`일 때 빈이 생성되며 Firebase 인증 정보가 필요합니다.

```java
FcmSendResult result = fcmService.sendToUser(
        userId,
        "예측 결과가 나왔어요",
        "참여한 예측의 결과를 확인해 보세요.",
        Map.of("notificationId", "123", "notificationType", "RESULT", "sourceId", "456")
);
```

- `data` 값은 문자열이며 생략하려면 빈 Map 또는 null을 전달합니다.
- 결과는 `successCount`, `failureCount`입니다. 등록 기기가 없으면 둘 다 0입니다.
- 성공은 Firebase가 발송 요청을 접수했다는 뜻이며 기기의 실제 수신이나 읽음 여부는 아닙니다.
- 기기별로 동기 발송합니다. 한 기기에서 오류가 발생해도 나머지 기기는 계속 처리합니다.
- 조회/정리는 별도의 짧은 DB 트랜잭션으로 처리하고 외부 통신 중에는 트랜잭션을 유지하지 않습니다.
- 기기 목록 조회 자체가 실패하거나 공통 입력값이 잘못되면 호출자에게 예외가 전달됩니다.

## 실패 처리

Firebase의 `UNREGISTERED` 응답에만 기기 연결을 삭제합니다.
`INVALID_ARGUMENT`는 메시지 형식 문제일 수도 있으므로 삭제하지 않습니다.
일시적 장애나 인증 오류도 토큰을 유지하며 자동 재시도는 아직 구현하지 않았습니다.
토큰 삭제가 실패해도 다른 기기의 발송을 계속합니다.
로그에는 사용자 ID, 기기 ID, 오류 코드/종류만 기록하고 토큰이나 메시지 내용은 기록하지 않습니다.

삭제 조건은 기기 ID, 사용자 ID, 발송 시점 토큰, 갱신 시각의 일치입니다.
발송 도중 새 토큰 등록이나 계정 변경이 있었다면 현재 연결을 보존합니다.
이미 Firebase로 전송한 요청은 이후 로그아웃으로 취소할 수 없습니다.

## 다음 연결 단계

채점/리포트 결과와 알림 내역을 DB에 커밋한 뒤 이 서비스를 호출해야 합니다.
`NOT_SUPPORTED`는 트랜잭션을 잠시 중단할 뿐 커밋 이후로 발송을 예약하는 기능이 아닙니다.
커밋 후 이벤트나 발송 대기 내역을 처리하는 작업에서 호출하도록 연결합니다.
현재 구현에는 업무 이벤트 연결, 알림 DB 저장, 자동 재시도가 포함되지 않습니다.
실제 기기 수신 검증은 React Native 앱의 실제 토큰을 등록한 뒤 진행합니다.

## 개발용 테스트 발송 API

서버 실행 환경에 아래 값을 추가해야 테스트 API가 생성됩니다.

```sh
FCM_TEST_API_ENABLED=true
```

`POST /api/v1/test/fcm/send`는 JWT로 로그인한 사용자에게 등록된 모든 기기로만 발송합니다.
사용자 ID는 요청에서 받지 않습니다. 운영 환경에서는 이 값을 설정하지 않아 엔드포인트를 비활성화합니다.

```json
{
  "title": "FCM 테스트",
  "body": "TREAT 테스트 알림입니다.",
  "data": {
    "notificationType": "RESULT",
    "sourceId": "123"
  }
}
```

응답의 `successCount`는 Firebase가 접수한 기기 수이고, `failureCount`는 발송 요청에 실패한 기기 수입니다.
둘 다 0이면 현재 로그인 사용자에게 등록된 기기가 없습니다.

참고: https://firebase.google.com/docs/cloud-messaging/send/admin-sdk
토큰 정리 기준: https://firebase.google.com/docs/cloud-messaging/manage-tokens
