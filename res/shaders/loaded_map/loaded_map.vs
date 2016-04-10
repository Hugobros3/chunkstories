//Entry attributes
attribute vec2 vertexIn;

void main(){
	gl_Position = vec4((vertexIn.xy / vec2(64.0)) * 2, 0.0, 1.0);
}