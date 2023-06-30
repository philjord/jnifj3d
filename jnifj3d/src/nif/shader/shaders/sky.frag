#version 120

precision mediump float;

varying vec2 glTexCoord0;
uniform sampler2D BaseMap;

varying float azi;

void main( void )
{
	vec4 baseMap = texture2D( BaseMap, glTexCoord0.st );
	//gl_FragColor = baseMap;
	
 	gl_FragColor = mix(baseMap,vec4(0.8,0.8,0.8,1),(1.0-azi)+0.05);
} 