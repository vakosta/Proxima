package ru.hse.hseditor.domain.app

import io.reflekt.Reflekt
import kotlin.reflect.KClass

class ServiceContainer {

    val globalServiceMap = mutableMapOf<KClass<*>, Any>()

    init {
        val serviceTypes = Reflekt.classes().withAnnotations<Service>().toSet()
        for (serviceType in serviceTypes) {
            serviceType.constructors.firstOrNull { it.parameters.isEmpty() }?.call()
                ?.let { globalServiceMap[serviceType] = it }
        }
    }
}