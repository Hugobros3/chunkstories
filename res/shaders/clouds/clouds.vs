attribute vec4 vertexIn;
attribute vec4 normalIn;
attribute float alphaIn;

varying vec4 vertex;

uniform vec3 camPos;
uniform vec3 sunPos;

varying vec3 eyeDirection;
varying float alpha;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
	alpha = alphaIn;
	
    vertex = projectionMatrix * modelViewMatrix * vertexIn;
	
	//vec4 transformedSS = vec4(gl_Vertex.x, gl_Vertex.y, -1.0, 1.0);
	
	eyeDirection = normalize(vertexIn.xyz - camPos);
	
	//eyeDirection = (modelViewMatrixInv * inverse(projectionMatrix) * transformedSS ).xyz;
	
    gl_Position = vec4(vertex);
}