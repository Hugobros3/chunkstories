#version 330
//Entry attributes
in vec4 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;
in vec4 normalIn;

out vec2 texcoord;
out vec2 worldLight;
out float fresnelTerm;
out float chunkFade;
out float rainWetness;
out float fogI;
out vec4 modelview;
out vec3 eye;

out vec3 inNormal;
out vec4 inVertex;
out vec4 colorPassed;

uniform float useColorIn;
uniform float useNormalIn;
uniform float isUsingInstancedData;
uniform sampler2D instancedDataSampler;

//Lighthing
uniform float sunIntensity;

uniform float time;
uniform vec3 camPos;

uniform float vegetation;
uniform float viewDistance;
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec2 worldLightIn;

uniform mat4 objectMatrix;
uniform mat3 objectMatrixNormal;

//Weather
uniform float wetness;

void main(){
	//Usual variable passing
	texcoord = texCoordIn;
	vec4 v = objectMatrix * vec4(vertexIn.xyz, 1.0);
	
	if(isUsingInstancedData > 0)
	{
		mat4 matrixInstanced = mat4(texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8, 32), (gl_InstanceID * 8) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 1, 32), (gl_InstanceID * 8 + 1) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 2, 32), (gl_InstanceID * 8 + 2) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 3, 32), (gl_InstanceID * 8 + 3) / 32), 0)
									);
	
		v = matrixInstanced * vec4(vertexIn.xyz, 1.0);
		
		inVertex = v;
		inNormal =  mat3(transpose(inverse(matrixInstanced))) * (normalIn).xyz;//(normalIn.xyz-0.5)*2.0;//normalIn;;
	}
	else
	{
		inVertex = v;
		inNormal = objectMatrixNormal * (normalIn).xyz;//(normalIn.xyz-0.5)*2.0;//normalIn;
	}
	
	fresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(v.xyz - camPos), vec3(inNormal)), 0.0, 1.0);
	
	colorPassed = colorIn;
	
	//Compute lightmap coords
	rainWetness = wetness;
	
	if(isUsingInstancedData > 0)
	{
		worldLight = vec2(texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 4, 32), (gl_InstanceID * 8 + 5) / 32), 0).xy / 15.0);
	}
	else
		worldLight = vec2(worldLightIn / 15.0);
	
	//Translate vertex
	modelview = modelViewMatrix * v;
	
	gl_Position = projectionMatrix * modelview;
	
	//Eye transform
	eye = v.xyz-camPos;
}