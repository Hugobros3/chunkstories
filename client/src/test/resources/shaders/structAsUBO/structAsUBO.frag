#version 450

in vec3 fragColor;
out vec4 outColor;

#include struct xyz.chunkstories.client.graphics.shaders.TestStructure
uniform TestStructure testUBO;

void main() {
    outColor = vec4(fragColor, testUBO.floater);
}