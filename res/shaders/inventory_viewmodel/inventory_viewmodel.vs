#version 330
//Entry attributes
attribute vec3 vertexIn;
attribute vec2 texCoordIn;
attribute vec3 normalIn;

in vec2 texCoord;

//uniform vec4 colorIn;

in vec4 interpolatedColor;

uniform mat4 transformation;

uniform vec2 screenSize;
uniform vec2 dekal;
uniform float scaling;

uniform vec2 texBase;
uniform vec2 texScaling;

void main(){
	//interpolatedColor = colorIn;
	
	texCoord = texBase + texCoordIn * texScaling;
	
	vec4 transformed = transformation * vec4(vertexIn.xyz, 1.0);
	
	transformed.xy *= scaling;
	transformed.xy += dekal;
	transformed.xy /= screenSize;
	transformed.xy = 2.0 * transformed.xy - vec2(1.0);
	
	float opacityModified = 0.0;
	vec3 shadingDir = normalIn;
	opacityModified += 0.25 * abs(dot(vec3(1.0, 0.0, 0.0), shadingDir));
	opacityModified += 0.45 * abs(dot(vec3(0.0, 0.0, 1.0), shadingDir));
	opacityModified += 0.6 * clamp(dot(vec3(0.0, -1.0, 0.0), shadingDir), 0.0, 1.0);
	
	interpolatedColor = vec4(vec3(1.0)*(1.0-opacityModified), 1.0);
	
	transformed.z *= 0.125;
	
	gl_Position = transformed;
}