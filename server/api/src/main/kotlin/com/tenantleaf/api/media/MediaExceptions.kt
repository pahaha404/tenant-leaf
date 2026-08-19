package com.tenantleaf.api.media

class MediaNotFoundException : RuntimeException()
class MediaStateException : RuntimeException()
class MediaValidationException(val field: String, val reason: String) : RuntimeException()
class MediaFileTooLargeException : RuntimeException()
class UnsupportedMediaTypeException : RuntimeException()
class ClientMediaIdConflictException : RuntimeException()
class IdempotencyKeyConflictException : RuntimeException()
class ObjectStorageUnavailableException(cause: Throwable? = null) : RuntimeException(cause)
