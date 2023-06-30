#version 120

precision mediump float;

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;
//End of FFP inputs
varying vec2 glTexCoord0;

uniform sampler2D SourceTexture;
uniform sampler2D GreyscaleMap;

uniform int doubleSided;

uniform int hasSourceTexture;
uniform int hasGreyscaleMap;
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

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 C;

varying vec3 N;
varying vec3 v;

vec4 colorLookup( float x, float y ) {
	
	return texture2D( GreyscaleMap, vec2( clamp(x, 0.0, 1.0), clamp(y, 0.0, 1.0)) );
}

void main( void )
{
	vec4 baseMap = texture2D( SourceTexture, glTexCoord0.st );
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
	vec4 color;

	vec3 normal = N;
	
	// Reconstructed normal
	//normal = normalize(cross(dFdy(v.xyz), dFdx(v.xyz)));
	
	//if ( doubleSided != 1 && !gl_FrontFacing ) { return; }
	
	vec3 E = normalize(ViewVec);

	float tmp2 = falloffDepth; // Unused right now
	
	// Falloff
	float falloff = 1.0;
	if ( useFalloff == 1 ) {
		float startO = min(falloffParams.z, 1.0);
		float stopO = max(falloffParams.w, 0.0);
		
		// TODO: When X and Y are both 0.0 or both 1.0 the effect is reversed.
		falloff = smoothstep( falloffParams.y, falloffParams.x, abs(E.b));

		falloff = mix( max(falloffParams.w, 0.0), min(falloffParams.z, 1.0), falloff );
	}
	
	float alphaMult = glowColor.a * glowColor.a;
	
	color.rgb = baseMap.rgb;
	color.a = baseMap.a;
	
	if ( hasWeaponBlood == 1 ) {
		color.rgb = vec3( 1.0, 0.0, 0.0 ) * baseMap.r;
		color.a = baseMap.a * baseMap.g;
	}
	
	color.rgb *= C.rgb * glowColor.rgb;
	color.a *= C.a * falloff * alphaMult;

	if ( greyscaleColor == 1 ) {
		// Only Red emissive channel is used
		float emRGB = glowColor.r;

		vec4 luG = colorLookup( baseMap.g, C.g * falloff * emRGB );

		color.rgb = luG.rgb;
	}
	
	if ( greyscaleAlpha == 1 ) {
		vec4 luA = colorLookup( baseMap.a, C.a * falloff * alphaMult );
		
		color.a = luA.a;
	}

	gl_FragColor.rgb = color.rgb * glowMult;
	gl_FragColor.a = color.a;
}
