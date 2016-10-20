#version 150
//Entry attributes
in vec2 vertexIn;

uniform vec2 screenSize;

out float height;
out float pos;

void main(){
	pos = vertexIn.x;
	height = vertexIn.y;
	gl_Position = vec4((vec2(vertexIn.x, vertexIn.y) * 2.0)/screenSize - vec2(1.0), 0.0, 1.0);
}