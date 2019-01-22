#version 450
#define VULKAN 100

in vec3 fragColor;
out vec4 outColor;

void main() {
    outColor = vtexture2D(1, gl_FragCoord.xy);
}