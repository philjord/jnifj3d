#version 150 
//#version 120 is not optional, trouble otherwise

//Note don't put if else constructs on one line or trouble

in vec4 glVertex;         
in vec4 glColor;       
//in vec3 glNormal;     
in vec2 glMultiTexCoord0; 

uniform mat4 glProjectionMatrix;
//uniform mat4 glProjectionMatrixInverse;
uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;

uniform int ignoreVertexColors;
	
uniform mat4 textureTransform;
//End of FFP inputs

in float Size;
in float Rotation;

out mat3 v_rotationMatrix;
out vec4 C;

// The size of the sprite being rendered as a sub texture for an atlas. 
in vec2 SubTextureSize;

out vec2 glTexCoord0;
out vec2 TextureSize;

 
uniform float screenWidth;      //screen width in pixels

void main( void )
{
	mat4 glModelViewMatrix = glViewMatrix * glModelMatrix;
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;
	
	//just hand these vertex attributes across to the frag shader direct
	glTexCoord0 = glMultiTexCoord0;
	TextureSize = SubTextureSize;
	
	vec4 v2 =  glModelViewMatrix * glVertex;
	vec4 projCorner = glProjectionMatrix * vec4(0.5*Size, 0.5*Size, v2.z, v2.w);
	gl_PointSize = screenWidth * projCorner.x / projCorner.w;
	
	if(ignoreVertexColors == 0)
		C = glColor; 
	else
		C = vec4(1.0, 1.0, 1.0, 1.0);
	
	//Expensive??
	float cos = cos(Rotation);
    float sin = sin(Rotation);
    // rotation matrix is just for the UV coords, so mix the atlas sub texture size in
    v_rotationMatrix = mat3(cos, sin, 0.0,
                        -sin, cos, 0.0,
                        (sin-cos+1.0)*SubTextureSize.s*0.5, (-sin-cos+1.0)*SubTextureSize.t*0.5, 1.0);
                        //double check that s and t are in the right slots
                        

}
