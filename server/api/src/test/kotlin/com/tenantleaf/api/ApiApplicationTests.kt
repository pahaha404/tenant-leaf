package com.tenantleaf.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertTrue

@SpringBootTest
class ApiApplicationTests(
	@Autowired private val jdbcTemplate: JdbcTemplate,
	@Value("\${spring.flyway.default-schema:public}") private val flywaySchema: String,
) {

	@Test
	fun contextLoads() {
	}

	@Test
	fun flywayMigrationCreatesSchemaMarker() {
		val markerTableExists = jdbcTemplate.queryForObject(
			"""
			SELECT EXISTS (
				SELECT 1
				FROM information_schema.tables
				WHERE table_schema = ?
				  AND table_name = 'api_schema_marker'
			)
			""".trimIndent(),
			Boolean::class.java,
			flywaySchema,
		) ?: false

		assertTrue(markerTableExists)
	}

}
