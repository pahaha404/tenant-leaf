package com.tenantleaf.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals

@SpringBootTest
class ApiApplicationTests(
	@Autowired private val jdbcTemplate: JdbcTemplate,
) {

	@Test
	fun contextLoads() {
	}

	@Test
	fun flywayMigrationCreatesSchemaMarker() {
		val markerTable = jdbcTemplate.queryForObject(
			"SELECT to_regclass('public.api_schema_marker')",
			String::class.java,
		)

		assertEquals("api_schema_marker", markerTable)
	}

}
