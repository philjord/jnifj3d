#version 120

attribute vec4 glVertex;         
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

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

uniform mat4 textureTransform;
//End of FFP inputs
varying vec2 glTexCoord0;

attribute vec3 tangent;
attribute vec3 binormal;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 ColorEA;
varying vec4 ColorD;

varying vec3 N;
varying vec3 t;
varying vec3 b;
varying vec3 v;


void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).st;	
	
	N = normalize(glNormalMatrix * glNormal);
	t = normalize(glNormalMatrix * tangent);
	b = normalize(glNormalMatrix * binormal);
	
	// NOTE: b<->t 
	mat3 tbnMatrix = mat3(b.x, t.x, N.x, 
                          b.y, t.y, N.y,
                          b.z, t.z, N.z);
						  
	v = vec3(glModelViewMatrix * glVertex);
	
	ViewVec = tbnMatrix * -v.xyz;
	LightDir = tbnMatrix * glLightSource[0].position.xyz;
	
	ColorEA = glFrontMaterial.emission + glLightModelambient * A;
	ColorD = glFrontMaterial.diffuse * D;
}