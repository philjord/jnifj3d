package nif.shader;

import java.util.ArrayList;
import java.util.HashSet;

import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.Shader;

public class GLSLShaderProgram2 extends GLSLShaderProgram {
	public static boolean		ALLOW_ANY_UNIFORM_NAME		= false;
	public String				name						= "";
	private HashSet<String>	freeformShaderUniformNames	= new HashSet<String>();

	/**
	 * Assume GLSLSourceCodeShader and attempt to set shader vertex attribute names and uniform names
	 */
	@Override
	public void setShaders(Shader[] shaders) {
		super.setShaders(shaders);

		this.setCapability(GLSLShaderProgram.ALLOW_SHADERS_READ);

		HashSet<String> allShaderUniformNames;//HashSet, never add the same uniform twice or chaos
		if (ALLOW_ANY_UNIFORM_NAME) {
			allShaderUniformNames = freeformShaderUniformNames;
		} else {		
			allShaderUniformNames = new HashSet<String>();
			for (Shader s : shaders) {
				allShaderUniformNames.addAll(((GLSLSourceCodeShader)s).shaderUniformNames);
			}
		}
		String[] shaderAttrNames = allShaderUniformNames.toArray(new String[allShaderUniformNames.size()]);
		setShaderAttrNames(shaderAttrNames);

		ArrayList<String> allShaderVertexAttributeNames = new ArrayList<String>();
		for (Shader s : shaders) {
			allShaderVertexAttributeNames.addAll(((GLSLSourceCodeShader)s).shaderVertexAttributeNames);
		}
		String[] shaderVertexAttributeNames = allShaderVertexAttributeNames
				.toArray(new String[allShaderVertexAttributeNames.size()]);
		setVertexAttrNames(shaderVertexAttributeNames);
	}

	/**
	 * For use if the shader is complex and we just fire code binding at it and see what the compiler says
	 * @param var
	 */
	protected void addUniformName(String var) {
		freeformShaderUniformNames.add(var);

		if (!ALLOW_ANY_UNIFORM_NAME) {
			System.out.println("GLSLSourceCodeShader.ALLOW_ANY_UNIFORM_NAME should be true when calling addUniformName("
								+ var + ")");
		}
	}

	/**
	 * Apparently shader attributes can only be set if the shader code declares them otherwise a type mismatch error
	 * comes back, who knew. Also problem happen if TUS get set with a name so must check before, also shaders will
	 * compile away variables totally
	 * @param var
	 * @param val
	 * @return
	 */
	public boolean programHasVar(String var) {
		if (ALLOW_ANY_UNIFORM_NAME) {
			addUniformName(var);
			return true;
		} else {
			boolean isInCode = false;
			
			for (Shader s : getShaders()) {
				if (((GLSLSourceCodeShader)s).shaderHasVar(var)) {
					isInCode = true;
					break;
				}
			}
			
			//FIXME: skyrim default prog is showing lines like this!
			//var not parsed proper!! hasSpecularMap  isInNames:false  isInCode:false in this program GLSLShaderProgram2: sk_default.prog
			//var not parsed proper!! SpecularMap  isInNames:false  isInCode:false in this program GLSLShaderProgram2: sk_default.prog

	/*		
			if(isInNames ==false|| isInCode ==false) {
				System.err.println("var not parsed proper!! " + var + "  isInNames:" + isInNames + "  isInCode:" + isInCode+ " in this program " +this);
			}*/
			
			return isInCode;
		}
	}

	@Override
	public String toString() {
		return "GLSLShaderProgram2: " + name;
	}
}
