import de.unisaarland.zhady.shady;
import de.unisaarland.zhady.vcc
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.graphics.common.shaders.GLSLGraphicsProgram
import java.nio.ByteBuffer

fun load_zhady() {
    try {
        println(System.getenv("PATH"))
        System.loadLibrary("zhady_shared_lib")
    } catch(e: UnsatisfiedLinkError) {
        e.message
    }

    vcc.vcc_check_clang()
}
