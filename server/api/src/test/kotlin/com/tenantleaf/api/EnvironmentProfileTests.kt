package com.tenantleaf.api

import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertySourcesPropertyResolver
import org.springframework.core.io.ClassPathResource
import org.springframework.boot.env.YamlPropertySourceLoader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentProfileTests {
    @Test
    fun productionProfileRequiresDeploymentOwnedInfrastructureValues() {
        val production = YamlPropertySourceLoader()
            .load("production", ClassPathResource("application-prod.yml"))
            .single()
        val required = mapOf(
            "spring.datasource.url" to "DATABASE_URL",
            "spring.datasource.username" to "POSTGRES_USER",
            "spring.datasource.password" to "POSTGRES_PASSWORD",
            "app.object-storage.endpoint" to "OBJECT_STORAGE_ENDPOINT",
            "app.object-storage.public-endpoint" to "OBJECT_STORAGE_PUBLIC_ENDPOINT",
            "app.object-storage.access-key" to "OBJECT_STORAGE_ACCESS_KEY",
            "app.object-storage.secret-key" to "OBJECT_STORAGE_SECRET_KEY",
            "app.object-storage.bucket" to "OBJECT_STORAGE_BUCKET",
        )

        val missingValues = PropertySourcesPropertyResolver(
            MutablePropertySources().apply { addLast(production) },
        )
        required.keys.forEach { key ->
            assertFailsWith<IllegalArgumentException> { missingValues.getRequiredProperty(key) }
        }

        val deploymentValues = required.values.associateWith { "configured-$it" }
        val configured = PropertySourcesPropertyResolver(
            MutablePropertySources().apply {
                addFirst(MapPropertySource("deployment", deploymentValues))
                addLast(production)
            },
        )
        required.forEach { (key, environmentVariable) ->
            assertEquals("configured-$environmentVariable", configured.getRequiredProperty(key))
        }
    }
}
