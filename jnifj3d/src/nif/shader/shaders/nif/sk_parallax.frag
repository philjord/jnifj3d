#version 120

precision mediump float;

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;
//End of FFP inputs
varying vec2 glTexCoord0;

uniform sampler2D BaseMap;
uniform sampler2D NormalMap;
uniform sampler2D HeightMap;
uniform sampler2D LightMask;
uniform sampler2D BacklightMap;

uniform vec3 specColor;
uniform float specStrength;
uniform float specGlossiness;

uniform vec3 glowColor;
uniform float glowMult;

uniform float alpha;

uniform vec2 uvScale;
uniform vec2 uvOffset;

uniform int hasEmit;
uniform int hasSoftlight;
uniform int hasBacklight;
uniform int hasRimlight;

uniform float lightingEffect1;
uniform float lightingEffect2;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;


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

void main( void )
{
	vec2 offset = glTexCoord0.st;
	
	vec3 E = normalize(ViewVec);
	
	float height = texture2D( HeightMap, offset ).r;
	offset += E.xy * (height * 0.08 - 0.04); 

	vec4 baseMap = texture2D( BaseMap, offset );
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
	vec4 normalMap = texture2D( NormalMap, offset );
	
	vec3 normal = normalize(normalMap.rgb * 2.0 - 1.0);
	
	vec3 L = normalize(LightDir);
	vec3 R = reflect(-L, normal);
	vec3 H = normalize( L + E );
	
	float NdotL = max( dot(normal, L), 0.0 );
	float NdotH = max( dot(normal, H), 0.0 );
	float EdotN = max( dot(normal, E), 0.0 );
	float NdotNegL = max( dot(normal, -L), 0.0 );


	vec4 color;
	vec3 albedo = baseMap.rgb * C.rgb;
	vec3 diffuse = A.rgb + (D.rgb * NdotL);


	// Emissive
	vec3 emissive = vec3(0.0);
	if ( hasEmit == 1 ) {
		emissive += glowColor * glowMult;
	}

	// Specular
	vec3 spec = specColor * specStrength * normalMap.a * pow(NdotH, specGlossiness);
	spec *= D.rgb;

	vec3 backlight = vec3(0.0);
	if ( hasBacklight == 1 ) {
		backlight = texture2D( BacklightMap, offset ).rgb;
		backlight *= NdotNegL;
		
		emissive += backlight * D.rgb;
	}

	vec4 mask = vec4(0.0);
	if ( hasRimlight == 1 || hasSoftlight == 1 ) {
		mask = texture2D( LightMask, offset );
	}

	vec3 rim = vec3(0.0);
	if ( hasRimlight == 1 ) {
		rim = mask.rgb * pow(vec3((1.0 - EdotN)), vec3(lightingEffect2));
		rim *= smoothstep( -0.2, 1.0, dot(-L, E) );
		
		emissive += rim * D.rgb;
	}
	
	vec3 soft = vec3(0.0);
	if ( hasSoftlight == 1 ) {
		float wrap = (dot(normal, L) + lightingEffect1) / (1.0 + lightingEffect1);

		soft = max( wrap, 0.0 ) * mask.rgb * smoothstep( 1.0, 0.0, NdotL );
		soft *= sqrt( clamp( lightingEffect1, 0.0, 1.0 ) );
		
		emissive += soft * D.rgb;
	}

	color.rgb = albedo * (diffuse + emissive) + spec;
	color.rgb = tonemap( color.rgb ) / tonemap( vec3(1.0) );
	color.a = C.a * baseMap.a;

	gl_FragColor = color;
	gl_FragColor.a *= alpha;
}
