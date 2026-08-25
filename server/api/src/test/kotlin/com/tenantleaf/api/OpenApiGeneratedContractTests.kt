package com.tenantleaf.api

import com.tenantleaf.api.generated.api.PropertiesApi
import com.tenantleaf.api.generated.api.InspectionsApi
import com.tenantleaf.api.generated.api.MediaApi
import com.tenantleaf.api.generated.api.ObservationsApi
import com.tenantleaf.api.generated.api.ReportsApi
import com.tenantleaf.api.generated.model.Bbox
import com.tenantleaf.api.generated.model.BboxCoordinateSystem
import com.tenantleaf.api.generated.model.AiLabel
import com.tenantleaf.api.generated.model.CaptureSource
import com.tenantleaf.api.generated.model.CreateMediaUploadRequest
import com.tenantleaf.api.generated.model.CreatePropertyRequest
import com.tenantleaf.api.generated.model.FrameOrigin
import com.tenantleaf.api.generated.model.InspectionAnalysisStatus
import com.tenantleaf.api.generated.model.InspectionStatus
import com.tenantleaf.api.generated.model.MediaAnalysisStatus
import com.tenantleaf.api.generated.model.MediaUploadStatus
import com.tenantleaf.api.generated.model.ObservationStatus
import com.tenantleaf.api.generated.model.ObservationType
import com.tenantleaf.api.generated.model.ReportStatus
import com.tenantleaf.api.generated.model.Zone
import com.tenantleaf.api.generated.model.ZoneAnalysisStatus
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenApiGeneratedContractTests {

	private val validator = Validation.buildDefaultValidatorFactory().validator

	@Test
	fun `API 인터페이스는 공통 기본 경로를 사용한다`() {
		assertEquals("/api/v1", PropertiesApi.BASE_PATH)
		assertEquals("/api/v1", InspectionsApi.BASE_PATH)
		assertEquals("/api/v1", MediaApi.BASE_PATH)
		assertEquals("/api/v1", ObservationsApi.BASE_PATH)
		assertEquals("/api/v1", ReportsApi.BASE_PATH)
	}

	@Test
	fun `근거 영역은 원본 JPEG 픽셀 xyxy 계약을 생성한다`() {
		val box = Bbox(left = 10.0, top = 20.0, right = 110.0, bottom = 220.0)

		assertEquals(10.0, box.left)
		assertEquals(220.0, box.bottom)
		assertEquals(listOf("PIXEL_XYXY"), BboxCoordinateSystem.entries.map { it.value })
	}

	@Test
	fun `임장 상태는 진행 종료 취소만 생성한다`() {
		assertEquals(
			listOf("IN_PROGRESS", "ENDED", "CANCELLED"),
			InspectionStatus.entries.map { it.value },
		)
	}

	@Test
	fun `임장 전체 분석 상태는 서버 집계값 일곱 가지를 생성한다`() {
		assertEquals(
			listOf(
				"NOT_STARTED",
				"UPLOADING",
				"QUEUED",
				"ANALYZING",
				"PARTIAL_COMPLETED",
				"COMPLETED",
				"FAILED",
			),
			InspectionAnalysisStatus.entries.map { it.value },
		)
	}

	@Test
	fun `AI 원본 라벨 열세 가지와 other를 생성한다`() {
		assertEquals(
			listOf(
				"crack",
				"mold",
				"peeling",
				"waterdamage",
				"tiledamage",
				"hole",
				"tilecrack",
				"paintdrips",
				"pinhole",
				"surfacedefect",
				"stain",
				"trowelmark",
				"other",
			),
			AiLabel.entries.map { it.value },
		)
	}

	@Test
	fun `구역과 미디어 상태 공통 타입을 생성한다`() {
		assertEquals(
			listOf("KITCHEN", "LIVING_ROOM", "BATHROOM", "UNKNOWN"),
			Zone.entries.map { it.value },
		)
		assertEquals(
			listOf("NO_MEDIA", "UPLOADING", "QUEUED", "ANALYZING", "COMPLETED", "PARTIAL_FAILED", "FAILED"),
			ZoneAnalysisStatus.entries.map { it.value },
		)
		assertEquals(listOf("PENDING", "UPLOADING", "UPLOADED", "FAILED"), MediaUploadStatus.entries.map { it.value })
		assertEquals(
			listOf("NOT_REQUESTED", "QUEUED", "ANALYZING", "COMPLETED", "FAILED"),
			MediaAnalysisStatus.entries.map { it.value },
		)
		assertEquals(listOf("META_GLASS", "ANDROID_CAMERA"), CaptureSource.entries.map { it.value })
		assertEquals(
			listOf("DURING_RECORDING_CAPTURE", "POST_RECORDING_EXTRACTION"),
			FrameOrigin.entries.map { it.value },
		)
	}

	@Test
	fun `JPEG 등록 요청은 초기 구역을 반드시 포함한다`() {
		val request = CreateMediaUploadRequest(
			clientMediaId = UUID.randomUUID(),
			zone = Zone.UNKNOWN,
			contentType = CreateMediaUploadRequest.ContentType.imageSlashJpeg,
			fileSize = 123,
			width = 640,
			height = 480,
			sourceVideoId = UUID.randomUUID(),
			sourceVideoOffsetMs = 3_000,
			frameOrigin = CreateMediaUploadRequest.FrameOrigin.POST_RECORDING_EXTRACTION,
			captureSource = CaptureSource.META_GLASS,
			capturedAt = OffsetDateTime.parse("2026-08-19T10:00:00+09:00"),
		)

		assertTrue(validator.validate(request).isEmpty())
		assertEquals(Zone.UNKNOWN, request.zone)
	}

	@Test
	fun `관찰과 리포트 상태 공통 타입을 생성한다`() {
		assertEquals(listOf("ACTIVE", "VIEWED", "DISMISSED"), ObservationStatus.entries.map { it.value })
		assertEquals(13, ObservationType.entries.size)
		assertEquals("CRACK_CHECK_NEEDED", ObservationType.entries.first().value)
		assertEquals("OTHER_CHECK_NEEDED", ObservationType.entries.last().value)
		assertEquals(
			listOf("NOT_REQUESTED", "WAITING_FOR_ANALYSIS", "GENERATING", "COMPLETED", "PARTIAL_COMPLETED", "FAILED"),
			ReportStatus.entries.map { it.value },
		)
	}

	@Test
	fun `확정된 미디어 관찰 리포트 계약과 폐기된 계약을 구분한다`() {
		val specification = File("../shared-types/openapi/openapi.yaml").readText()

		assertFalse(specification.contains("/checklist"))
		assertFalse(specification.contains("/frames/"))
		assertTrue(specification.contains("/inspections/{inspectionId}/media/upload-requests"))
		assertTrue(specification.contains("/media/{mediaId}/upload-complete"))
		assertTrue(specification.contains("/media/{mediaId}/upload-retry"))
		assertTrue(specification.contains("/inspections/{inspectionId}/media/finalize"))
		assertTrue(specification.contains("/inspections/{inspectionId}/observations"))
		assertTrue(specification.contains("/observations/{observationId}"))
		assertTrue(specification.contains("/properties/{propertyId}/reports"))
		assertTrue(specification.contains("/inspections/{inspectionId}/report"))
		assertTrue(specification.contains("coordinateSystem:"))
		assertTrue(specification.contains("PIXEL_XYXY"))
		assertTrue(specification.contains("scoreIsProvisional"))
		assertTrue(specification.contains("totalMediaCount"))
		assertTrue(specification.contains("ReportRepresentativePhoto"))
		assertTrue(specification.contains("representativePhotos"))
		assertTrue(specification.contains("maxItems: 20"))
		assertTrue(specification.contains("maximum: 2097152"))
		assertFalse(specification.contains("maximum: 1048576"))
		assertFalse(specification.contains("/analyses/"))
		assertFalse(specification.contains("/detections/"))
		assertFalse(specification.contains("checklistItemId"))
		assertFalse(specification.contains("CreateFrameUploadRequest"))
	}

	@Test
	fun `사용자가 입력하는 매물 정보를 생성 타입에 포함한다`() {
		val request = CreatePropertyRequest(
			name = "역세권 원룸",
			addressSummary = "서울시 ○○구",
			depositAmount = 10_000_000,
			monthlyRentAmount = 650_000,
			maintenanceFeeAmount = 70_000,
			areaSquareMeters = 18.5,
			floor = "2층",
			options = setOf("에어컨", "냉장고", "세탁기"),
			brokerContact = "○○부동산 02-0000-0000",
		)

		assertTrue(validator.validate(request).isEmpty())
		assertEquals(10_000_000L, request.depositAmount)
		assertEquals(650_000L, request.monthlyRentAmount)
		assertEquals(70_000L, request.maintenanceFeeAmount)
		assertEquals(18.5, request.areaSquareMeters)
		assertEquals("2층", request.floor)
		assertEquals(setOf("에어컨", "냉장고", "세탁기"), request.options)
		assertEquals("○○부동산 02-0000-0000", request.brokerContact)
	}

	@Test
	fun `매물 금액과 면적은 음수나 영을 허용하지 않는다`() {
		val request = CreatePropertyRequest(
			name = "검증용 매물",
			depositAmount = -1,
			monthlyRentAmount = -1,
			maintenanceFeeAmount = -1,
			areaSquareMeters = 0.0,
		)

		val invalidFields = validator.validate(request)
			.map { it.propertyPath.toString() }
			.toSet()

		assertEquals(
			setOf("depositAmount", "monthlyRentAmount", "maintenanceFeeAmount", "areaSquareMeters"),
			invalidFields,
		)
	}
}
