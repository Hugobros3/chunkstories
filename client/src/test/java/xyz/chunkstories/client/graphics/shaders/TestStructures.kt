package xyz.chunkstories.client.graphics.shaders

import org.joml.Matrix3f
import xyz.chunkstories.api.graphics.structs.InterfaceBlock

class TestSubStructure : InterfaceBlock {
    val a : Int = 8
    val b = -1
}

class TestStructure : InterfaceBlock {
    var floater : Float = 5.0F
    var inter : Int = 1
    var matrix : Matrix3f = Matrix3f()
    var values = FloatArray(5)
    val inc = arrayOf(TestSubStructure(), TestSubStructure())
    val nik = 9999999.0F
}