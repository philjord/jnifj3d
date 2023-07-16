#version 120

precision mediump float;

uniform mat4 glModelViewMatrixInverse;
uniform mat4 glViewMatrix;

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;
//End of FFP inputs
varying vec2 glTexCoord0;

uniform sampler2D SourceTexture;
uniform sampler2D GreyscaleMap;
uniform samplerCube CubeMap;
uniform sampler2D NormalMap;
uniform sampler2D SpecularMap;

uniform int doubleSided;

uniform int hasSourceTexture;
uniform int hasGreyscaleMap;
uniform int hasCubeMap;
uniform int hasNormalMap;
uniform int hasEnvMask;

uniform int greyscaleAlpha;
uniform int greyscaleColor;

uniform int useFalloff;
uniform int vertexColors;
uniform int vertexAlpha;

uniform int hasWeaponBlood;

uniform vec4 glowColor;
uniform float glowMult;

uniform vec4 falloffParams;
uniform float falloffDepth;

uniform float lightingInfluence;
uniform float envReflection;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec3 N;
varying vec3 t;
varying vec3 b;
varying vec3 v;

vec4 colorLookup( float x, float y ) 
{	
	//BTDX store these as BGRA so deswizzle to RGBA
	return  texture2D( GreyscaleMap, vec2( clamp(x, 0.0, 1.0), clamp(y, 0.0, 1.0)) ).bgra;
}

void main( void )
{

	vec2 offset = glTexCoord0.st;
	
	vec4 baseMap = texture2D( SourceTexture, offset );
	if(alphaTestEnabled != 0){		
		if(alphaTestFunction==512)discard;//never (never keep it)
		if(alphaTestFunction==513 && !(baseMap.a< alphaTestValue))discard;
		if(alphaTestFunction==514 && !(baseMap.a==alphaTestValue))discard;
		if(alphaTestFunction==515 && !(baseMap.a<=alphaTestValue))discard;				
		if(alphaTestFunction==516 && !(baseMap.a> alphaTestValue))discard;
		if(alphaTestFunction==517 && !(baseMap.a!=alphaTestValue))discard;
		if(alphaTestFunction==518 && !(baseMap.a>=alphaTestValue))discard;			
		//alphaTestFunction==519//always (always keep it)
	}
	//swizzle the alpha and green  
	vec4 normalMap = vec4( texture2D( NormalMap, offset ).ag * 2.0 - 1.0, 0.0, 0.0 );
	//re-create the z  
	normalMap.z = sqrt( 1.0 - dot( normalMap.xy,normalMap.xy ) ); 
	vec2 specMap = texture2D( SpecularMap, offset ).ag; 
	
	vec3 normal = N;
	if ( bool(hasNormalMap) ) {
		normal = normalize(normalMap.rgb * 2.0 - 1.0);
	}
	if ( !gl_FrontFacing && bool(doubleSided) ) {
		normal *= -1.0;	
	}
	
	vec3 L = normalize(LightDir);
	vec3 V = normalize(ViewVec);
	vec3 R = reflect(-L, normal);
	vec3 H = normalize( L + V );
	
	float NdotL = max( dot(normal, L), 0.000001 );
	float NdotH = max( dot(normal, H), 0.000001 );
	float NdotV = max( dot(normal, V), 0.000001 );
	float LdotH = max( dot(L, H), 0.000001 );
	float NdotNegL = max( dot(normal, -L), 0.000001 );

	vec3 reflected = reflect( V, normal );
	vec3 reflectedVS = t * reflected.x + b * reflected.y + N * reflected.z;
	vec3 reflectedWS = vec3( glViewMatrix * (glModelViewMatrixInverse * vec4( reflectedVS, 0.0 )) );
	
	// Falloff
	float falloff = 1.0;
	if ( bool(useFalloff) ) {
		float startO = min(falloffParams.z, 1.0);
		float stopO = max(falloffParams.w, 0.0);
		
		// TODO: When X and Y are both 0.0 or both 1.0 the effect is reversed.
		falloff = smoothstep( falloffParams.y, falloffParams.x, abs(V.b));
    
		falloff = mix( max(falloffParams.w, 0.0), min(falloffParams.z, 1.0), falloff );
	}
	
	float alphaMult = glowColor.a * glowColor.a;
	
	vec4 color;
	color.rgb = baseMap.rgb;
	color.a = baseMap.a;
	
	// FO4 Unused?
	//if ( hasWeaponBlood ) {
	//	color.rgb = vec3( 1.0, 0.0, 0.0 ) * baseMap.r;
	//	color.a = baseMap.a * baseMap.g;
	//}
	
	color.rgb *= C.rgb * glowColor.rgb;
	color.a *= C.a * falloff * alphaMult;

	if ( bool(greyscaleColor) ) {
		// Only Red emissive channel is used
		float emRGB = glowColor.r;

		vec4 luG = colorLookup( baseMap.g, C.g * falloff * emRGB );

		color.rgb = luG.rgb;
	}
	
	if ( bool(greyscaleAlpha) ) {
		vec4 luA = colorLookup( baseMap.a, C.a * falloff * alphaMult );
		
		color.a = luA.a;
	}
	
	vec3 diffuse = A.rgb + (D.rgb * NdotL);
	color.rgb = mix( color.rgb, color.rgb * diffuse, lightingInfluence );
	
	color.rgb *= glowMult;
	
	// Specular
	float g = 1.0;
	float s = 1.0;
	if ( bool(hasEnvMask) ) {
		g = specMap.r;
		s = specMap.g;
	}
	
	// Environment
	vec4 cube = textureCube( CubeMap, reflectedWS );
	if ( bool(hasCubeMap) ) {
		cube.rgb *= envReflection * sqrt(g) * s;
		
		color.rgb += cube.rgb * falloff;
	}
	
	gl_FragColor.rgb = color.rgb;
	gl_FragColor.a = color.a;
}