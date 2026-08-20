package com.tenantleaf.api.property

import com.tenantleaf.api.inspection.InspectionRepository
import com.tenantleaf.api.media.ApiIdempotencyRecordRepository
import com.tenantleaf.api.media.MediaRepository
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class PropertyApiIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val inspectionRepository: InspectionRepository,
    @Autowired private val repository: PropertyRepository,
    @Autowired private val mediaRepository: MediaRepository,
    @Autowired private val idempotencyRepository: ApiIdempotencyRecordRepository,
) {
    @BeforeEach
    fun cleanDatabase() {
        idempotencyRepository.deleteAll()
        mediaRepository.deleteAll()
        inspectionRepository.deleteAll()
        repository.deleteAll()
    }

    @Test
    fun `매물을 등록 조회 수정 삭제한다`() {
        val createResult = mockMvc.perform(
            post("/api/v1/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "신림역 원룸",
                      "addressSummary": "서울 관악구",
                      "depositAmount": 10000000,
                      "monthlyRentAmount": 650000,
                      "maintenanceFeeAmount": 70000,
                      "areaSquareMeters": 19.83471,
                      "floor": "3층",
                      "options": ["에어컨", "세탁기"],
                      "brokerContact": "샘플 부동산 02-0000-0000",
                      "note": "채광 확인 필요"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("신림역 원룸"))
            .andExpect(jsonPath("$.depositAmount").value(10000000))
            .andExpect(jsonPath("$.options", hasSize<Any>(2)))
            .andReturn()

        val propertyId = objectMapper.readTree(createResult.response.contentAsByteArray)["id"].stringValue()

        mockMvc.perform(get("/api/v1/properties"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].id").value(propertyId))

        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.addressSummary").value("서울 관악구"))

        mockMvc.perform(
            patch("/api/v1/properties/{propertyId}", propertyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"monthlyRentAmount":680000,"note":"수압도 확인"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.monthlyRentAmount").value(680000))
            .andExpect(jsonPath("$.addressSummary").value("서울 관악구"))
            .andExpect(jsonPath("$.note").value("수압도 확인"))

        mockMvc.perform(
            patch("/api/v1/properties/{propertyId}", propertyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"addressSummary":null}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.addressSummary").doesNotExist())
            .andExpect(jsonPath("$.note").value("수압도 확인"))

        mockMvc.perform(delete("/api/v1/properties/{propertyId}", propertyId))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROPERTY_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test
    fun `빈 수정 요청과 공백 이름을 거부한다`() {
        val propertyId = createPropertyDirectly(ownerId = DemoUserContext.DEMO_USER_ID)

        mockMvc.perform(
            patch("/api/v1/properties/{propertyId}", propertyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        mockMvc.perform(
            post("/api/v1/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))

        mockMvc.perform(
            patch("/api/v1/properties/{propertyId}", propertyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"floor":"   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors[0].field").value("floor"))
    }

    @Test
    fun `다른 사용자의 매물은 조회할 수 없다`() {
        val propertyId = createPropertyDirectly(ownerId = UUID.randomUUID())

        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROPERTY_NOT_FOUND"))
    }

    private fun createPropertyDirectly(ownerId: UUID): UUID {
        val now = OffsetDateTime.now()
        val entity = repository.save(
            PropertyEntity(
                id = UUID.randomUUID(),
                ownerId = ownerId,
                name = "테스트 매물",
                areaSquareMeters = BigDecimal("20.000000"),
                createdAt = now,
                updatedAt = now,
            ),
        )
        return entity.id
    }
}
