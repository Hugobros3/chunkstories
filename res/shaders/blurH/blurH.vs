#version 330
uniform vec2 screenSize;

in vec2 vertexIn;
out vec2 texCoord;
out vec2 texCoordBlur[14];

void main()
{
	vec2 pixelSize = vec2(1.0) / screenSize;
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5+0.5;    
	texCoordBlur[0] = texCoord + vec2(-7.0, 0.0) * pixelSize;
	texCoordBlur[1] = texCoord + vec2(-6.0, 0.0) * pixelSize;
	texCoordBlur[2] = texCoord + vec2(-5.0, 0.0) * pixelSize;
	texCoordBlur[3] = texCoord + vec2(-4.0, 0.0) * pixelSize;
	texCoordBlur[4] = texCoord + vec2(-3.0, 0.0) * pixelSize;
	texCoordBlur[5] = texCoord + vec2(-2.0, 0.0) * pixelSize;
	texCoordBlur[6] = texCoord + vec2(-1.0, 0.0) * pixelSize;
	texCoordBlur[7] = texCoord + vec2( 1.0, 0.0) * pixelSize;
	texCoordBlur[8] = texCoord + vec2( 2.0, 0.0) * pixelSize;
	texCoordBlur[9] = texCoord + vec2( 3.0, 0.0) * pixelSize;
	texCoordBlur[10] = texCoord + vec2( 4.0, 0.0) * pixelSize;
	texCoordBlur[11] = texCoord + vec2( 5.0, 0.0) * pixelSize;
	texCoordBlur[12] = texCoord + vec2( 6.0, 0.0) * pixelSize;
	texCoordBlur[13] = texCoord + vec2( 7.0, 0.0) * pixelSize;
}