#version 150 

in vec4 glVertex;         
in vec4 glColor;       
in vec2 glMultiTexCoord0; 

uniform mat4 glProjectionMatrix;
uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;

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

uniform int ignoreVertexColors;

uniform mat4 textureTransform;

out vec2 glTexCoord0;

out vec3 ViewVec;
out vec4 C;


void main( void )
{
	mat4 glModelViewMatrix = glViewMatrix*glModelMatrix;// calculated here to reduce transer from CPU
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).st;		
		
	vec3 v = vec3(glModelViewMatrix * glVertex);
	ViewVec = -v.xyz;// do not normalize also used for view dist	
	 
	if( ignoreVertexColors != 0) 
	{
		// objectColor should be used if it is no lighting, and reusing material diffuse appears wrong
		C = glFrontMaterial.diffuse;
	}
	else 
		C = glColor; 				 
}
