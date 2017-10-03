#version 330

out vec4 proc_texcoord;
out vec4 texcoord;
out vec2 lightMapCoords;

// The vertex we're going to pass to the fragment shader.
out vec4 outVertex;

uniform float time;

out vec4 modelview;

attribute vec4 particlesPositionIn;
attribute vec2 textureCoordinatesIn;

uniform float areTextureCoordinatesSupplied;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float billboardSize;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

const vec2 vertices[] = vec2[]( vec2(1.0 ,  1.0),
								vec2(-1.0,  1.0),
								vec2(-1.0, -1.0),
								vec2(-1.0, -1.0),
								vec2(1.0 ,  1.0),
								vec2(1.0,  -1.0)
								);

void main(){
	//Usual variable passing
	
	vec2 proceduralVertex = vertices[gl_VertexID % 6];
	
	proc_texcoord = vec4(proceduralVertex*0.5+0.5, 0, 0);
	texcoord = vec4(textureCoordinatesIn, 0, 0);
	
	vec4 v = particlesPositionIn;
	
	outVertex = v;
	
	//Compute lightmap coords
	lightMapCoords = vec2(0.0, 1.0);
	
	modelview = modelViewMatrix * v;
	
	float angle = (particlesPositionIn.x + particlesPositionIn.y + particlesPositionIn.z) * 2.0;
	
	vec2 proceduralVertexRotated = vec2(proceduralVertex.x * cos(angle) - proceduralVertex.y * sin(angle), proceduralVertex.x * sin(angle) + proceduralVertex.y * cos(angle));
	
	modelview += vec4(proceduralVertexRotated*billboardSize, 0.0, 0.0);
	
	//modelview += vec4(proceduralVertex*billboardSize, 0.0, 0.0);
	
	vec4 clochard = projectionMatrix * modelview;
	
	gl_Position = clochard;
}
