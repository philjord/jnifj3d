#version 120

precision mediump float;

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;
//End of FFP inputs
varying vec2 glTexCoord0;


uniform sampler2D BaseMap;
uniform sampler2D NormalMap;
uniform sampler2D GlowMap;
uniform sampler2D BacklightMap;
uniform sampler2D SpecularMap;
uniform sampler2D GreyscaleMap;

uniform vec3 specColor;
uniform float specStrength;
uniform float specGlossiness; // "Smoothness" in FO4; 0-1
uniform float fresnelPower;

uniform float paletteScale;

uniform vec3 glowColor;
uniform float glowMult;

uniform float alpha;

uniform vec3 tintColor;

uniform int hasEmit;
uniform int hasGlowMap;
uniform int hasSoftlight;
uniform int hasBacklight;
uniform int hasRimlight;
uniform int hasTintColor;
uniform int hasSpecularMap;
uniform int greyscaleColor;
uniform int doubleSided;

uniform float lightingEffect1;
uniform float rimPower;
uniform float backlightPower;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;



float G1V(float NdotV, float k)
{
    return 1.0 / (NdotV * (1.0 - k) + k);
}

float LightingFuncGGX_REF(float NdotL, float NdotV, float NdotH, float LdotH, float roughness, float F0)
{
    float alpha = roughness * roughness;
	
    float F, D, vis;

    // D
    float alphaSqr = alpha * alpha;
    float denom = NdotH * NdotH * (alphaSqr - 1.0) + 1.0;
    D = alphaSqr / (denom * denom);

    // F
    float LdotH5 = pow( 1.0 - LdotH, fresnelPower );
    F = F0 + (1.0 - F0) * LdotH5;

    // V
    float k = alpha / 2.0;
    vis = G1V( NdotL, k ) * G1V( NdotV, k );

    float specular = NdotL * D * F * vis;
    return specular;
}

vec3 F_Schlick( vec3 f0, float f90, float u )
{
    return f0 + ( f90 - f0 ) * pow( 1.0 - u, fresnelPower );
}

float Fr_DisneyDiffuse( float NdotV, float NdotL, float LdotH, float linearRoughness )
{
    float  energyBias   = mix( 0.0, 0.5, linearRoughness );
    float  energyFactor = mix( 1.0, 1.0 / 1.51, linearRoughness );
    float  fd90         = energyBias + 2.0 * LdotH * LdotH * linearRoughness;
    vec3   f0           = vec3( 1.0 );
    float  lightScatter = F_Schlick( f0, fd90, NdotL ).r;
    float  viewScatter  = F_Schlick( f0, fd90, NdotV ).r;

    return lightScatter * viewScatter * energyFactor;
}

vec3 tonemap(vec3 x)
{
	float _A = 0.15;
	float _B = 0.50;
	float _C = 0.10;
	float _D = 0.20;
	float _E = 0.02;
	float _F = 0.30;

	return ((x*(_A*x+_C*_B)+_D*_E)/(x*(_A*x+_B)+_D*_F))-_E/_F;
}

vec3 toGrayscale(vec3 color)
{
	return vec3(dot(vec3(0.3, 0.59, 0.11), color));
}

vec4 colorLookup( float x, float y ) 
{	
	//BTDX store these as BGRA so deswizzle to RGBA
	return  texture2D( GreyscaleMap, vec2( clamp(x, 0.0, 1.0), clamp(y, 0.0, 1.0)) ).bgra;
}

float scale( float f, float min, float max )
{
	return f * ( max - min ) + min;
}

