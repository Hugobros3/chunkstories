#version 450

in vec3 fragColor;
out vec4 outColor;

#using struct xyz.chunkstories.client.graphics.shaders.TestStructure
uniform TestStructure testUBO;

void main() {
    outColor = vec4(fragColor, testUBO.nik);
}