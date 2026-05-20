#version 120
//#version 410 core


attribute vec4 glVertex;  
attribute vec4 glColor;    
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 
attribute vec2 glMultiTexCoord1; 
       

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
//End of FFP inputs - note all attributes above this line are discarded in GLSLSourceCodeShader!



// FROM https://github.com/fo76utils/nifskope/tree/develop/res/shaders
 
varying vec3 LightDir;
varying vec3 ViewDir;

varying vec4 texCoord;

varying mat3 btnMatrix;

varying vec4 C;

attribute vec3 tangent;// these attributes are fixed names
//attribute vec3 bitangentVector;

 
//ffp mat4	projectionMatrix;
//ffp vec4	lightSourcePosition[3];


//ffp uniform mat3 normalMatrix;			// in row-major order
//ffp uniform mat4 modelViewMatrix;

uniform vec4 vertexColorOverride;	// components greater than zero replace the vertex color

//ffp layout ( location = 0 ) in vec3 vertexPosition;
//ffp layout ( location = 1 ) in vec4 vertexColor;
//ffp layout ( location = 2 ) in vec3 normalVector;
//attributes layout ( location = 3 ) in vec3 tangentVector;
//attributes layout ( location = 4 ) in vec3 bitangentVector;
//ffp layout ( location = 7 ) in vec2 multiTexCoord0;
//ffp layout ( location = 8 ) in vec2 multiTexCoord1;

//not doing GPU skinning #include "bonetransform.glsl"

void main()
{
	vec4	v = vec4( glVertex.xyz, 1.0 );
	vec3	n = glNormal;
	vec3	t = tangent;
	vec3	b = cross(n,t);

	//Not doing GPU skinning 
	//if ( boneWeights[0].x > 0.0 && doSkinning )
	//	boneTransform( v, n, t, b );

	//v = modelViewMatrix * v;// both in one
	gl_Position = glModelViewProjectionMatrix * v; // both in one
	texCoord = vec4(glMultiTexCoord0, glMultiTexCoord1);

	btnMatrix[2] = normalize( n * glNormal );
	btnMatrix[1] = normalize( t * glNormal );
	btnMatrix[0] = normalize( b * glNormal );

	//No orthographic option
	//if ( projectionMatrix[3][3] == 1.0 )
	//	ViewDir = vec3(0.0, 0.0, 1.0);	// orthographic view
	//else
		ViewDir = -v.xyz;
	LightDir = glLightSource[0].position.xyz;
		
	C = mix( glColor, vertexColorOverride, vec4(greaterThan( vertexColorOverride, vec4( 0.0 ) )) );

}