void main( void )
{

	vec2 offset = glTexCoord0.st;

	vec4 baseMap = texture2D( BaseMap, offset );
	
	if(alphaTestEnabled != 0)
	{				
	 	if(alphaTestFunction==516)//>
			if(baseMap.a<=alphaTestValue)discard;			
		else if(alphaTestFunction==518)//>=
			if(baseMap.a<alphaTestValue)discard;		
		else if(alphaTestFunction==514)//==
			if(baseMap.a!=alphaTestValue)discard;
		else if(alphaTestFunction==517)//!=
			if(baseMap.a==alphaTestValue)discard;
		else if(alphaTestFunction==513)//<
			if(baseMap.a>=alphaTestValue)discard;
		else if(alphaTestFunction==515)//<=
			if(baseMap.a>alphaTestValue)discard;		
		else if(alphaTestFunction==512)//never	
			discard;			
	}
	
	//swizzle the alpha and green  
	vec4 normalMap = vec4( texture2D( NormalMap, offset ).ag * 2.0 - 1.0, 0.0, 0.0 );
	//re-create the z  
	normalMap.z = sqrt( 1.0 - dot( normalMap.xy,normalMap.xy ) ); 
	vec2 specMap = texture2D( SpecularMap, offset ).ag; 
	
	vec4 glowMap = texture2D( GlowMap, offset );
	
	vec3 normal = normalize(normalMap.rgb * 2.0 - 1.0);
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

	vec4 color;
	vec3 albedo = baseMap.rgb * C.rgb;
	vec3 diffuse = A.rgb + D.rgb * NdotL;
	if ( bool(greyscaleColor) ) {
		vec4 luG = colorLookup( baseMap.g, C.g * paletteScale );

		albedo = luG.rgb;
	}
	
	// Emissive
	vec3 emissive = vec3(0.0);
	if ( bool(hasEmit) ) {
		emissive += glowColor * glowMult;
		
		if ( bool(hasGlowMap) ) {
			emissive *= glowMap.rgb;
		}
	}

	// Specular
	float g = 1.0;
	float s = 1.0;
	float roughness = 0.1;
	vec3 spec = vec3(0.0);
	if ( bool(hasSpecularMap) ) {
		g = specMap.r;
		s = specMap.g;
		roughness = scale( 1.0 - ( g * specGlossiness ), 0.1, 0.9 );
		spec = specColor * s * LightingFuncGGX_REF( NdotL, NdotV, NdotH, LdotH, roughness, 0.04 ) * specStrength;
		spec *= D.rgb * 0.9;
		spec = clamp( spec, 0.0, 1.0 );
	}
	
	//float Fd = Fr_DisneyDiffuse( NdotV, NdotL, LdotH, roughness );

	vec3 backlight = vec3(0.0);
	//if ( hasBacklight ) {
	//	backlight = texture2D( BacklightMap, offset ).rgb;
	//	backlight *= NdotNegL;
	//	
	//	emissive += backlight * D.rgb;
	//}

	vec4 mask = vec4(0.0);
	if ( bool(hasRimlight) || bool(hasSoftlight) ) {
		mask = vec4( s );
	}

	vec3 rim = vec3(0.0);
	if ( bool(hasRimlight) ) {
		rim = mask.rgb * pow(vec3((1.0 - NdotV)), vec3(rimPower));
		rim *= smoothstep( -0.2, 1.0, dot(-L, V) );
		
		emissive += rim * D.rgb;
	}
	
	vec3 soft = vec3(0.0);
	//if ( hasSoftlight ) {
	//	float wrap = (dot(normal, L) + lightingEffect1) / (1.0 + lightingEffect1);
    //
	//	soft = max( wrap, 0.0 ) * mask.rgb * smoothstep( 1.0, 0.0, NdotL );
	//	soft *= sqrt( clamp( lightingEffect1, 0.0, 1.0 ) );
	//	
	//	emissive += soft * D.rgb;
	//}
	
	if ( bool(hasTintColor) ) {
		albedo *= tintColor;
	}

	color.rgb = albedo * (diffuse + emissive);
	color.rgb += spec;
	color.rgb = tonemap( color.rgb ) / tonemap( vec3(1.0) );
	color.a = C.a * baseMap.a;
    
	gl_FragColor = color;
	gl_FragColor.a *= alpha;
}

