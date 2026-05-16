# StickyMemo 프로젝트 리서치

작성일: 2026-05-15  
대상 경로: `E:\Appdevelopment\Stickymemo`

## 1. 프로젝트 개요

StickyMemo는 Android 네이티브 앱으로, Jetpack Compose 기반의 메모 앱에 맛집 기록 기능을 결합한 프로젝트다. 핵심 사용 시나리오는 일반 메모, 체크리스트, 위치 기반 알림 메모, 통화 중 메모, 맛집/레시피 링크 기록이다.

현재 `assembleDebug` 기준 빌드는 성공한다. 다만 Kotlin 소스와 XML 주석/문자열 다수에서 한글 인코딩 깨짐이 관찰되어, 실제 UI 문구 품질과 유지보수성에는 즉시 개선이 필요하다.

## 2. 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 플랫폼 | Android, Kotlin |
| UI | Jetpack Compose, Material 3 |
| 상태 관리 | AndroidX ViewModel, Kotlin Flow, StateFlow |
| 데이터 저장 | Room Database |
| 위치 | Google Play Services Location, Geofencing |
| 이미지 | Coil Compose, Android Photo Picker/Camera |
| 네트워크 | OkHttp, Retrofit/Gson 의존성 |
| 주소 검색 | Kakao Local Search API |
| 빌드 | Gradle Kotlin DSL, AGP 8.13.2, Kotlin 2.0.21 |

주요 의존성은 `app/build.gradle.kts`에 선언되어 있으며, Compose BOM은 `2024.11.00`, Room은 `2.6.1`, Lifecycle ViewModel Compose는 `2.8.7`을 사용한다.

## 3. 앱 구조

프로젝트는 단일 Android 앱 모듈 `app`으로 구성되어 있다.

주요 패키지:

| 경로 | 역할 |
| --- | --- |
| `com.ois.stickymemo` | MainActivity, 서비스, 브로드캐스트 리시버 |
| `com.ois.stickymemo.data` | Room Entity, DAO, Database |
| `com.ois.stickymemo.viewmodel` | Memo/Restaurant ViewModel |
| `com.ois.stickymemo.ui` | Compose 화면, 다이얼로그, 헬퍼 |
| `com.ois.stickymemo.ui.theme` | Compose 테마 |

핵심 진입점은 `MainActivity.kt`다. 앱은 상단 탭으로 `MEMO`와 `RESTAURANT`를 전환하며, 별도의 Navigation 라이브러리 없이 sealed class와 Compose state로 화면 전환을 관리한다.

## 4. 주요 기능

### 4.1 메모

메모 타입은 `MemoType` enum으로 정의된다.

| 타입 | 용도 |
| --- | --- |
| `NORMAL` | 일반 텍스트 메모 |
| `CHECKLIST` | JSON 문자열로 저장되는 체크리스트 |
| `LOCATION` | 위도/경도/반경 기반 위치 알림 메모 |
| `CALL` | 전화 수신/통화 중 오버레이 메모 |

메모 목록은 검색, 고정, 복제, 공유, 시간 그룹, 타입 그룹을 지원한다. 체크리스트는 진행률 미리보기를 제공한다.

### 4.2 위치 기반 알림

위치 메모 저장 시 `GeofenceHelper`가 Google Geofencing API에 지오펜스를 등록한다. 진입 이벤트는 `GeofenceBroadcastReceiver`가 받고, 해당 메모를 Room에서 조회한 뒤 알림을 띄운다.

현재 확인된 특이점:

- 위치 권한과 백그라운드 위치 권한이 Manifest에 선언되어 있다.
- `getCurrentLocation`은 마지막 위치만 읽는다.
- 지오펜스 삭제는 헬퍼에 구현되어 있으나, 메모 삭제 시 자동 호출 흐름은 명확히 연결되어 있지 않다.

### 4.3 통화 메모

`CallReceiver`가 전화 상태 이벤트를 받고 `CallOverlayService`를 시작한다. 서비스는 `WindowManager`와 `ComposeView`를 사용해 통화 중 작은 메모 입력 오버레이를 표시하고, 저장 시 `CALL` 타입 메모를 Room에 추가한다.

현재 확인된 특이점:

