# Meta AI Glasses 카메라 프리뷰 재사용 가이드

다른 Android/Jetpack Compose 프로젝트에서 Meta AI Glasses의 실시간 카메라 프리뷰를 화면에 표시하기 위한 에이전트용 작업 기준이다. 기준 구현은 이 저장소의 Meta Wearables DAT Android `0.9.0` `CameraAccess` 샘플이다.

## 1. 프리뷰는 다음 파이프라인이다

```text
안경 카메라
  -> DAT DeviceSession (STARTED)
  -> Camera + Stream (STREAMING)
  -> Stream.videoStream의 압축 HEVC VideoFrame
  -> MediaCodec HEVC 디코더
  -> Android Surface
  -> AndroidExternalSurface(Compose 화면)
```

`videoStream`의 프레임을 `Image` 또는 `ImageBitmap`으로 매 프레임 변환하지 않는다. 압축 HEVC를 `MediaCodec`으로 디코드해 `Surface`에 직접 렌더링해야 프리뷰와 Compose UI가 함께 부드럽게 동작한다.

## 2. 재사용할 기존 파일

새 디코더를 작성하지 말고, 아래 파일을 대상 프로젝트의 패키지에 맞춰 복사하고 import만 정리한다.

| 역할 | 현재 파일 | 대상 프로젝트에서 할 일 |
| --- | --- | --- |
| HEVC 디코딩·Surface 렌더링 | `AIGlassFood/app/src/main/java/com/mtvs/food/stream/HevcDecoder.kt` | 그대로 재사용한다. `MediaCodec`, 별도 decoder thread, codec config 및 keyframe 처리가 포함돼 있다. |
| 프레임 수집·디코더 생명주기 | `AIGlassFood/app/src/main/java/com/mtvs/food/camera/CameraViewModel.kt` | 기존 카메라 ViewModel이 있으면 프리뷰 관련 필드와 흐름만 합친다. |
| Compose Surface 호스트 | `meta-wearables-dat-android/samples/CameraAccess/.../ui/CameraScreen.kt`의 `PreviewBackground` | `AndroidExternalSurface`와 `onSurface`/`onDestroyed` 패턴을 사용한다. |

## 3. 필수 구현 순서

1. 기기 상태 가이드의 연결 흐름을 먼저 구현한다. 세션이 `STARTED`이기 전에는 스트림을 만들지 않는다.
2. 카메라 권한을 확인·요청한 뒤 `session.addCamera(StreamConfiguration(...))`로 capability를 붙인다.
3. 프리뷰에는 `compressVideo = true`를 설정한다. 이 프레임이 HEVC 디코더의 입력이다.
4. `stream.videoStream`, `stream.state`, `stream.errorStream` collector를 **`stream.start()` 전에** 등록한다.
5. Compose가 제공한 `Surface`를 ViewModel/preview controller에 전달한다.
6. 첫 압축 프레임에서만 `HevcDecoder`를 만들고 해당 `Surface`에 연결한다. 이후 프레임은 같은 디코더로 전달한다.
7. Surface가 파괴되거나 stream이 `STOPPED`/`CLOSED`가 되면 디코더를 `stop()`하고 참조를 비운다.

## 4. 최소 스트림 설정

프리뷰만 필요하면 녹화기나 foreground service는 추가하지 않는다.

```kotlin
val camera = session.addCamera(
    StreamConfiguration(
        videoQuality = VideoQuality.MEDIUM,
        frameRate = 24,
        compressVideo = true,
    ),
).getOrElse { error ->
    // 기존 앱의 오류 상태로 전달
    return
}

val stream = camera.stream
observePreviewStream(stream) // video/state/error collector를 먼저 등록
stream.start()
```

지원되는 품질은 `HIGH`(720x1280), `MEDIUM`(504x896), `LOW`(360x640)다. 처음에는 `MEDIUM` / `24fps`를 사용한다. 발열·지연·프레임 드롭이 실제 기기에서 확인되면 `LOW` 또는 `15fps`로 내린다.

## 5. Compose Surface 호스트

```kotlin
@Composable
fun GlassPreview(
    isPreviewActive: Boolean,
    onSurfaceChanged: (Surface?) -> Unit,
) {
    if (!isPreviewActive) return

    AndroidExternalSurface(modifier = Modifier.fillMaxSize()) {
        onSurface { surface, _, _ ->
            onSurfaceChanged(surface)
            surface.onDestroyed { onSurfaceChanged(null) }
        }
    }
}
```

`AndroidExternalSurface`는 Compose 안에서 사용하는 `SurfaceView` 기반 출력 대상이다. 프리뷰 위에 버튼·배지·반투명 오버레이를 `Box`로 올릴 수 있다. `Surface` 객체를 Compose 상태에 저장하지 말고 ViewModel/controller에 전달해 디코더가 소유하게 한다.

