# StickyMemo Phase 2 Tasks

기준 문서: [REBUILD_PLAN.md](REBUILD_PLAN.md)

## 목표

`MEMO / RESTAURANT`가 나뉜 현재 구조를 `Home / Records / Places / Settings` 중심의 상황 기반 기록 앱 구조로 바꾼다.

## 작업 순서

### 1. Navigation Shell

- 하단 내비게이션 `Home / Records / Places / Settings` 추가
- 기존 `MemoListScreen`은 `Records`로 이동
- 기존 `RestaurantListScreen`은 `Places`로 이동
- 기존 상단 `MEMO / RESTAURANT` 탭 제거

### 2. Global FAB

- 화면별 FAB를 전역 FAB 하나로 통합
- FAB 클릭 시 `ModalBottomSheet` 표시
- 액션: 빠른 메모, 체크리스트, 장소 기록, 위치 알림, 통화 메모
- 위치/통화 메모는 기존 권한 지연 요청 흐름 유지

### 3. Home Screen

- Quick Capture 입력 영역
- Pinned Records
- Recent Records
- Today Checklist
- Recent Places
- 기존 DB 구조를 유지하고 presentation layer에서만 통합

### 4. Record Presentation Model

- `RecordCardUiModel` 추가
- `Memo`와 `Restaurant`를 UI 표시용 Record로 매핑
- DB 마이그레이션 없이 Records/Home에서 통합 노출

### 5. Places Naming

- 사용자 노출 문구에서 `맛집`을 `장소` 중심으로 변경
- 내부 Entity 이름은 Phase 2에서는 유지
- `RestaurantListScreen` UI 문구만 우선 변경

### 6. Settings

- 권한 관리 섹션 추가
- 고급 기능 섹션에 통화 메모, 오버레이 메모, 백그라운드 위치 배치
- 실제 기능 활성화는 opt-in 흐름에서만 권한 요청

## 완료 조건

- `./gradlew.bat assembleDebug` 성공
- 앱 첫 화면이 Home
- 전역 FAB에서 모든 기록 생성 진입 가능
- 기존 메모/장소 CRUD 회귀 없음
- Places가 맛집 전용 앱처럼 보이지 않음
