# Firebase Admin SDK 설정

백엔드에서 Firebase Admin SDK를 초기화하고 `FirebaseMessaging` 빈을 제공합니다.
기기 토큰 API와 푸시 발송 로직은 다음 단계에서 연결합니다.

## Firebase 콘솔 준비

1. React Native 앱과 같은 Firebase 프로젝트를 선택합니다.
2. 프로젝트 설정 → 서비스 계정 → Firebase Admin SDK에서 서비스 계정 키를 발급합니다.
3. JSON 파일을 서버에 비밀 파일로 배치합니다. 소스 코드나 배포 이미지에 포함하지 않습니다.
   로컬에서 프로젝트 내부에 둘 경우 Git에서 제외된 `secrets/` 디렉터리를 사용합니다.

## 서버 환경변수

```sh
export FIREBASE_ENABLED=true
export FIREBASE_PROJECT_ID=your-firebase-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

- `FIREBASE_ENABLED`: 기본값 `false`. 인증 정보가 없는 개발·테스트 환경에서는 초기화를 생략합니다.
- `FIREBASE_PROJECT_ID`: Firebase 콘솔의 프로젝트 ID입니다. 활성화 시 반드시 지정합니다.
- `GOOGLE_APPLICATION_CREDENTIALS`: 서버에서 읽을 수 있는 서비스 계정 JSON 파일의 절대 경로입니다.
  JSON 내용 자체를 넣는 변수가 아닙니다. 컨테이너에서는 파일을 마운트하고 컨테이너 내부 경로를 지정합니다.

환경변수를 설정한 뒤 기존 방식으로 서버를 실행합니다. IDE 실행 시에도 실행 설정에 같은 변수를 지정합니다.
SDK는 Application Default Credentials(ADC)를 사용하므로, 지원되는 배포 환경에서는 연결된 서비스 계정도 사용할 수 있습니다.
Firebase를 활성화했는데 프로젝트 ID가 없거나 인증 정보를 읽을 수 없으면 서버 시작이 실패합니다.

초기화 성공은 실제 FCM 발송 권한이나 기기 수신까지 검증한 결과가 아닙니다.
기기 토큰과 발송 로직을 연결한 뒤 실제 기기에서 별도로 검증해야 합니다.

## GitHub Actions → EC2 JAR 배포

저장소의 Settings → Secrets and variables → Actions → Repository secrets에 다음 두 값을 등록합니다.

| Secret 이름 | 값 |
| --- | --- |
| `FIREBASE_PROJECT_ID` | React Native 앱과 같은 Firebase 프로젝트 ID |
| `GOOGLE_APPLICATION_CREDENTIALS` | 서비스 계정 JSON 파일의 전체 내용 (`{`부터 `}`까지). 경로나 Base64 값이 아닙니다. |

GitHub Secret은 JSON 내용을 보관하고, 실행 환경변수는 파일 경로를 가리킵니다.
`.github/workflows/deploy.yml`이 이 변환을 수행합니다.

1. 빌드 전에 JSON 형식과 프로젝트 ID 일치를 검사합니다.
2. Actions 임시 디렉터리에 인증 파일을 만들고 파일 경로를 `GOOGLE_APPLICATION_CREDENTIALS`로 설정합니다.
3. 기존 방식으로 JAR를 빌드하고 S3에 업로드합니다. 인증 파일은 JAR와 S3에 포함하지 않습니다.
4. SSH로 EC2의 `/home/<EC2_USER>/app/secrets/firebase-service-account.json`에 별도 전달합니다.
5. EC2에서 기존 `.env`를 읽은 뒤 Firebase 활성화 값, 프로젝트 ID, 인증 파일 경로를 설정하고 JAR를 실행합니다.
6. Actions 임시 인증 파일을 작업 종료 시 삭제합니다. EC2 파일은 서버 실행에 필요하므로 유지합니다.

인증 디렉터리는 `700`, 인증 파일은 `600` 권한으로 관리합니다.
`FIREBASE_ENABLED`는 이 배포 경로에서 자동으로 `true`가 됩니다. EC2 `.env`에 Firebase 항목을 추가할 필요는 없습니다.
두 Secret이 없으면 배포는 빌드 전에 실패합니다. 현재 워크플로는 `main` 브랜치에 push될 때 실행됩니다.
`bootJar`는 Firebase에 접속하지 않으며 실제 인증은 서버 실행·FCM 요청 시 사용됩니다.

공식 문서: https://firebase.google.com/docs/admin/setup
