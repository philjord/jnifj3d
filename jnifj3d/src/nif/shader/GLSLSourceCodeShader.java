package nif.shader;

import java.util.ArrayList;
import java.util.HashSet;

import org.jogamp.java3d.SourceCodeShader;

public class GLSLSourceCodeShader extends SourceCodeShader {

	public HashSet<String>	shaderUniformNames			= new HashSet<String>();
	public HashSet<String>	shaderVertexAttributeNames	= new HashSet<String>();
	
	public String shaderSource = "";

	public GLSLSourceCodeShader(int shadingLanguage, int shaderType, String name, String shaderSource) {
		super(shadingLanguage, shaderType, shaderSource);
		setName(name);
		this.shaderSource = shaderSource.replace("\t", " ");// just to make white space easier to check

		//attempt to extract attribute names very poorly
		//start by discarding everything before //End of FFP inputs
		String allCode = shaderSource;
		if (allCode.contains("//End of FFP inputs"))
			allCode = allCode.substring(allCode.indexOf("//End of FFP inputs") + "//End of FFP inputs".length());

		String[] declarations = allCode.split("\n");
		for (String codeLine : declarations) {
			// chuck away any comment parts at the end of the line
			if (codeLine.contains("//"))
				codeLine = codeLine.substring(0, codeLine.indexOf("//")).trim();
			// TODO: cross fingers there is nothing in /**/ style comments

			if (codeLine.trim().startsWith("uniform") && !GLSLShaderProgram2.ALLOW_ANY_UNIFORM_NAME) {
				// find start of name after type
				String line = codeLine.substring(codeLine.lastIndexOf(" ") + 1);
				// drop final ;
				line = line.replace(";", "").trim();
				shaderUniformNames.add(line);
			} else if (codeLine.trim().startsWith("attribute")) {
				// find start of name after type
				String line = codeLine.substring(codeLine.indexOf(" ", codeLine.indexOf(" ") + 1) + 1);
				// drop final ;
				line = line.replace(";", "").trim();
				shaderVertexAttributeNames.add(line);
			}
		}
	}

	@Override
	public String toString() {
		return "SourceCodeShader2: " + getName();
	}



	@Override
	public String getShaderSource() {
		return shaderSource;
	}
	
	// I've got 2 competing systems here! Either assume all variables are fine, or badly parse the code to get a few variables
	public boolean shaderHasVar(String var) {
		if (GLSLShaderProgram2.ALLOW_ANY_UNIFORM_NAME) {
			System.out.println("GLSLSourceCodeShader.ALLOW_ANY_UNIFORM_NAME should be false when calling shaderHasVar(" + var + ")");
			return true;
		} else {
			return shaderSource.contains(" " + var + ";");
		}
	}

	

	public static String[][] replacements = new String[][] { //			
		{"gl_ProjectionMatrix", "glProjectionMatrix"}, //
		{"gl_ProjectionMatrixInverse", "glProjectionMatrixInverse"}, //
		{"gl_ModelViewMatrix", "glModelViewMatrix"}, //
		{"gl_ModelViewMatrixInverse", "glModelViewMatrixInverse"}, //
		{"gl_ModelViewProjectionMatrix", "glModelViewProjectionMatrix"}, //
		{"gl_Vertex", "glVertex"}, //
		{"gl_Normal", "glNormal"}, //
		{"gl_Color", "glColor"}, //
		{"gl_SecondaryColor", "No option research"}, //
		{"gl_TextureMatrix", "textureTransform with no texture units"},
		{"gl_MultiTexCoord", "glMultiTexCoord* where * is texture unit number"}, //
		{"gl_TexCoord", "manual varying now (like glTexCoord0)"}, //		
		{"gl_FrontColor", "manual varying now"}, //			
		{"gl_FogCoord", "glFogCoord not sure about this yet"}, //
		{"gl_LightSource", "glLightSource"}, //
		{"gl_FrontMaterial", "glFrontMaterial"}, //
		{"gl_BackMaterial", "No option research"}, //
		{"gl_FrontLightModelProduct", "No option research"}, //
		{"gl_BackLightModelProduct", "No option research"}, //
	};

	/**
	 * list of suggested replacements
	 * 
	 */
	public static ArrayList<String> testForFFP(String ss) {
		ArrayList<String> ret = new ArrayList<String>();
		for (int i = 0; i < replacements.length; i++) {
			String[] rep = replacements[i];
			if (ss.contains(rep[0]))
				ret.add(rep[0] + " should be replaced with " + rep[1]);
		}
		return ret;

	}
}
