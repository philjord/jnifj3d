#version 140

precision mediump float;
 
//End of FFP inputs
in vec2 glTexCoord0;

uniform float transparencyAlpha;	

uniform sampler2D BaseMap;
 
in mediump vec2 TextureSize;

in vec4 C;
in mat3 v_rotationMatrix;

out vec4 glFragColor;


void main( void )
{	 
	//mediump vec2 realTexCoord = glTexCoord0 + (gl_PointCoord * TextureSize);
	mediump vec2 realTexCoord = gl_PointCoord;
	mediump vec3 rotTexCoord = v_rotationMatrix * vec3(realTexCoord, 1.0);
    mediump vec4 fragColor = texture(BaseMap, rotTexCoord.st ); 



    glFragColor = fragColor * C;
    glFragColor.a *= transparencyAlpha;
   
}
