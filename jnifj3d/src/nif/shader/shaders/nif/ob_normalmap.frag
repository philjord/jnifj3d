#version 120

precision highp float; // must be highp if any uniform (like lightSource) is also in the vertex shader
precision highp int;

struct lightSource
{
	 vec4 position;
	 vec4 diffuse;
	 vec4 specular;
	 float constantAttenuation, linearAttenuation, quadraticAttenuation;
	 float spotCutoff, spotExponent;
	 vec3 spotDirection;
};

uniform int numberOfLights;
const int maxLights = 2;
uniform lightSource glLightSource[maxLights];

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;
//End of FFP inputs
varying vec2 glTexCoord0;

uniform sampler2D BaseMap;
uniform sampler2D NormalMap;
uniform sampler2D LightMask;
uniform sampler2D BacklightMap;

uniform vec3 specColor;
uniform float specStrength;
uniform float specGlossiness;

uniform vec3 glowColor;
uniform float glowMult;

uniform int hasEmit;
uniform int hasSoftlight;
uniform int hasBacklight;
uniform int hasRimlight;

uniform float lightingEffect1;
uniform float lightingEffect2;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 ColorEA;
varying vec4 ColorD;

varying vec4 A;
varying vec4 D;

varying vec3 N;


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
	
	// check for unbound normalmap and use vertex normals instead
	if(normalMap == vec4(0,0,0,1))
		normal = N;
		
	vec3 L = normalize(LightDir);
	vec3 E = normalize(ViewVec);
	vec3 R = reflect(-L, normal);
	
	float NdotL  = max( dot(normal, L), 0.0 );
	float EdotN  = max( dot(normal, E), 0.0 );
	float wrap   = max( dot(normal, -L), 0.0 );
	float facing = max( dot(-L, E), 0.0 );
	

	vec4 color;
	color.rgb = baseMap.rgb;
	color.rgb *= ColorEA.rgb + (ColorD.rgb * NdotL);
	color.a = ColorD.a * baseMap.a;
	
	
	// Emissive
	if ( bool(hasEmit) ) {
		color.rgb += tonemap( baseMap.rgb * glowColor ) / tonemap( 1.0 / vec3(glowMult + 0.001) );
	}

	// Specular
	float spec = 0.0;
	if ( NdotL > 0.0 && specStrength > 0.0 ) {
		float RdotE = max( dot(R, E), 0.0 );
		if ( RdotE > 0.0 ) {
			spec = normalMap.a * glLightSource[0].specular.r * specStrength * pow(RdotE, 0.8*specGlossiness);
			color.rgb += spec * specColor;
		}
	}

	vec3 backlight;
	if ( bool(hasBacklight) ) {
		backlight = texture2D( BacklightMap, offset ).rgb;
		color.rgb += baseMap.rgb * backlight * wrap * D.rgb;
	}

	vec4 mask;
	if ( bool(hasRimlight) || bool(hasSoftlight) ) {
		mask = texture2D( LightMask, offset );
	}

	vec3 rim;
	if ( bool(hasRimlight) ) {
		rim = vec3((1.0 - NdotL) * (1.0 - EdotN));
		rim = mask.rgb * pow(rim, vec3(lightingEffect2)) * D.rgb * vec3(0.66);
		rim *= smoothstep( -0.5, 1.0, facing );
		
		color.rgb += rim;
	}
	
	vec3 soft;
	if ( bool(hasSoftlight) ) {
		soft = vec3((1.0 - wrap) * (1.0 - NdotL));
		soft = smoothstep( -1.0, 1.0, soft );

		// TODO: Very approximate, kind of arbitrary. There is surely a more correct way.
		soft *= mask.rgb * pow(soft, vec3(4.0/(lightingEffect1*lightingEffect1)));
		soft *= D.rgb * A.rgb + (0.01 * lightingEffect1*lightingEffect1);

		color.rgb += baseMap.rgb * soft;
	}

	color.rgb = tonemap( color.rgb ) / tonemap( vec3(1.0) );
	
	gl_FragColor = color;
}
