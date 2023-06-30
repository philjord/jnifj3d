#version 120

attribute vec4 glVertex;
attribute vec4 glColor;
attribute vec2 glMultiTexCoord0; 

attribute vec4 samplers0;
attribute vec4 samplers1;
attribute vec4 samplers2;
attribute vec4 samplers3;

uniform mat4 glProjectionMatrix;
uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;
uniform mat4 glModelViewMatrix;
//uniform mat4 glModelViewProjectionMatrix;
//uniform mat3 glNormalMatrix;

 
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

varying vec2 glTexCoord0;

varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec4 fragSamplers0;
varying vec4 fragSamplers1;
varying vec4 fragSamplers2;
varying vec4 fragSamplers3;
 

void main( void )
{	
	mat4 glModelViewMatrix = glViewMatrix*glModelMatrix;
	gl_Position = glProjectionMatrix*glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = glMultiTexCoord0.st; 
	
	vec3 v = vec3(glModelViewMatrix * glVertex);
	ViewVec = -v.xyz;
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0 ) 
		C = glFrontMaterial.diffuse; 
	else 
		C = glColor;
	D = glLightSource[0].diffuse * glFrontMaterial.diffuse;
	 
	
	fragSamplers0 = samplers0;
	fragSamplers1 = samplers1;
	fragSamplers2 = samplers2; 	
	fragSamplers3 = samplers3; 	
	//TODO: I could work out the right ones here now and pass the samplers across to the frag as varying
	// or maybe not need to check
	
}
