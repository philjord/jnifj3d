#version 120

precision mediump float;

//https://github.com/niftools/nifskope/blob/develop/res/shaders/fo4_default.frag

uniform mat4 glModelMatrix;
uniform mat4 glModelViewMatrixInverse;

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;
//End of FFP inputs
varying vec2 glTexCoord0;

uniform sampler2D BaseMap;
uniform sampler2D NormalMap;
//uniform sampler2D LightMask;
uniform sampler2D BacklightMap;
uniform sampler2D EnvironmentMap;
uniform sampler2D SpecularMap;
uniform sampler2D GreyscaleMap;
uniform samplerCube CubeMap;

uniform vec3 specColor;
uniform float specStrength;
uniform float specGlossiness; // "Smoothness" in FO4; 0-1
uniform float fresnelPower;

uniform float paletteScale;

uniform vec3 glowColor;
uniform float glowMult;

uniform float alpha;


uniform int hasEmit;
uniform int hasSoftlight;
uniform int hasBacklight;
uniform int hasRimlight;
uniform int hasCubeMap;
uniform int hasEnvMask;
uniform int hasSpecularMap;
uniform int greyscaleColor;
uniform int doubleSided;

uniform float lightingEffect1;
uniform float rimPower;
uniform float backlightPower;

uniform float envReflection;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec3 N;
varying vec3 t;
varying vec3 b;



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
	//BTDX store these as BGRA so deswizzle to RGBA - PJ no?
	return  texture2D( GreyscaleMap, vec2( clamp(x, 0.0, 1.0), clamp(y, 0.0, 1.0)) );//.bgra;
}

