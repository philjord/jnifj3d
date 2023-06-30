#version 120

precision highp float; // must be highp if any uniform (like lightSource) is also in the vertex shader
precision highp int;

uniform vec4 glLightModelambient;

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
uniform sampler2D GlowMap;
uniform sampler2D LightMask;
uniform sampler2D BacklightMap;

uniform int hasGlowMap;
uniform vec3 glowColor;
uniform float glowMult;

uniform vec3 specColor;
uniform float specStrength;
uniform float specGlossiness;

uniform int hasSoftlight;
uniform int hasBacklight;
uniform int hasRimlight;

uniform float lightingEffect1;
uniform float lightingEffect2;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 ColorEA;
varying vec4 ColorD;

vec3 tonemap(vec3 x)
{
	float A = 0.15;
	float B = 0.50;
	float C = 0.10;
	float D = 0.20;
	float E = 0.02;
	float F = 0.30;

	return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
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
	vec4 nmap = texture2D( NormalMap, offset );
	
	vec4 color;
	color.rgb = baseMap.rgb;
	
	vec3 normal = normalize(nmap.rgb * 2.0 - 1.0);
		
	float spec = 0.0;
	
	vec3 emissive = texture2D( GlowMap, offset ).rgb;
	
	// Skyrim
	if ( bool(hasGlowMap) ) {
		color.rgb += tonemap( baseMap.rgb * emissive.rgb * glowColor ) / tonemap( 1.0 / (vec3(glowMult) + 0.001) );
	}
	
	vec3 L = normalize(LightDir);
	vec3 E = normalize(ViewVec);
	vec3 R = reflect(-L, normal);
	//vec3 H = normalize( L + E );
	float NdotL = max(dot(normal, L), 0.0);
	
	color.rgb *= ColorEA.rgb + ColorD.rgb * NdotL;
	color.a = ColorD.a * baseMap.a;
	
	if ( NdotL > 0.0 && specStrength > 0.0 ) {
		float RdotE = max( dot( R, E ), 0.0 );
		
		// TODO: Attenuation?
		
		if ( RdotE > 0.0 ) {
			spec = nmap.a * glLightSource[0].specular.r * specStrength * pow(RdotE, 0.8*specGlossiness);
			color.rgb += spec * specColor;
		}
	}
	
	vec3 backlight;
	if ( bool(hasBacklight) ) {
		backlight = texture2D( BacklightMap, offset ).rgb;
		color.rgb += baseMap.rgb * backlight * (1.0 - NdotL) * 0.66;
	}
	
	vec4 mask;
	if ( bool(hasRimlight) || bool(hasSoftlight) ) {
		mask = texture2D( LightMask, offset );
	}
	
	float facing = dot(-L, E);
	
	vec3 rim;
	if ( bool(hasRimlight) ) {
		rim = vec3(( 1.0 - NdotL ) * ( 1.0 - max(dot(normal, E), 0.0)));
		//rim = smoothstep( 0.0, 1.0, rim );
		rim = mask.rgb * pow(rim, vec3(lightingEffect2)) * glLightSource[0].diffuse.rgb * vec3(0.66);
		rim *= smoothstep( -0.5, 1.0, facing );
		color.rgb += rim;
	}
	
	float wrap = dot(normal, -L);
	
	vec3 soft;
	if ( bool(hasSoftlight) ) {
		soft = vec3((1.0 - wrap) * (1.0 - NdotL));
		soft = smoothstep( -1.0, 1.0, soft );

		// TODO: Very approximate, kind of arbitrary. There is surely a more correct way.
		soft *= mask.rgb * pow(soft, vec3(4.0/(lightingEffect1*lightingEffect1))); // * glLightSource0ambient.rgb;
		//soft *= smoothstep( -1.0, 0.0, soft );
		//soft = mix( soft, color.rgb, glLightSource0ambient.rgb );
		
		//soft = smoothstep( 0.0, 1.0, soft );
		soft *= glLightSource[0].diffuse.rgb * glLightModelambient.rgb + (0.01 * lightingEffect1*lightingEffect1);

		//soft = clamp( soft, 0.0, 0.5 );
		//soft *= smoothstep( -0.5, 1.0, facing );
		//soft = mix( soft, color.rgb, glLightSource0diffuse.rgb );
		color.rgb += baseMap.rgb * soft;
	}
	
	//color = min( color, 1.0 );
	
	gl_FragColor = color;
}
