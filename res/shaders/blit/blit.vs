uniform vec2 screenSize;

attribute vec3 vertexIn;
varying vec2 texCoord;

void main()
{
	vec2 pixelSize = vec2(1.0) / screenSize;
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5+0.5;
}