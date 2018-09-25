package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.vulkan.VK10.*
import java.lang.Exception

fun Int.physicalDeviceTypeName() = when(this) {
    VK_PHYSICAL_DEVICE_TYPE_OTHER -> "Other"
    VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "Integrated GPU"
    VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "Discrete GPU"
    VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "Virtualized GPU"
    VK_PHYSICAL_DEVICE_TYPE_CPU -> "Software renderer"
    else -> throw Exception("Unrecognized physical device type !")
}