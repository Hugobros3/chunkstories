#version 150
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D depthBuffer;
uniform sampler2D normalTexture;
uniform sampler2D noiseTexture;

in vec2 screenCoord;

//SSAO kernel values
const int KERNEL_SIZE = 16;
uniform vec3[KERNEL_SIZE] ssaoKernel;

//Common camera matrices & uniforms
uniform mat4 modelViewMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform float viewWidth;
uniform float viewHeight;

uniform int kernelsPerFragment;

<include ../lib/transformations.glsl>

out vec4 fragColor;

float isTooFar(float z)
{
  float n = 0.1; // camera z near
  float f = 3000.0; // camera z far
  float dist = (2.0 * n) / (f + n - z * (f - n));
  dist *= f;
  return clamp(1-(dist-1000)*1.0, 0.0, 1.0);
}

const float radius = 1.0;

void main()
{
	vec2 noiseScale = vec2(viewWidth/4.0, viewHeight/4.0);
	
	vec3 worldspace = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer).xyz;
	
	vec3 color = vec3(1.0);
	vec3 normal = texture(normalTexture, screenCoord).xyz * 2.0 - 1.0;
	
	vec3 randomVec = texture(noiseTexture, screenCoord * noiseScale).xyz * 2 - 1.0; 
	
	vec3 tangent = normalize(randomVec - normal * dot(randomVec, normal));
	vec3 bitangent = cross(normal, tangent);
	mat3 TBN = mat3(tangent, bitangent, normal);  
	
	float occlusion = 0.0;
	for(int i = 0; i < kernelsPerFragment; i+=KERNEL_SIZE/kernelsPerFragment)
	{
		// get sample position
		vec3 sample = TBN * ssaoKernel[i]; // From tangent to view-space
		sample = worldspace + sample * radius; 
		
		vec3 offset = convertCameraSpaceToScreenSpace(sample);
		
		vec3 worldspace2 = convertScreenSpaceToCameraSpace(offset.xy, depthBuffer).xyz;
		
		float rangeCheck = smoothstep(0.0, 1.0, radius * 0.2 / abs(sample.z - worldspace2.z));
		occlusion += (worldspace2.z >= sample.z ? 1.0 : 0.0) * rangeCheck; 
		
		//float sampleDepth = -texture(depthBuffer, offset.xy).w;
		//occlusion += (sampleDepth >= sample.z ? 1.0 : 0.0);  
		
		//color = vec3(0.2, 0.5, 0.5) * linearizeDepth2(texture(depthBuffer, offset.xy).x);
	}
	
	occlusion/=kernelsPerFragment;
	//occlusion = 1;
	
	//color = vec3(0.8, 0.5, 0.5) * linearizeDepth2(texture(depthBuffer, screenCoord).x);
		
	fragColor = vec4(color*(1-occlusion * (isTooFar(texture(depthBuffer, screenCoord).x))), 1.0);//texture(comp_light, screenCoord);
}
