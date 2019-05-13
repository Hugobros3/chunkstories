#version 450

//Passed variables
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 colorOut;

//Common camera matrices & uniforms
#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

uniform samplerCube background;

void main()
{
	//colorOut = vec4(clamp(eyeDirection, 0.0, 1.0), 1.0);
	colorOut = texture(background, normalize(vec3(eyeDirection.x, -eyeDirection.y, eyeDirection.z)));
}
