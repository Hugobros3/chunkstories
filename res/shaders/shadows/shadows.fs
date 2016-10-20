#version 150
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

in vec4 texCoordPassed;

uniform sampler2D diffuseTexture;

out vec4 fragColor;

void main(){
	if(texture(diffuseTexture, texCoordPassed.st).a < 0.1)
		discard;
		
	gl_FragDepth = gl_FragCoord.z;
	fragColor = vec4(1.0, gl_FragCoord.z, gl_FragCoord.z, 1.0);
}