## 6. 프레임 처리 규칙

```text
compressed VideoFrame 수신
  -> buffer를 ByteArray로 복사하고 원래 position 복원
  -> VPS/SPS/PPS(codec config) 누적
  -> Surface가 있으면 디코더를 한 번 생성
  -> 누적한 codec config를 디코더에 먼저 전달
  -> 현재 프레임 전달
  -> 첫 일반 프레임 수신 시 프리뷰 표시 상태 갱신
```

VPS/SPS/PPS와 keyframe 처리를 생략하면 Surface가 늦게 생성된 경우 검은 화면이 남을 수 있다. 이 때문에 `HevcDecoder.kt`를 단순 MediaCodec 코드로 대체하지 않는다.

프레임 처리와 디코더 종료는 같은 lock으로 보호한다. `onDestroyed`와 `videoStream` collector가 동시에 실행될 수 있어, 해제된 Surface에 새 디코더를 연결하면 안 된다.

## 7. 상태와 정리 규칙

| 이벤트 | UI | 처리 |
| --- | --- | --- |
| `StreamState.STARTING` | 로딩 | Surface는 유지, 첫 프레임을 기다린다. |
| `StreamState.STREAMING` + 첫 일반 프레임 | 라이브 | 프리뷰와 제어 UI를 표시한다. |
| 세션 `PAUSED` | 마지막 프레임 + 일시중지 배지 | 스트림을 임의 재시작하지 않고 다음 상태를 기다린다. |
| `StreamState.STOPPED` 또는 `CLOSED` | 프리뷰 제거 | collector, 디코더, camera capability를 정리한다. |
| `Stream.errorStream` | 오류 안내 | 오류를 UI/log에 전달하고 필요한 정리를 수행한다. |
| Compose Surface 파괴 | 프리뷰 없음 | 디코더만 해제한다. 세션·스트림 정책은 화면 수명과 분리한다. |

`STOPPED` 상태의 기존 `Stream`/`Camera`를 새 연결처럼 재사용하지 않는다. 다음 프리뷰는 새 capability를 붙인다.

## 8. 에이전트 작업 프롬프트

```text
Meta Wearables DAT 카메라 프리뷰를 기존 Android Compose 프로젝트에 추가한다.

- 기존 카메라/연결 ViewModel과 상태 모델을 먼저 찾아 재사용한다. UI는 DAT 타입이나 VideoFrame을 직접 참조하지 않는다.
- session.state == STARTED 뒤에 Camera를 추가하고, StreamConfiguration은 MEDIUM, 24fps, compressVideo=true로 시작한다.
- videoStream/state/error collector를 stream.start() 전에 등록한다.
- AIGlassFood의 stream/HevcDecoder.kt를 재사용하고, MediaCodec 출력은 Compose AndroidExternalSurface가 제공한 Surface로 직접 렌더링한다.
- 프레임을 ImageBitmap으로 매 프레임 변환하거나 새 디코더를 작성하지 않는다.
- VPS/SPS/PPS, keyframe, Surface destroy, stream terminal 상태를 기존 HevcDecoder/CameraViewModel 패턴대로 처리한다.
- 프리뷰만 구현한다. 녹화·파일 저장·foreground service는 요구가 있을 때만 추가한다.
- MockDeviceKit과 실제 안경에서 각각 상태 전이, 첫 프레임, 중지, 화면 회전/이탈을 검증하고 결과를 분리해 보고한다.
```

## 9. 완료 기준

- [ ] `STARTED` 전에는 프리뷰 시작 버튼이 비활성화된다.
- [ ] `STREAMING` 후 첫 일반 프레임이 Surface에 보인다.
- [ ] UI 오버레이가 프리뷰 위에 정상 표시된다.
- [ ] 화면 이탈, Surface 파괴, stream 종료에서 크래시·해제된 Surface 접근이 없다.
- [ ] MockDeviceKit 검증과 실제 안경 프리뷰 검증을 구분해 기록한다.

## 10. 확인할 원본

- `AIGlassFood/app/src/main/java/com/mtvs/food/stream/HevcDecoder.kt`
- `AIGlassFood/app/src/main/java/com/mtvs/food/camera/CameraViewModel.kt`
- `meta-wearables-dat-android/samples/CameraAccess/app/src/main/java/.../ui/CameraScreen.kt`
- `meta-wearables-dat-android/plugins/mwdat-android/skills/camera-streaming/SKILL.md`
- Meta DAT Android API reference: https://wearables.developer.meta.com/docs/reference/android/dat/latest
