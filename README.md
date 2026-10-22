# generator-jpo

**공공데이터포털(data.go.kr) 금융위원회 공시 API의 응답을 읽어서 JPA 코드(Entity, Repository, Service)를 자동으로 만들어 주는 도구**입니다.
[jpa-mini-project](https://github.com/jeong-jihyun/jpa-mini-project)에 있는 약 20개 공시 엔티티를 손으로 쓰는 대신 만들기 위해 쓴 코드 생성기입니다.

## 동작 방식

```
API 주소 → 호출(1건) → 응답의 항목 이름·값을 읽음 → 필드 타입 추측 → 파일 3개 생성
```

| 생성 파일 | 내용 |
| --- | --- |
| `{이름}Entity.java` | `@Entity` 클래스. 첫 필드로 `Long id`(자동 증가)를 넣고, 응답 항목마다 `@Column` 필드를 만듦 (항목 이름에 `Ctt`가 들어가면 길이 4000) |
| `{이름}Repository.java` | Repository 인터페이스 |
| `{서비스이름}.java` | API를 페이지 단위로 호출해 저장하는 서비스 틀 |

- 호출 주소에서 이름을 뽑습니다. 예: `.../GetDiscInfoService_V2/getOutsDireHumaResoAffaRepo_V2` → 클래스 이름 `OutsDireHumaResoAffaRepo`, 서비스 이름 `DiscInfoService`
- 주소의 `resultType`이 `xml`(없으면 기본값)이면 XML로, 아니면 JSON으로 읽습니다.
- 파일은 **실행한 폴더**(현재 작업 디렉터리)에 만들어집니다.

## 사용 기술

| 구분 | 내용 |
| --- | --- |
| 언어 / 빌드 | Java (Gradle toolchain 23), Spring Boot 3.4.1 |
| 라이브러리 | json-simple 1.1.1 (JSON 파싱), 표준 XML(DOM) 파서 |
| 구성 | `MakerApplication`(시작점·호출·생성), `util/GeneratorUtil`(이름 변환, 타입 추측, 파일 쓰기) |

> `MakerApplication`에는 `@SpringBootApplication`이 붙어 있지만 `main`에서 Spring을 띄우지 않고 일반 프로그램처럼 실행됩니다.

## 사용 방법

### 1. 준비물

- JDK 23 (Gradle toolchain이 요구하는 버전. `item.getFirst()`를 쓰므로 21 이상이 필요)
- 공공데이터포털 **인증키**

### 2. 실행

```bash
# 방법 A: 기본 예시(사외이사 신고 조회)를 인증키만 넣어서 실행
export SERVICE_KEY=발급받은_인증키
./gradlew bootRun

# 방법 B: 만들고 싶은 API 주소를 인자로 직접 전달
./gradlew bootRun --args='"https://apis.data.go.kr/1160100/service/GetDiscInfoService_V2/getDishDiscInfo_V2?serviceKey=인증키&numOfRows=1&pageNo=1&resultType=json"'
```

실행하면 현재 폴더에 `…Entity.java`, `…Repository.java`, `…Service.java`가 생깁니다.

## 폴더 구조

```
generator-jpo/
├── build.gradle, settings.gradle, gradlew   # 빌드 설정
└── src/
    ├── main/java/com/jpo/generator/maker/
    │   ├── MakerApplication.java            # 호출·XML/JSON 처리·코드 생성
    │   └── util/GeneratorUtil.java          # 보조 함수
    ├── main/resources/application.properties
    └── test/java/.../MakerApplicationTests.java   # 빈 contextLoads 테스트
```

## 개선 내역 (2026-10)

| 구분 | 문제 | 수정 내용 |
| --- | --- | --- |
| 비밀 값 노출 | 공공데이터 인증키가 `MakerApplication`의 호출 주소에 그대로 들어 있었음 | 첫 번째 인자 또는 환경변수 `SERVICE_KEY`로 받도록 변경. 둘 다 없으면 안내 문구를 출력하고 종료 |

## 확인하지 못한 것

이 작업 환경에서는 Maven Central과 data.go.kr에 접속할 수 없어 **빌드와 실제 생성 실행은 하지 못했습니다.** 위 설명은 코드를 읽어서 확인한 내용이고, 수정한 부분은 `javac`로 문법 오류가 없는 것까지만 확인했습니다. (의존 라이브러리가 없어 나오는 "찾을 수 없음" 오류는 제외)

## ⚠️ 보안 안내

이전 커밋 기록에 **인증키가 남아 있습니다.** 파일에서는 지웠지만 기록에는 남으므로 **인증키 재발급**을 권장합니다.

## 알려진 한계 (코드에서 확인한 내용)

생성되는 **서비스 파일은 그대로는 컴파일되지 않고 손질이 필요**합니다.

- `import`가 일부만 들어가고(`IOException`, `URL`, `HttpsURLConnection` 등 없음), `@Slf4j`와 생성자가 없어 `log`, Repository 필드를 쓸 수 없습니다.
- 로그 문구에 `BusiSuspDiscInfo`가 고정으로 들어갑니다.
- 만들어진 서비스는 JSON 방식 코드를 쓰면서 주소에는 `resultType=xml`을 붙입니다.
- `GeneratorUtil.getQueryMap`은 `값이 없는 파라미터`(`a=`)가 있으면 오류가 납니다.
