//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io
const float distScale = 0.8;

//Transform coordinates to skew buffer while reading
vec4 accuratizeShadow(vec4 shadowMap)
{
	shadowMap.xy /= ( (1.0f - distScale) + sqrt(shadowMap.x * shadowMap.x + shadowMap.y * shadowMap.y) * distScale );
	
	shadowMap.w = length(shadowMap.xy);
	
	//Transformation for screen-space
	shadowMap.xyz = shadowMap.xyz * 0.5 + 0.5;
	return shadowMap;
}

//Transform coordinates to skew buffer while writing
vec4 accuratizeShadowIn(vec4 shadowMap)
{
	shadowMap.xy *= 1.0 /( (1.0f - distScale) + sqrt(shadowMap.x * shadowMap.x + shadowMap.y * shadowMap.y) * distScale );
	
	return shadowMap;
}