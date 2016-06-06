//Entry attributes
attribute vec2 vertexIn;

uniform vec2 screenSize;

varying float height;
varying float pos;

void main(){
	pos = vertexIn.x;
	height = vertexIn.y;
	gl_Position = vec4((vertexIn * 2.0)/screenSize - vec2(1.0), 0.0, 1.0);
}