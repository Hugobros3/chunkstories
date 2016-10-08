#version 130
in vec3 vertexIn;

uniform vec2 screenSize;
const vec2 backgroundSize = vec2(1024);

out vec2 texCoord;
out vec2 ditherCoord;
out vec2 normalizedVertexIn;

void main()
{
	vec2 pixelPosition = (vertexIn.xy * 0.5 + vec2(0.5)) * screenSize;
	normalizedVertexIn = vertexIn.xy;
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	ditherCoord = pixelPosition / backgroundSize;
	texCoord = (vertexIn.xy * 0.5 + vec2(0.5));
}