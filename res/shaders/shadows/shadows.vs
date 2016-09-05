#version 130
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

uniform vec3 objectPosition;

uniform float entity;

uniform mat4 localTransform;
uniform mat4 boneTransform;

uniform float isUsingInstancedData;
uniform sampler2D instancedDataSampler;

<include ../lib/shadowTricks.glsl>

void main(){
	texCoordPassed = vec4(texCoordIn/32768.0,0,0);
	//gl_Position = ftransform();
	vec4 v = localTransform * boneTransform * vec4(vertexIn.xyz, 1);
	
	float movingness = normalIn.w * (1-entity);
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
		v.xyz += objectPosition;
	}
		
	gl_Position = accuratizeShadowIn(depthMVP * v);
}

