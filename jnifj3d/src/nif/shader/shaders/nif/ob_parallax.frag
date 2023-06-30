#version 120

precision highp float; // must be highp if any uniform (like lightSource) is also in the vertex shader
precision highp int;

struct material
{
	int lightEnabled;
 	vec4 ambient;
 	vec4 diffuse;
 	vec4 emission; 
 	vec3 specular;
 	float shininess;
};
uniform material glFrontMaterial;

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

varying vec2 glTexCoord0;

uniform sampler2D BaseMap;
uniform sampler2D NormalMap;

varying vec3 LightDir;
varying vec3 HalfVector;
varying vec3 ViewVec;

varying vec4 ColorEA;
varying vec4 ColorD;

varying vec3 N;

void main( void )
{
	float offset = 0.015 - texture2D( BaseMap, glTexCoord0.st ).a * 0.03;
	vec2 texco = glTexCoord0.st + normalize( ViewVec ).xy * offset;
	
	vec4 color = ColorEA;

	vec4 normal = texture2D( NormalMap, texco );
	normal.rgb = normal.rgb * 2.0 - 1.0;
	
	float NdotL = max( dot( normal.rgb, normalize( LightDir ) ), 0.0 );
	
	if ( NdotL > 0.0 )
	{
		color += ColorD * NdotL;
		float NdotHV = max( dot( normal.rgb, normalize( HalfVector ) ), 0.0 );
		color += normal.a * glFrontMaterial.specular * glLightSource[0].specular.rgb * pow( NdotHV, glFrontMaterial.shininess );
	}
	
	color = min( color, 1.0 );
	color *= texture2D( BaseMap, texco );
	
	gl_FragColor = color;
}
