#version 330
in vec2 vertexIn;

out vec2 vertex;
out vec3 eyeDirection;

uniform vec3 sunPos;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
    vertex = vertexIn.xy;
	
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, 1.0, 1.0);
	
	eyeDirection = (modelViewMatrixInv * projectionMatrixInv * transformedSS ).xyz;
	
    gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}
