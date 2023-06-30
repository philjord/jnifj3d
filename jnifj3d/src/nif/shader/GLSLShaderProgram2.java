package nif.shader;

import java.util.ArrayList;

import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.Shader;

public class GLSLShaderProgram2 extends GLSLShaderProgram
{
	public String name = "";

	/**
	 * Assume GLSLSourceCodeShader and attempt to set shader vertex attribute names and uniform names
	 */
	@Override
	public void setShaders(Shader[] shaders)
	{
		super.setShaders(shaders);
		
		this.setCapability(GLSLShaderProgram.ALLOW_SHADERS_READ);

		ArrayList<String> allShaderUniformNames = new ArrayList<String>();
		for (Shader s : shaders)
		{
			allShaderUniformNames.addAll(((GLSLSourceCodeShader) s).shaderUniformNames);
		}
		String[] shaderAttrNames = allShaderUniformNames.toArray(new String[allShaderUniformNames.size()]);
		setShaderAttrNames(shaderAttrNames);

		ArrayList<String> allShaderVertexAttributeNames = new ArrayList<String>();
		for (Shader s : shaders)
		{
			allShaderVertexAttributeNames.addAll(((GLSLSourceCodeShader) s).shaderVertexAttributeNames);
		}
		String[] shaderVertexAttributeNames = allShaderVertexAttributeNames.toArray(new String[allShaderVertexAttributeNames.size()]);
		setVertexAttrNames(shaderVertexAttributeNames);
	}

	/**
	 * Apparently shader attributes can only be set if the shader code declares them otherwise 
	 * a type mismatch error comes back, who knew. Also problem happen if TUS get set with a name
	 * so must check before, also shaders will compile away variables totally
	 * @param var
	 * @param val
	 * @return
	 */
	public boolean programHasVar(String var)
	{
		for (Shader s : getShaders())
		{
			if (((GLSLSourceCodeShader) s).shaderHasVar(var))
				return true;
		}
		return false;
	}

	public boolean programHasVar(String var, Object val)
	{
		for (Shader s : getShaders())
		{
			//type checking not in fact done so use faster call
			if (((GLSLSourceCodeShader) s).shaderHasVar(var))
				return true;
			//if (((GLSLSourceCodeShader) s).shaderHasVar(var, val.getClass().getSimpleName()))
			//	return true;
		}
		return false;
	}

	public boolean programHasVar(String var, Object val, int arrSize)
	{
		for (Shader s : getShaders())
		{
			if (((GLSLSourceCodeShader) s).shaderHasVar(var, val.getClass().getSimpleName(), arrSize))
				return true;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "GLSLShaderProgram2: " + name;
	}
}