float scale( float f, float min, float max )
{
	return f * ( max - min ) + min;
}
#define FLT_EPSILON 1.192092896e-07F // smallest such that 1.0 + FLT_EPSILON != 1.0
void main( void )
{

																																			
	vec2 offset = glTexCoord0.st; 

	vec4 baseMap = texture2D( BaseMap, offset );								//gl_FragColor = baseMap;return;		

	if(alphaTestEnabled != 0) {	
		float combinedA = baseMap.a * C.a;
		if(alphaTestFunction==512)discard;//never (never keep it)
		else if(alphaTestFunction==513 && !(combinedA < alphaTestValue))discard;
		else if(alphaTestFunction==514 && !(combinedA == alphaTestValue))discard;
		else if(alphaTestFunction==515 && !(combinedA <= alphaTestValue))discard;				
		else if(alphaTestFunction==516 && !(combinedA > alphaTestValue))discard;
		else if(alphaTestFunction==517 && !(combinedA != alphaTestValue))discard;
		else if(alphaTestFunction==518 && !(combinedA >= alphaTestValue))discard;			
		//alphaTestFunction==519//always (always keep it)
	}
	
	
	//FIXME: PJ, this seems to cause big lighting trouble? it's cause teh sharp shadow lines
	//NOTE the my coords are y up whereas nifskope has z up
	vec4 normalMap = vec4( texture2D( NormalMap, offset ).xyy * 2.0 - 1.0, 0.0); normalMap.y=0.0;
	//re-create the z  
	//normalMap.z = sqrt( 1.0 - dot( normalMap.xy,normalMap.xy ) );    			//gl_FragColor =  normalMap;return;

	// spec only use 2 value r and g below (r is gloss, g is spec)
	//https://www.reddit.com/r/FalloutMods/comments/3uaq1l/fo4_lets_talk_about_texture_creation_editing/
	vec2 specMap = texture2D( SpecularMap, offset ).rg; 						//gl_FragColor =  vec4(specMap,0,1);return;
	 
	vec3 normal = normalize(normalMap.rgb * 2.0 - 1.0);							//gl_FragColor =  vec4(normal,1);return;
	if ( !gl_FrontFacing && bool(doubleSided) ) {
		normal *= -1.0;	
	}																		//gl_FragColor =  vec4(normal,1);return;
	
	vec3 L = normalize(LightDir);											//if(normal.x<0.9){gl_FragColor =  vec4(L,1);return;}
	vec3 V = normalize(ViewVec);											//if(normal.x<0.9){gl_FragColor =  vec4(V,1);return;}
	vec3 R = reflect(-L, normal);											//gl_FragColor =  vec4(R,1);return;
	vec3 H = normalize( L + V );											//gl_FragColor =  vec4(H,1);return;
	
	float NdotL = max( dot(normal, L), FLT_EPSILON );
	float NdotH = max( dot(normal, H), FLT_EPSILON );
	float NdotV = max( dot(normal, V), FLT_EPSILON );
	float LdotH = max( dot(L, H), FLT_EPSILON );
	float NdotNegL = max( dot(normal, -L), FLT_EPSILON );

	vec4 color;
	vec3 albedo = baseMap.rgb * C.rgb;										//gl_FragColor =  vec4(albedo,1);return;
	
	vec3 diffuse = A.rgb + (D.rgb * NdotL);									//gl_FragColor =  vec4(diffuse,1);return;
	
	if ( bool(greyscaleColor)) {
		vec4 luG = colorLookup( baseMap.g, C.g * paletteScale );
		albedo = luG.rgb;													//if(albedo.r > 0.2){gl_FragColor =  vec4(albedo,1);return;}//signal!
	}
	
	// Emissive
	vec3 emissive = vec3(0.0);
	if ( bool(hasEmit) ) {
		emissive += glowColor * glowMult;									//if(emissive.r > 0.2){gl_FragColor =  vec4(emissive,1);return;}//signal!
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
		spec = clamp( spec, 0.0, 1.0 );										//gl_FragColor =  vec4(spec,1);return;
	}
	
	// Environment
	// TODO: why does textureCube not work on Android? check again is does seem to
	
	//FIXME: I notice sk_env also has same problem, possibly all cubes are bum
	// note carefully that bsa display does not show all problems
	// notice I'm runnig hasEnvMask and it seems to work now
	vec3 reflected = reflect( V, normal );
	vec3 reflectedVS = b * reflected.x + t * reflected.y + N * reflected.z;
	vec3 reflectedWS = vec3( glModelMatrix * (glModelViewMatrixInverse * vec4( reflectedVS, 0.0 )) );

	//FIXEM: can't get the textureCube() to return a value
	if ( bool(hasCubeMap) ) {												//gl_FragColor =  vec4(0,1,1,1);return;//signal!
		// gles doesn't have this in the frag shader 
		//vec4 cube = textureCubeLod( CubeMap, reflectedWS, 8.0 - g * 8.0 );			//gl_FragColor =  vec4(cube.rgb,1);return;
		vec4 cube = textureCube( CubeMap, reflectedWS );				
		
	
		cube.rgb *= envReflection * specStrength * sqrt(g) * 0.9;
			
		//disabled because sk_env had trouble with this
		//vec4 env = texture2D( EnvironmentMap, offset );		
		//cube.rgb *= mix( s, env.r, float(hasEnvMask) );							
    
		albedo += cube.rgb;
	}


	vec3 backlight = vec3(0.0);
	if ( bool(hasBacklight) ) {											//gl_FragColor =  vec4(1,1,1,1);return;//signal!
		backlight = texture2D( BacklightMap, offset ).rgb;
		backlight *= NdotNegL;
		
		emissive += backlight * D.rgb;									//gl_FragColor =  vec4(D.rgb,1);return;//signal!
	}
	
 
	vec4 mask = vec4(0.0);
	if ( bool(hasRimlight) || bool(hasSoftlight) ) {					
		mask = vec4( s );												//gl_FragColor =  vec4(mask.rgb,1);return;
	}


	vec3 rim = vec3(0.0);
	if ( bool(hasRimlight) ) {											
		rim = mask.rgb * pow(vec3((1.0 - NdotV)), vec3(rimPower));
		rim *= smoothstep( -0.2, 1.0, dot(-L, V) );
		
		emissive += rim * D.rgb;										//gl_FragColor =  vec4(emissive.rgb,1);return;
	}

	
	vec3 soft = vec3(0.0);
	if ( bool(hasSoftlight) ) {											
		float wrap = (dot(normal, L) + lightingEffect1) / (1.0 + lightingEffect1);
    
		soft = max( wrap, 0.0 ) * mask.rgb * smoothstep( 1.0, 0.0, NdotL );
		soft *= sqrt( clamp( lightingEffect1, 0.0, 1.0 ) );
		
		emissive += soft * D.rgb;											//gl_FragColor =  vec4(emissive,1);return;
	}


	color.rgb = albedo * (diffuse + emissive);	
	color.rgb += spec;
//	color.rgb = tonemap( color.rgb ) / tonemap( vec3(1.0) );   //tonemaps do weirds things sometimes

	color.a = C.a * baseMap.a;

	gl_FragColor = color;
	gl_FragColor.a *= alpha; 
}
