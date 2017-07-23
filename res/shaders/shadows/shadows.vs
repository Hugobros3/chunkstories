#version 140
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

in vec4 vertexIn;
in vec4 normalIn;
in vec2 texCoordIn;

out vec4 texCoordPassed;

uniform mat4 depthMVP;

uniform float time;

uniform float vegetation;

uniform float allowForWavyStuff;

uniform mat4 objectMatrix;

uniform float useVoxelCoordinates;
uniform float isUsingInstancedData;
uniform sampler2D instancedDataSampler;

<include ../lib/shadowTricks.glsl>

void main(){

	if(useVoxelCoordinates > 0.0)
		texCoordPassed = vec4(texCoordIn/32768.0,0,0);
	else
		texCoordPassed = vec4(texCoordIn,0,0);
	
	//gl_Position = ftransform();
	vec4 v = objectMatrix * vec4(vertexIn.xyz, 1);
	
	float movingness = normalIn.w * allowForWavyStuff;
	<ifdef dynamicGrass>
	if(movingness > 0)
	{
		v.x += sin(time + v.z + v.y / 2.0) * 0.1;
		v.z += cos(time + v.x*1.5 + 0.3) * 0.1;
	}
	<endif dynamicGrass>
	
	if(isUsingInstancedData > 0)
	{
		mat4 matrixInstanced = mat4(texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8, 32), (gl_InstanceID * 8) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 1, 32), (gl_InstanceID * 8 + 1) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 2, 32), (gl_InstanceID * 8 + 2) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 8 + 3, 32), (gl_InstanceID * 8 + 3) / 32), 0)
									);
	
		v = matrixInstanced * vec4(vertexIn.xyz, 1.0);
	}
	else
	{
		//v.xyz += objectPosition;
	}
		
	gl_Position = accuratizeShadowIn(depthMVP * v);
}