- `SYSTEM_ALERT_WINDOW`, `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS` 권한이 필요하다.
- Android 버전별 통화/발신 번호 접근 제약이 크므로 실제 기기 검증이 중요하다.
- 오버레이용 lifecycle owner를 직접 구성한다.

### 4.4 맛집 기록

맛집 기능은 `Restaurant` Entity와 `RestaurantViewModel`을 중심으로 동작한다.

지원 기능:

- 맛집명, 위치, 평점, 리뷰, 태그, 이미지 URI 저장
- 최신순/평점순 정렬
- 태그 필터
- 여러 이미지 선택
- 공유받은 텍스트 URL을 레시피 URL로 받아 맛집 등록 화면으로 진입
- Kakao Local Search API를 통한 주소/장소 검색

## 5. 데이터 모델

### 5.1 Memo

`Memo`는 단일 테이블 `memos`에 저장된다. 일반 메모, 체크리스트, 위치 메모, 통화 메모를 하나의 Entity에 nullable 필드로 합친 구조다.

주요 필드:

- `id`
- `type`
- `title`, `content`
- `latitude`, `longitude`, `locationName`, `locationRadius`
- `contactName`, `contactPhone`
- `checklistJson`
- `isCompleted`, `isPinned`
- `colorHex`
- `tags`
- `imageUri`
- `createdAt`, `updatedAt`

체크리스트는 별도 테이블이 아니라 JSON 문자열로 저장된다. 단순 구현에는 유리하지만 항목 단위 검색/통계/마이그레이션에는 불리하다.

### 5.2 Restaurant

`Restaurant`는 `restaurants` 테이블에 저장된다.

주요 필드:

- `id`
- `name`
- `location`
- `latitude`, `longitude`
- `rating`
- `review`
- `tags`
- `imageUris`
- `recipeUrl`, `recipeTitle`
- `visitedAt`, `createdAt`

태그와 이미지 URI는 콤마 구분 문자열이다. 단기적으로는 간단하지만, 값 자체에 콤마가 포함되거나 이미지별 메타데이터가 필요해질 경우 구조화가 필요하다.

## 6. 권한과 Manifest

Manifest에 선언된 주요 권한:

