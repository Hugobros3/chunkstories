#version 330
uniform vec2 screenSize;

in vec3 vertexIn;
out vec2 texCoord;

void main()
{
	//vec2 pixelSize = vec2(1.0) / screenSize;
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5+0.5;
}