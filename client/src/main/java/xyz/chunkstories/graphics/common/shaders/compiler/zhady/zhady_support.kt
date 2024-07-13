import de.unisaarland.zhady.shady;
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.shaders.GLSLGraphicsProgram
import java.nio.ByteBuffer

fun load_zhady() {

}

data class CompiledSpvGraphicsPipeline(val stages: Map<ShaderStage, ByteBuffer>)

fun generateSpirV(program: GLSLGraphicsProgram, spv13: Boolean): CompiledSpvGraphicsPipeline {
    TODO()
}