varying vec2 vertex;

uniform vec3 sunPos;

varying vec3 eyeDirection;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
    vertex = gl_Vertex.xy;
	
	vec4 transformedSS = vec4(gl_Vertex.x, gl_Vertex.y, -1.0, 1.0);
	
	eyeDirection = (modelViewMatrixInv * inverse(projectionMatrix) * transformedSS ).xyz;
	
    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);
}