- `INTERNET`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- `CAMERA`, `READ_MEDIA_IMAGES`
- `POST_NOTIFICATIONS`
- `READ_CONTACTS`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`
- `READ_PHONE_STATE`, `READ_CALL_LOG`, `PROCESS_OUTGOING_CALLS`
- `SYSTEM_ALERT_WINDOW`

앱의 기능 범위상 권한이 넓다. 특히 백그라운드 위치, 통화 상태, 통화 기록, 오버레이 권한은 Play 정책과 사용자 신뢰 측면에서 민감도가 높다. 권한 요청 타이밍을 기능 사용 직전에 좁히는 UX가 필요하다.

## 7. 빌드 검증 결과

실행 명령:

```powershell
.\gradlew.bat assembleDebug
```

결과:

- `BUILD SUCCESSFUL`
- 최초 실행 시 Gradle 9.3.1 wrapper 다운로드가 필요했다.
- 경고: `kotlinOptions`가 deprecated이며 `compilerOptions`로 이전 권장
- 경고: deprecated Gradle features로 인해 Gradle 10 호환성 이슈 가능

## 8. 현재 리스크와 기술 부채

### P0/P1: 한글 문자열 인코딩 깨짐

소스 전반에서 한글 주석과 UI 문자열이 깨져 보인다. 빌드는 성공하지만 실제 사용자 화면 문구가 깨져 표시될 가능성이 높다. 예: 탭 라벨, 버튼, 알림 채널명, 다이얼로그 문구, Toast 메시지 등.

권장 조치:

- 파일 인코딩을 UTF-8로 통일
- UI 문자열을 `strings.xml`로 이동
- 깨진 문자열을 실제 한국어 문구로 복구

### P1: Kakao API 키 하드코딩

`KakaoAddressSearch.kt`에 Kakao REST API 키가 직접 포함되어 있다. 공개 저장소나 배포 앱에서는 키 유출과 할당량 오남용 위험이 있다.

권장 조치:

- `local.properties`, Gradle BuildConfig, 또는 서버 프록시를 사용
- 키 제한 도메인/앱 패키지/서명 설정 점검
- 기존 노출 키는 재발급 검토

### P1: Room destructive migration

`MemoDatabase`가 `fallbackToDestructiveMigration()`을 사용한다. DB 버전은 7이지만 명시적 migration이 없다. 스키마 변경 시 사용자 데이터가 삭제될 수 있다.

권장 조치:

- `exportSchema = true` 검토
- 버전별 Migration 작성
- 개발용 destructive migration과 배포용 migration 정책 분리

### P1: 민감 권한 UX와 정책 리스크

앱 시작 시 위치/연락처/알림 권한을 한꺼번에 요청하고, 오버레이 권한 설정 화면도 즉시 연다. 기능을 처음 쓰기 전부터 민감 권한을 요구하면 이탈률과 거부율이 높아질 수 있다.

권장 조치:

- 기능 진입 시점 권한 요청으로 변경
- 권한 요청 전 자체 설명 화면 제공
- 통화/백그라운드 위치 기능은 명확한 opt-in 흐름 추가

### P2: 데이터 정규화 부족

체크리스트 JSON, 태그 콤마 문자열, 이미지 URI 콤마 문자열은 빠른 개발에는 적합하지만 검색, 동기화, 통계, 마이그레이션 확장성은 낮다.

권장 조치:

- 체크리스트 항목 테이블 분리 검토
- 태그 테이블 및 N:N 매핑 검토
- 이미지 첨부 테이블 분리 검토

### P2: ViewModel/Repository 계층 부재

ViewModel이 직접 `MemoDatabase.getDatabase(...).dao()`를 사용한다. 현재 규모에서는 동작하지만 테스트, mocking, 데이터 소스 교체가 어려워진다.

권장 조치:

- Repository 계층 도입
- DAO 인터페이스 주입
- ViewModel unit test 작성

## 9. 테스트 현황

현재 테스트 파일은 Android Studio 기본 템플릿 수준으로 보인다.

- `ExampleUnitTest.kt`
- `ExampleInstrumentedTest.kt`

실제 비즈니스 로직, DAO, ViewModel, UI, 권한 흐름, 지오펜스/통화 오버레이에 대한 테스트는 확인되지 않았다.

우선 추가하면 좋은 테스트:

- `MemoDao` CRUD 및 정렬 테스트
- `RestaurantDao` 태그 필터/평점 정렬 테스트
- 체크리스트 JSON 변환 테스트
- `RestaurantViewModel` 정렬/필터 StateFlow 테스트
- 주요 Compose 화면 스모크 테스트

## 10. 개선 로드맵 제안

### 1단계: 앱 품질 복구

- 깨진 한글 문자열 복구
- 문자열 리소스화
- 민감 API 키 제거
- `assembleDebug`와 기본 테스트를 CI 또는 로컬 스크립트로 고정

### 2단계: 데이터 안정성 확보

- Room migration 작성
- destructive migration 제거
- 체크리스트/태그/이미지 데이터 구조 개선 검토

### 3단계: 권한과 사용자 흐름 정리

- 기능별 권한 요청으로 변경
- 백그라운드 위치/오버레이/통화 권한에 설명 UI 추가
- 권한 거부 시 graceful fallback 구현

### 4단계: 아키텍처 정돈

- Repository 계층 추가
- ViewModel 테스트 가능성 개선
- 화면 전환을 Navigation Compose로 이전할지 검토

### 5단계: 기능 고도화

- 메모 상세 검색과 태그 필터 강화
- 맛집 지도 보기
- 레시피 URL 메타데이터 파싱
- 백업/내보내기 UX 개선
- 알림 히스토리 및 완료 처리

## 11. 한줄 평가

StickyMemo는 메모, 위치 알림, 통화 오버레이, 맛집 기록이라는 생활 밀착형 기능을 한 앱에 담은 실험적 Android Compose 프로젝트다. 현재는 빌드 가능한 프로토타입 수준이며, 가장 먼저 문자열 인코딩/권한 UX/API 키/DB migration을 정리하면 실제 사용 가능한 앱으로 빠르게 안정화할 수 있다.
