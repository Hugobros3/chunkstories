#version 330
//Entry attributes
in vec2 vertexIn;

uniform vec2 screenSize;

out float height;
out float pos;

uniform float sizeInPixels;
uniform float heightInPixels;

uniform float xPosition;
uniform float yPosition;

void main(){

	//float sizeInPixels = 1024;
	//float heightInPixels = 256;

	pos = (vertexIn.x * 0.5 + 0.5) * sizeInPixels;
	height = (vertexIn.y * 0.5 + 0.5) * heightInPixels;
	gl_Position = vec4(vec2((pos + xPosition) / screenSize.x, (height + yPosition) / screenSize.y) * 2.0 - vec2(1.0), 0.0, 1.0);
}