package com.tenantleaf.api

import com.tenantleaf.api.generated.api.PropertiesApi
import com.tenantleaf.api.generated.model.ChecklistStatus
import com.tenantleaf.api.generated.model.CreateFrameUploadRequest
import com.tenantleaf.api.generated.model.CreatePropertyRequest
import com.tenantleaf.api.generated.model.DetectionLabel
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiGeneratedContractTests {

	private val validator = Validation.buildDefaultValidatorFactory().validator

	@Test
	fun `API 인터페이스는 공통 기본 경로를 사용한다`() {
		assertEquals("/api/v1", PropertiesApi.BASE_PATH)
	}

	@Test
	fun `체크리스트 상태 네 가지를 생성한다`() {
		assertEquals(
			listOf("unchecked", "normal", "needs_review", "not_applicable"),
			ChecklistStatus.entries.map { it.value },
		)
	}

	@Test
	fun `AI 탐지 라벨 열두 가지를 생성한다`() {
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
			),
			DetectionLabel.entries.map { it.value },
		)
	}

	@Test
	fun `프레임 크기 제한을 생성된 요청 타입에서 검증한다`() {
		val request = CreateFrameUploadRequest(
			deviceId = UUID.randomUUID(),
			fileName = "frame.jpg",
			contentType = CreateFrameUploadRequest.ContentType.imageSlashJpeg,
			contentLength = 1_048_577,
			width = 1920,
			height = 1080,
			capturedAt = OffsetDateTime.now(),
		)

		val violations = validator.validate(request)

		assertTrue(violations.any { it.propertyPath.toString() == "contentLength" })
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
