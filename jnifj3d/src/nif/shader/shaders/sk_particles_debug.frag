#version 140

precision mediump float;
 
//End of FFP inputs
in vec2 glTexCoord0; // represents only the start of the uv, does not vary across the particle

uniform float transparencyAlpha;	

uniform sampler2D BaseMap;
uniform sampler2D GreyscaleMap;

uniform bool hasGreyscaleMap;
 
in mediump vec2 TextureSize;

in vec4 C;
in mediump mat3 v_rotationMatrix;

out vec4 glFragColor;

float sqrt2 = sqrt(2.0);

vec4 colorLookup( float x, float y ) {	
	return texture( GreyscaleMap, vec2( clamp(x, 0.0, 1.0), clamp(y, 0.0, 1.0)) );
}

void main( void )
{	
	// we need to increase the texcoord so it's a smaller square inside the unit square
	mediump vec2  unrotatedUV = gl_PointCoord; //unit 0-1	
	// reduce the texcoords to be a smaller square by sampling a larger range	
	unrotatedUV *= (sqrt2 * TextureSize);
	
										
	// now recenter that new smaller square
	unrotatedUV += (TextureSize * 0.5) - ((sqrt2 * TextureSize) * 0.5); 
	mediump vec3 rotCoord = v_rotationMatrix * vec3((unrotatedUV), 1.0); 	
		
	// move across to the actual in use sub texture
 	mediump vec2 realTexCoord = glTexCoord0 + rotCoord.st;   
 
 	//rotated textures go outside the bounds of 0-1 so discard anything else
 	//If it falls outside the sub texture range discard it, sub texture start at glTexCoord0 and has stride of TextureSize
 	//if( realTexCoord.s < glTexCoord0.s || realTexCoord.s > glTexCoord0 + TextureSize.s 
 	// || realTexCoord.t < glTexCoord0.t || realTexCoord.t > glTexCoord0 + TextureSize.t) {
 	// but it seems clearer to check the rotCoord adjustment values
 	if(rotCoord.s < 0.0 || rotCoord.t < 0.0  ||  rotCoord.s > TextureSize.s  || rotCoord.t > TextureSize.t) { 
 		discard;		
	}
 	   
    //Taken from sk_effectshader, very roughly
	mediump vec4 baseMap = texture(BaseMap, realTexCoord.st); 
		
		
	vec4 color;
	color.rgb = baseMap.rgb;
	color.a = baseMap.a;
																																						
	color.rgb *= C.rgb;
	color.a *= C.a;

	if(hasGreyscaleMap) {
		vec4 luG = colorLookup( baseMap.g, C.g );
		color.rgb = luG.rgb; 
	}
	
	glFragColor.rgb = color.rgb;
	glFragColor.a = color.a;


																	// if(hasGreyscaleMap) 
   																	//		glFragColor = vec4(1,0,1,1);
   																
   																	 
}
