#version 450

in vec2 vertexPos;
out vec4 fragColor;

//#include struct <...BlockInfo>
//#instanceData BlockInfo blockInfo

struct BlockInfo2;

struct BlockInfo {
	vec4 position;
	vec4 blockColor;
	BlockInfo2 b2;
};

struct BlockInfo2 {
	int z;
};

buffer _instanceDataBuffer_BlockInfo_blockInfo {
	BlockInfo[512] _instanceDataArray_BlockInfo_blockInfo;
};

#define blockInfo _instanceDataArray_BlockInfo_blockInfo[gl_InstanceIndex]

void main()
{
	vec4 lol = blockInfo.position;
	fragColor = vec4(vertexPos, 1.0 - length(vec2(-1.0) - vertexPos) * 0.5, 1.0);
}