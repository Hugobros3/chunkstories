#version 130
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
const float gammaInv = 0.45454545454;

const vec4 waterColor = vec4(0.2, 0.4, 0.45, 1.0);

vec3 convertCameraSpaceToScreenSpace(vec3 cameraSpace) {
    vec4 clipSpace = projectionMatrix * vec4(cameraSpace, 1.0);
    vec3 NDCSpace = clipSpace.xyz / clipSpace.w;
    vec3 screenSpace = 0.5 * NDCSpace + 0.5;
		 screenSpace.z = 0.1f;
    return screenSpace;
}

vec3 unprojectPixel(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0-1.0, texture2D(depthBuffer, co, 0.0).x * 2.0 - 1.0), 1.0);
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

vec4 convertScreenSpaceToWorldSpace(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0-1.0, texture2D(depthBuffer, co, 0.0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}
// note: valve edition
//       from http://alex.vlachos.com/graphics/Alex_Vlachos_Advanced_VR_Rendering_GDC2015.pdf
// note: input in pixels (ie not normalized uv)
vec3 ScreenSpaceDither( vec2 vScreenPos )
{
	// Iestyn's RGB dither (7 asm instructions) from Portal 2 X360, slightly modified for VR
	//vec3 vDither = vec3( dot( vec2( 171.0, 231.0 ), vScreenPos.xy + iGlobalTime ) );
    vec3 vDither = vec3( dot( vec2( 171.0, 231.0 ), vScreenPos.xy ) );
    vDither.rgb = fract( vDither.rgb / vec3( 103.0, 71.0, 97.0 ) );
    return vDither.rgb / 255.0; //note: looks better without 0.375...

    //note: not sure why the 0.5-offset is there...
    //vDither.rgb = fract( vDither.rgb / vec3( 103.0, 71.0, 97.0 ) ) - vec3( 0.5, 0.5, 0.5 );
	//return (vDither.rgb / 255.0) * 0.375;
}
void main() {
	vec2 finalCoords = f_texcoord;
	
	// Water coordinates distorsion
	finalCoords.x += underwater*sin(finalCoords.x * 50.0 + finalCoords.y * 60.0 + time * 1.0) / viewWidth * 5.0;
	finalCoords.y += underwater*cos(finalCoords.y * 60.0 + time * 1.0) / viewHeight * 2.0;
	
	// Sampling
	vec4 compositeColor = texture2DLod(shadedBuffer, finalCoords, 0.0);
	
	// etc
	
	compositeColor = mix(compositeColor, compositeColor * waterColor, underwater);
	
	compositeColor *= apertureModifier;
	<ifdef doBloom>
	compositeColor.rgb += texture2D(bloomBuffer, finalCoords).rgb;
	//finalLight *= clamp(lum-0.8, 0.0, 10.0);
	<endif doBloom>
	
	//compositeColor.rgb = compositeColor.rgb / (compositeColor.rgb + vec3(1.0)) ;
	compositeColor.rgb = pow(compositeColor.rgb, vec3(gammaInv));
	
	vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(finalCoords);
	compositeColor = mix(compositeColor, vec4(0.0), underwater * clamp(length(cameraSpacePosition) / 32.0, 0.0, 1.0));
	vec4 pixelNormal = texture2D(normalBuffer, finalCoords);
	pixelNormal.rgb = pixelNormal.rgb * 2.0 - vec3(1.0);
    vec3 cameraSpaceViewDir = normalize(cameraSpacePosition.xyz);
    vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal.xyz));
	vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
	
	//Debug water surface (we don't want negative -dark- values for reflection)
	
	//compositeColor.rgb = vec3(0.0, normSkyDirection.y * 40, 0.0);
    
	vec3 its2 = compositeColor.rgb;
    vec3 rnd2 = ScreenSpaceDither( gl_FragCoord.xy );
    compositeColor.rgb = its2 + rnd2.x;
	
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
			shit = texture2DLod(debugBuffer, sampleCoords, 4.0);
			shit = vec4(1.0, 0.5, 0.0, 1.0) * texture2D(normalBuffer, sampleCoords).w;
			shit.yz += texture2D(metaBuffer, sampleCoords).xy;
			<ifdef dynamicGrass>
			
			shit = vec4(1.0, 1.0, 1.0, 1.0) * texture2D(bloomBuffer, sampleCoords).x * 1.0;
			//shit = texture2DLod(debugBuffer, sampleCoords, 80.0);
			<endif dynamicGrass>
		}
	}
	shit.a = 1.0;
	return shit;
}
