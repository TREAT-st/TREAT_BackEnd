# React Native 기기 등록 API

두 API 모두 기존 로그인 JWT를 `Authorization: Bearer <accessToken>`으로 전달합니다.
사용자 ID는 요청으로 받지 않고 인증된 사용자에서 가져옵니다.

## 토큰 등록·갱신

`PATCH /api/v1/users/me/devices/{installationId}`

```json
{
  "fcmToken": "FCM에서 발급받은 토큰",
  "platform": "ANDROID"
}
```

- `installationId`: 앱 설치 시 생성하고 앱 저장소에 보관하는 임의 UUID 등을 사용합니다.
  로그인 사용자가 바뀌어도 유지하고 다른 설치와 공유하지 않습니다.
  영문, 숫자, `_`, `-`를 허용하며 길이는 1~128자입니다.
- `fcmToken`: 공백 없는 1~512자 문자열입니다.
- `platform`: `ANDROID` 또는 `IOS`입니다.
- 로그인 완료 및 토큰 갱신 시 호출합니다. 같은 설치 ID는 기존 행을 갱신합니다.
- 다른 계정으로 로그인해 호출하면 해당 설치의 소유자를 새 사용자로 변경합니다.
- 동일 토큰을 다른 설치 ID로 등록하면 HTTP 409(코드 4500)를 반환합니다.
  앱이 저장한 설치 ID와 최신 토큰을 확인하고 재등록하세요. 동시에 처음 등록한 요청도 충돌할 수 있습니다.

성공 시 기존 `ApiResponseDto` 형식으로 아래 `result`를 반환합니다. 토큰 자체는 응답하지 않습니다.

```json
{
  "deviceId": 1,
  "installationId": "7fc5e5f4-49d8-4bbb-9db0-c1df9edba532",
  "platform": "ANDROID",
  "updatedAt": "2026-09-28T12:00:00"
}
```

## 연결 해제

`DELETE /api/v1/users/me/devices/{installationId}`

요청 본문은 없습니다. 로그아웃 시 JWT를 폐기하기 전에 호출합니다.
현재 사용자 소유인 해당 기기 행만 삭제하며, 반복 호출·없는 기기·다른 사용자 소유는 변경 없이 HTTP 200을 반환합니다.
따라서 계정 변경 후 이전 사용자의 지연된 로그아웃 요청이 새 사용자 연결을 삭제하지 않습니다.
연결 해제 실패 시 앱은 재시도해야 합니다. 이 API가 JWT 로그아웃 API에서 자동 호출되지는 않습니다.

## 저장 및 범위

`user_device` 테이블에 사용자 외래 키, 설치 ID, FCM 토큰, 플랫폼, 갱신 시각을 저장합니다.
설치 ID와 FCM 토큰은 각각 유일하며, 사용자 ID에는 조회용 인덱스가 있습니다.
현재 프로젝트의 `spring.jpa.hibernate.ddl-auto=update` 설정으로 서버 시작 시 테이블이 생성됩니다.
이 단계는 기기 연결 관리까지만 구현하며 실제 FCM 발송은 별도 서비스에서 연결합니다.
