//#version 120
uniform sampler2D shadedBuffer;

uniform sampler2D albedoBuffer;
uniform sampler2D depthBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D metaBuffer;
uniform sampler2D debugBuffer;

uniform sampler2D shadowMap;

uniform sampler2D bloomBuffer;
uniform sampler2D ssaoBuffer;

uniform samplerCube skybox;

varying vec2 f_texcoord;

varying vec2 scaledPixel;

uniform float viewWidth;
uniform float viewHeight;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 sunPos;

uniform float time;
uniform float underwater;

uniform float apertureModifier;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

const vec4 waterColor = vec4(51/255.0, 104/255.0, 110/255.0, 1.0);

vec3 convertCameraSpaceToScreenSpace(vec3 cameraSpace) {
    vec4 clipSpace = projectionMatrix * vec4(cameraSpace, 1.0);
    vec3 NDCSpace = clipSpace.xyz / clipSpace.w;
    vec3 screenSpace = 0.5 * NDCSpace + 0.5;
		 screenSpace.z = 0.1f;
    return screenSpace;
}

vec3 unprojectPixel(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2-1, texture2D(depthBuffer, co, 0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition.xyz;
}

float linearizeDepth(float z)
{
  float n = 0.1; // camera z near
  float f = 300.0; // camera z far
  return (2.0 * n) / (f + n - z * (f - n));	
}

vec4 getDebugShit(vec2 coords);

void main() {
	vec2 finalCoords = f_texcoord;
	
	// Water coordinates distorsion
	finalCoords.x += underwater*sin(finalCoords.x * 50 + finalCoords.y * 60 + time * 1.0) / viewWidth * 5.0;
	finalCoords.y += underwater*cos(finalCoords.y * 60 + time * 1.0) / viewHeight * 2.0;
	
	// Sampling
	vec4 compositeColor = texture2D(shadedBuffer, finalCoords);
	
	// Do reflections here
	
	// etc
	
	compositeColor = mix(compositeColor, compositeColor * waterColor, underwater * 0.5);
	
	compositeColor *= apertureModifier;
	<ifdef doBloom>
	compositeColor.rgb += texture2D(bloomBuffer, finalCoords).rgb;
	<endif doBloom>
	
	compositeColor.rgb = pow(compositeColor.rgb, vec3(gammaInv));
	
	gl_FragColor = compositeColor;
	
	<ifdef debugGBuffers>
	gl_FragColor = getDebugShit(f_texcoord);
	<endif debugGBuffers>
}


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
			shit = pow(texture2D(shadedBuffer, sampleCoords, 1), vec4(gammaInv));
		else
			shit = texture2D(normalBuffer, sampleCoords);
	}
	else
	{
		if(coords.y > 0.5)
		{
			shit = texture2D(albedoBuffer, sampleCoords);
			//if(shit.a == 0)
			shit += (1-shit.a) * vec4(1.0, 0.0, 1.0, 1.0);
		}
		else
		{
			shit = texture2DLod(debugBuffer, sampleCoords, 4);
			shit = vec4(1.0, 0.5, 0.0, 1.0) * texture2D(normalBuffer, sampleCoords).w;
			shit.yz += texture2D(metaBuffer, sampleCoords).xy;
			<ifdef dynamicGrass>
			shit = texture2DLod(shadedBuffer, sampleCoords, 80);
			<endif dynamicGrass>
		}
	}
	shit.a = 1.0;
	return shit;
}
