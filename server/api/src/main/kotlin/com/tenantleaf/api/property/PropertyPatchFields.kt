package com.tenantleaf.api.property

import com.tenantleaf.api.generated.model.UpdatePropertyRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.lang.reflect.Type

@Component
class PropertyPatchFields {
    fun current(): Set<String> {
        val value = RequestContextHolder.currentRequestAttributes()
            .getAttribute(ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST)
        return (value as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
    }

    companion object {
        const val ATTRIBUTE_NAME = "propertyPatchFields"
    }
}

@ControllerAdvice
class PropertyPatchRequestBodyAdvice(
    private val objectMapper: ObjectMapper,
) : RequestBodyAdviceAdapter() {
    override fun supports(
        methodParameter: MethodParameter,
        targetType: Type,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = targetType == UpdatePropertyRequest::class.java

    override fun beforeBodyRead(
        inputMessage: HttpInputMessage,
        parameter: MethodParameter,
        targetType: Type,
        converterType: Class<out HttpMessageConverter<*>>,
    ): HttpInputMessage {
        val body = inputMessage.body.readAllBytes()
        val root = objectMapper.readTree(body)
        val fields = if (root.isObject) root.propertyNames().toSet() else emptySet()
        RequestContextHolder.currentRequestAttributes()
            .setAttribute(PropertyPatchFields.ATTRIBUTE_NAME, fields, RequestAttributes.SCOPE_REQUEST)

        return object : HttpInputMessage {
            override fun getHeaders() = inputMessage.headers
            override fun getBody() = ByteArrayInputStream(body)
        }
    }
}
