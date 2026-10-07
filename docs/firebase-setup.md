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

저장소의 Settings → Secrets and variables → Actions → Repository secrets에 다음 값을 등록합니다.

| Secret 이름 | 값 |
| --- | --- |
| `FIREBASE_PROJECT_ID` | React Native 앱과 같은 Firebase 프로젝트 ID |
| `GOOGLE_APPLICATION_CREDENTIALS` | 서비스 계정 JSON 파일의 전체 내용 (`{`부터 `}`까지). 경로나 Base64 값이 아닙니다. |
| `EC2_KNOWN_HOSTS` | 신뢰할 수 있는 경로에서 확인한 EC2 SSH 호스트 키 한 줄 전체 |

GitHub Secret은 JSON 내용을 보관하고, 실행 환경변수는 파일 경로를 가리킵니다.
`.github/workflows/deploy.yml`이 이 변환을 수행합니다.

1. 빌드 전에 JSON 형식과 프로젝트 ID 일치를 검사합니다.
2. Actions 임시 디렉터리에 인증 파일을 만들고 파일 경로를 `GOOGLE_APPLICATION_CREDENTIALS`로 설정합니다.
3. 기존 방식으로 JAR를 빌드하고 S3에 업로드합니다. 인증 파일은 JAR와 S3에 포함하지 않습니다.
4. `EC2_KNOWN_HOSTS`의 호스트 키가 `EC2_HOST`와 일치하는지 확인합니다.
5. 호스트 키 검증에 성공한 경우에만 SSH로 EC2의 `/home/<EC2_USER>/app/secrets/firebase-service-account.json`에 별도 전달합니다.
6. EC2에서 기존 `.env`를 읽은 뒤 Firebase 활성화 값, 프로젝트 ID, 인증 파일 경로를 설정하고 JAR를 실행합니다.
7. Actions 임시 인증 파일과 SSH 파일을 작업 종료 시 삭제합니다. EC2 인증 파일은 서버 실행에 필요하므로 유지합니다.

### EC2 호스트 키 Secret 준비

Actions 실행 중 `ssh-keyscan`으로 얻은 값을 즉시 신뢰하면 중간자 공격을 막을 수 없습니다.
EC2에 AWS Systems Manager Session Manager 등 이미 신뢰할 수 있는 경로로 접속하여 서버의 SSH 공개 키 지문을 먼저 확인합니다.

```sh
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

관리자 PC에서 EC2 호스트 키를 가져와 지문이 같은지 비교합니다.

```sh
ssh-keyscan -t ed25519 <EC2_HOST> > ec2_known_hosts
ssh-keygen -lf ec2_known_hosts
```

지문이 일치하면 `ec2_known_hosts`의 한 줄 전체를 GitHub Actions의 `EC2_KNOWN_HOSTS` Secret에 저장합니다.
EC2 인스턴스를 교체하거나 SSH 호스트 키를 재생성한 경우에는 새 지문을 다시 확인한 뒤 Secret을 갱신해야 합니다.

인증 디렉터리는 `700`, 인증 파일은 `600` 권한으로 관리합니다.
`FIREBASE_ENABLED`는 이 배포 경로에서 자동으로 `true`가 됩니다. EC2 `.env`에 Firebase 항목을 추가할 필요는 없습니다.
필수 Secret이 없거나 `EC2_HOST`와 호스트 키가 일치하지 않으면 배포가 실패합니다. 현재 워크플로는 `main` 브랜치에 push될 때 실행됩니다.
`bootJar`는 Firebase에 접속하지 않으며 실제 인증은 서버 실행·FCM 요청 시 사용됩니다.

공식 문서: https://firebase.google.com/docs/admin/setup
