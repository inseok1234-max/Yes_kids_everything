# StickyMemo Rebuild Plan

작성일: 2026-05-16

## 1. 프로젝트 재정의

StickyMemo는 여러 앱이 섞인 형태에서 벗어나 아래 개념으로 재정의한다.

```txt
StickyMemo = 상황 기반 개인 기록 앱

빠르게 기록하고,
나중에 쉽게 다시 찾는 앱
```

핵심은 빠른 입력, 상황 기록, 장소 기반 기록, 나중에 다시 찾기 쉬운 구조다.

## 2. 핵심 UX 방향

앱을 정리 도구보다 "생각나는 순간 바로 붙이는 앱"으로 설계한다.

우선순위:

1. 빠른 입력
2. 최소 탭 수
3. 즉시 저장
4. 검색 용이성
5. 기록 재발견

## 3. 정보 구조 재설계

기존 `MEMO / RESTAURANT` 구조는 앱이 두 개 붙은 느낌을 준다. 하단 내비게이션 기반으로 변경한다.

```txt
Home
Records
Places
Settings
```

## 4. 탭별 역할

### Home

앱의 핵심 화면. 앱을 열자마자 기록하고 최근 정보를 다시 보여준다.

구성:

- Quick Capture
- Pinned Records
- Recent Records
- Today Checklist
- Upcoming Reminders
- Recent Places

벤치마크:

- Google Keep
- Samsung Notes

### Records

모든 기록을 통합해서 보여준다.

기록 타입:

- Memo
- Task
- Place
- Reminder
- Call

기능:

- 전체 검색
- 타입 필터
- 태그 필터
- 색상 필터
- 정렬

UI 방향:

- Compact Card
- 빠른 스크롤
- Google Keep 스타일

### Places

기존 Restaurant 기능을 `Place Record`로 재구성한다. 맛집 앱처럼 보이지 않게 하고, 맛집/카페/병원/거래처/여행지까지 저장 가능한 장소 기록으로 확장한다.

표시 정보:

- 장소명
- 주소
- 태그
- 평점
- 메모
- 사진
- 방문일
- 관련 URL

UI 방향:

- Large Card
- 이미지 중심
- 장소 카드 UX

벤치마크:

- Beli
- Foursquare

### Settings

구성:

- 권한 관리
- 백업/내보내기
- 데이터 관리
- 고급 기능
- 앱 정보

민감 기능은 고급 기능으로 이동한다.

- 통화 메모
- 오버레이 메모
- 백그라운드 위치

## 5. 작성 UX 재설계

전역 FAB 하나로 기록 시작점을 통합한다.

FAB 클릭 시 ModalBottomSheet:

- 빠른 메모
- 체크리스트
- 장소 기록
- 위치 알림
- 통화 메모

원칙:

```txt
3초 안에 기록 시작
```

## 6. 위젯 전략

위젯은 정보 표시보다 빠른 입력 중심으로 설계한다.

핵심 위젯:

- Quick Capture Widget

Medium Widget 기준:

- 빠른 메모
- 체크리스트
- 위치 메모
- 사진 첨부
- 최근 기록 2~3개

UX:

- 앱 전체 실행 없이 Quick Capture 화면만 표시
- 즉시 저장
- 즉시 종료 가능

기술 방향:

- Jetpack Glance
- Material 3

벤치마크:

- Google Keep Widget
- Samsung Notes Widget
- TickTick Widget

## 7. 디자인 시스템 방향

핵심 스타일:

```txt
70% Google Keep
20% Samsung Notes
10% Notion
```

Places 탭 한정:

- Beli
- Foursquare

## 8. 디자인 원칙

유지할 것:

- Google Keep: 빠른 기록, 카드형 UI, 색상, 라벨, 검색
- Samsung Notes: 상세 화면 안정감, 첨부 UX, 이미지 삽입, 툴바
- Notion: 타입 구조, 속성 배지, 정리감

금지할 것:

- Notion식 블록 에디터
- 복잡한 데이터베이스 UX
- 과한 애니메이션
- 음식 배달앱 같은 전체 UI
- 작은 버튼 남발

## 9. 기술 구조 개선

현재 문제:

- ViewModel이 DAO 직접 참조
- DB 구조 혼합
- destructive migration
- 문자열 하드코딩
- API 키 노출
- 권한 요청 과다

개선 방향:

1. 문자열 UTF-8 복구, `strings.xml` 이동, API 키 제거, 권한 요청 시점 분리
2. Repository 도입, UI State 정리, 통합 RecordCard 모델 추가
3. Room Migration 작성, Checklist 테이블 분리, Tag 테이블 검토, Image Attachment 구조화

## 10. 데이터 모델 방향

현재는 `Memo`와 `Restaurant`가 분리되어 있다. UI 관점에서는 `Record` 하나로 보이게 한다.

```txt
Record
 ├ Memo
 ├ Task
 ├ Place
 ├ Reminder
 └ Call
```

초기 단계에서는 DB를 전부 갈아엎지 않는다. ViewModel, UI Adapter, Presentation Layer에서 먼저 통합한다.

## 11. 권한 정책 재설계

신규 원칙:

```txt
기능 사용할 때만 요청
```

예시:

- 위치 메모 생성 시 위치 권한 요청
- 통화 메모 활성화 시 설명 화면 후 `READ_PHONE_STATE`, `SYSTEM_ALERT_WINDOW` 요청

## 12. MVP 우선순위

### Phase 1: 안정화

- 문자열 복구
- API 키 제거
- 권한 흐름 정리
- 기본 UI 정리

### Phase 2: 구조 개편

- Home 추가
- FAB 통합
- Places 구조 재설계
- RecordCard UI 도입

### Phase 3: 위젯

- Quick Capture Widget
- 최근 기록 표시
- 체크리스트 위젯

### Phase 4: 데이터 구조 개선

- Migration
- Repository
- Tag 구조화
- Checklist 정규화

### Phase 5: 고급 기능

- 지도 보기
- 자동 장소 추천
- 위치 기반 추천
- 알림 히스토리
- 백업/동기화

## 13. 최종 목표 UX

사용자는 앱을 열고 바로 기록하고 나중에 쉽게 찾는다.

StickyMemo는 정리 앱보다 생각을 붙잡아두는 앱이 되어야 한다.

## 14. 최종 한줄 방향

```txt
Google Keep의 빠른 기록성과
Samsung Notes의 안정감,
Notion의 구조화 개념,
Places 기록 기능을 결합한
상황 기반 개인 기록 앱
```
