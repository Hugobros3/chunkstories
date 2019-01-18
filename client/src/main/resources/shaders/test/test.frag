#version 450

in vec2 vertexPos;
out vec4 fragColor;

vec4 funnyStuff(float a) {
	return vec4(1.0, a, 1.0-a, 1.0);
}

#define aaData funnyStuff(0.5)

int generateaaData() {
	return -5;
}

void main()
{
	vec4 lol = normalize(aaData);
	fragColor = vec4(vertexPos, 1.0 - length(vec2(-1.0) - vertexPos) * 0.5, 1.0);
}