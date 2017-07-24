#version 150 core
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D depthBuffer;
uniform sampler2D metaBuffer;
uniform sampler2D shadedBuffer;
uniform sampler2D normalBuffer;

//Reflections stuff
uniform samplerCube environmentCubemap;

//Passed variables
in vec2 screenCoord;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;

uniform sampler2D lightColors;
uniform sampler2D blockLightmap;
uniform sampler2D ssaoBuffer;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;
uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;
uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;
uniform vec3 camPos;

//Shadow mapping
uniform float shadowVisiblity; // Used for night transitions, hides shadows
uniform sampler2DShadow shadowMap;
uniform mat4 shadowMatrix;

uniform float dayTime;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

//Gamma constants
<include ../lib/gamma.glsl>

uniform vec3 shadowColor;
uniform vec3 sunColor;

out vec4 fragColor;

<include ../sky/sky.glsl>
<include ../lib/transformations.glsl>
<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>
<include ../lib/ssr.glsl>

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer);
	
	vec4 normalBufferData = texture(normalBuffer, screenCoord);
	vec4 pixelMeta = texture(metaBuffer, screenCoord);
	
	vec3 pixelNormal = decodeNormal(normalBufferData);
	float spec = pow(normalBufferData.z, 1.0);
	
	//Discard fragments using alpha
	if(texture(shadedBuffer, screenCoord).a > 0.0 && spec > 0.0)
	{
		fragColor = computeReflectedPixel(depthBuffer, shadedBuffer, environmentCubemap, screenCoord, cameraSpacePosition.xyz, pixelNormal, pixelMeta.y);
		//shadingColor = vec4(1.0, 0.0, 0.0, 0.0);
	}
	else
		discard;
	
	// Apply fog
	/*vec3 sum = (cameraSpacePosition.xyz);
	float dist = length(sum)-fogStartDistance;
	float fogFactor = (dist) / (fogEndDistance-fogStartDistance);
	float fogIntensity = clamp(fogFactor, 0.0, 1.0);
	
	vec3 fogColor = getSkyColorWOSun(time, normalize(((modelViewMatrixInv * cameraSpacePosition).xyz - camPos).xyz));
	
	fragColor = mix(shadingColor, vec4(fogColor,shadingColor.a), fogIntensity);*/
}
