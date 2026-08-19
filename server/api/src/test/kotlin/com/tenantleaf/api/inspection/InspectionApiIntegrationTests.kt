package com.tenantleaf.api.inspection

import com.tenantleaf.api.property.DemoUserContext
import com.tenantleaf.api.property.PropertyEntity
import com.tenantleaf.api.property.PropertyRepository
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class InspectionApiIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val inspectionRepository: InspectionRepository,
    @Autowired private val propertyRepository: PropertyRepository,
) {
    @BeforeEach
    fun cleanDatabase() {
        inspectionRepository.deleteAll()
        propertyRepository.deleteAll()
    }

    @Test
    fun `임장을 생성하고 목록과 상세를 조회한 뒤 종료한다`() {
        val propertyId = createProperty(DemoUserContext.DEMO_USER_ID)

        val createResult = mockMvc.perform(post("/api/v1/properties/{propertyId}/inspections", propertyId))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.analysisStatus").value("NOT_STARTED"))
            .andExpect(jsonPath("$.startedAt").isNotEmpty)
            .andReturn()

        val inspectionId = objectMapper.readTree(createResult.response.contentAsByteArray)["id"].stringValue()

        mockMvc.perform(get("/api/v1/properties/{propertyId}/inspections", propertyId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items", hasSize<Any>(1)))
            .andExpect(jsonPath("$.items[0].id").value(inspectionId))

        mockMvc.perform(get("/api/v1/inspections/{inspectionId}", inspectionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))

        mockMvc.perform(
            patch("/api/v1/inspections/{inspectionId}/status", inspectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ENDED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ENDED"))
            .andExpect(jsonPath("$.endedAt").isNotEmpty)
            .andExpect(jsonPath("$.cancelledAt").doesNotExist())

        mockMvc.perform(
            patch("/api/v1/inspections/{inspectionId}/status", inspectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELLED"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"))
    }

    @Test
    fun `진행 중인 임장을 취소한다`() {
        val propertyId = createProperty(DemoUserContext.DEMO_USER_ID)
        val inspectionId = createInspection(propertyId, DemoUserContext.DEMO_USER_ID).id

        mockMvc.perform(
            patch("/api/v1/inspections/{inspectionId}/status", inspectionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELLED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.cancelledAt").isNotEmpty)
            .andExpect(jsonPath("$.endedAt").doesNotExist())
    }

    @Test
    fun `없는 매물과 다른 사용자의 임장을 노출하지 않는다`() {
        mockMvc.perform(post("/api/v1/properties/{propertyId}/inspections", UUID.randomUUID()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROPERTY_NOT_FOUND"))

        val otherOwner = UUID.randomUUID()
        val otherPropertyId = createProperty(otherOwner)
        val otherInspectionId = createInspection(otherPropertyId, otherOwner).id

        mockMvc.perform(post("/api/v1/properties/{propertyId}/inspections", otherPropertyId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("PROPERTY_NOT_FOUND"))

        mockMvc.perform(get("/api/v1/inspections/{inspectionId}", otherInspectionId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("INSPECTION_NOT_FOUND"))
    }

    @Test
    fun `임장이 존재하는 매물 삭제는 안전하게 거부한다`() {
        val propertyId = createProperty(DemoUserContext.DEMO_USER_ID)
        createInspection(propertyId, DemoUserContext.DEMO_USER_ID)

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/properties/{propertyId}", propertyId))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"))
    }

    private fun createProperty(ownerId: UUID): UUID {
        val now = OffsetDateTime.now()
        return propertyRepository.save(
            PropertyEntity(
                id = UUID.randomUUID(),
                ownerId = ownerId,
                name = "테스트 매물",
                createdAt = now,
                updatedAt = now,
            ),
        ).id
    }

    private fun createInspection(propertyId: UUID, ownerId: UUID): InspectionEntity {
        val now = OffsetDateTime.now()
        return inspectionRepository.save(
            InspectionEntity(
                id = UUID.randomUUID(),
                propertyId = propertyId,
                ownerId = ownerId,
                status = InspectionLifecycleStatus.IN_PROGRESS,
                analysisStatus = InspectionAggregateStatus.NOT_STARTED,
                startedAt = now,
                createdAt = now,
            ),
        )
    }
}
