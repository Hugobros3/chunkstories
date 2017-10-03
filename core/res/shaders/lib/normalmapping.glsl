// I suck at maths, so I used this code in the meanwhile I get how it works
// http://www.thetenthplanet.de/archives/1180

mat3 cotangent_frame(vec3 N, vec3 p, vec2 uv)
{
    // récupère les vecteurs du triangle composant le pixel
    vec3 dp1 = dFdx( p );
    vec3 dp2 = dFdy( p );
    vec2 duv1 = dFdx( uv );
    vec2 duv2 = dFdy( uv );

    // résout le système linéaire
    vec3 dp2perp = cross( dp2, N );
    vec3 dp1perp = cross( N, dp1 );
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;

    // construit une trame invariante à l'échelle 
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    return mat3( T * invmax, B * invmax, N );
}

vec3 perturb_normal( vec3 N, vec3 V, vec2 texcoord, vec3 normalMapDirection)
{
	normalMapDirection.x *= -1;
    // N, la normale interpolée et
    // V, le vecteur vue (vertex dirigé vers l'œil)
	mat3 TBN = cotangent_frame(N, -V, texcoord);
    return normalize(TBN * normalMapDirection);
}

vec3 decodeNormal(vec4 compressed)
{
	float scale = 1.7777;
	
	vec3 nn = compressed.xyz * vec3(2.0 * scale, 2 * scale, 0.0) + vec3(-scale, -scale, 1.0);
	float g = 2.0 / dot(nn.xyz, nn.xyz);
	vec3 n = vec3(g * nn.xy, g - 1.0);
	
	return n;
}

vec4 encodeNormal(vec3 uncompressed)
{
	float scale = 1.7777;
	vec2 enc = uncompressed.xy / (uncompressed.z + 1.0);
	enc /= scale;
	enc = enc * 0.5 + vec2(0.5);
	
	return vec4(enc, 0.0, 0.0);
}

/*vec3 decodeNormal(vec4 compressed)
{
	return compressed.rgb * 2.0 - vec3(1.0);
}

vec4 encodeNormal(vec3 uncompressed)
{
	return vec4(uncompressed * 0.5 + vec3(0.5), 0.0);
}*/