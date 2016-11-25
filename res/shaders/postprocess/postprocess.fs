#version 150 core
uniform sampler2D shadedBuffer;

uniform sampler2D albedoBuffer;
uniform sampler2D depthBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D metaBuffer;
uniform sampler2D debugBuffer;

uniform sampler2D shadowMap;

uniform sampler2D bloomBuffer;
uniform sampler2D ssaoBuffer;

uniform sampler2D pauseOverlayTexture;
uniform float pauseOverlayFade;

uniform samplerCube skybox;

in vec2 texCoord;
in vec2 pauseOverlayCoords;

uniform float viewWidth;
uniform float viewHeight;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float time;
uniform float underwater;

uniform float apertureModifier;
uniform vec2 screenViewportSize;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

const vec4 waterColor = vec4(0.2, 0.4, 0.45, 1.0);

<include ../lib/transformations.glsl>
/*<include fxaa.fs>*/
<include dither.glsl>

vec4 getDebugShit(vec2 coords);

out vec4 fragColor;

float poltergeist(vec2 coordinate, float seed)
{
    return fract(sin(dot(coordinate*seed, vec2(12.9898, 78.233)))*43758.5453);
}

void main() {
	vec2 finalCoords = texCoord;
	
	// Water coordinates distorsion
	finalCoords.x += underwater*sin(finalCoords.x * 50.0 + finalCoords.y * 60.0 + time * 1.0) / screenViewportSize.x * 5.0;
	finalCoords.y += underwater*cos(finalCoords.y * 60.0 + time * 1.0) / screenViewportSize.y * 2.0;
	
	// Sampling
	vec4 compositeColor = texture(shadedBuffer, finalCoords);
	
	// Tints pixels blue underwater
	compositeColor = mix(compositeColor, compositeColor * waterColor, underwater);
	
	compositeColor *= apertureModifier;
	
	//Applies bloom
	<ifdef doBloom>
	compositeColor.rgb += texture2D(bloomBuffer, finalCoords).rgb;
	<endif doBloom>
	
	//Gamma-corrects stuff
	compositeColor.rgb = pow(compositeColor.rgb, vec3(gammaInv));
	
	vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(finalCoords, depthBuffer);
	//Darkens further pixels underwater
	compositeColor = mix(compositeColor, vec4(0.0), underwater * clamp(length(cameraSpacePosition) / 32.0, 0.0, 1.0));
	
	//Dither the final pixel colour
	vec3 its2 = compositeColor.rgb;
    vec3 rnd2 = screenSpaceDither( gl_FragCoord.xy );
    compositeColor.rgb = its2 + rnd2.xyz;
	
	//Applies pause overlay
	vec3 overlayColor = texture2D(pauseOverlayTexture, pauseOverlayCoords).rgb;
	overlayColor = vec3(
	
	( mod(gl_FragCoord.x + gl_FragCoord.y, 2.0) * 0.45 + 0.55 )
	* 
	( poltergeist(gl_FragCoord.xy, time) * 0.15 + 0.85 )
	
	);
	compositeColor.rgb *= mix(vec3(1.0), overlayColor, clamp(pauseOverlayFade, 0.0, 1.0));
	
	//Ouputs
	fragColor = compositeColor;
	
	//Debug flag
	<ifdef debugGBuffers>
	fragColor = getDebugShit(texCoord);
	<endif debugGBuffers>
}

//Draws divided screen with debug buffers
vec4 getDebugShit(vec2 coords)
{
	vec2 sampleCoords = coords;
	sampleCoords.x = mod(sampleCoords.x, 0.5);
	sampleCoords.y = mod(sampleCoords.y, 0.5);
	sampleCoords *= 2.0;
	
	vec4 shit = vec4(0.0);
	if(coords.x > 0.5)
	{
		if(coords.y > 0.5)
			shit = pow(texture2D(shadedBuffer, sampleCoords, 0.0), vec4(gammaInv));
		else
			shit = texture2D(normalBuffer, sampleCoords);
	}
	else
	{
		if(coords.y > 0.5)
		{
			shit = texture2D(albedoBuffer, sampleCoords);
			//if(shit.a == 0)
			shit += (1.0-shit.a) * vec4(1.0, 0.0, 1.0, 1.0);
		}
		else
		{
			shit = texture2D(debugBuffer, sampleCoords).xyzw;
			//shit = vec4(1.0, 0.5, 0.0, 1.0) * texture2D(normalBuffer, sampleCoords).w;
			//shit.yz += texture2D(metaBuffer, sampleCoords).xy;
			<ifdef dynamicGrass>
			
			shit = vec4(1.0, 1.0, 1.0, 1.0) * texture2D(bloomBuffer, sampleCoords).x * 1.0;
			//shit = texture2DLod(debugBuffer, sampleCoords, 80.0);
			<endif dynamicGrass>
		}
	}
	shit.a = 1.0;
	return shit;
}
