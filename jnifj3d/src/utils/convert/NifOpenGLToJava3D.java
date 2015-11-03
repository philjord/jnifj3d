package utils.convert;

import javax.media.j3d.Material;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.TransparencyAttributes;

import nif.enums.CompareMode;
import nif.enums.StencilAction;
import nif.enums.VertMode;
import nif.niobject.NiAlphaProperty;

public class NifOpenGLToJava3D
{
	public static int convertBlendMode(int in, boolean isSource)
	{
		if (in == NiAlphaProperty.GL_ONE)
			return TransparencyAttributes.BLEND_ONE;
		else if (in == NiAlphaProperty.GL_ZERO)
			return TransparencyAttributes.BLEND_ZERO;
		else if (in == NiAlphaProperty.GL_SRC_COLOR)
		{
			return isSource ? TransparencyAttributes.BLEND_SRC_ALPHA : TransparencyAttributes.BLEND_SRC_COLOR;// haha illegal for source blend			
		}
		else if (in == NiAlphaProperty.GL_ONE_MINUS_SRC_COLOR)
			return TransparencyAttributes.BLEND_ONE_MINUS_SRC_COLOR;
		else if (in == NiAlphaProperty.GL_DST_COLOR)
			return TransparencyAttributes.BLEND_DST_COLOR;
		else if (in == NiAlphaProperty.GL_ONE_MINUS_DST_COLOR)
			return TransparencyAttributes.BLEND_ONE_MINUS_DST_COLOR;
		else if (in == NiAlphaProperty.GL_SRC_ALPHA)
			return TransparencyAttributes.BLEND_SRC_ALPHA;
		else if (in == NiAlphaProperty.GL_ONE_MINUS_SRC_ALPHA)
			return TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA;
		else
		{
			System.out.println("default used for convertBlendMode in=" + in);
			return TransparencyAttributes.BLEND_SRC_ALPHA;
		}

		//Possibly these are very rare? TransparencyAttributes doesn't have them
		//nap.sourceBlendMode() == NiAlphaProperty.GL_DST_ALPHA)
		//nap.sourceBlendMode() == NiAlphaProperty.GL_ONE_MINUS_DST_ALPHA)
		//nap.sourceBlendMode() == NiAlphaProperty.GL_SRC_ALPHA_SATURATE)
	}

	public static int convertAlphaTestMode(int in)
	{
		//Note possiblyflags in morrowind are different, only time I get 000
		if (in == NiAlphaProperty.GL_ALWAYS)
			return RenderingAttributes.LESS;//ALWAYS would be mental, disables Z buffer
			//return RenderingAttributes.ALWAYS;
		else if (in == NiAlphaProperty.GL_LESS)
			return RenderingAttributes.LESS;
		else if (in == NiAlphaProperty.GL_EQUAL)
			return RenderingAttributes.EQUAL;
		else if (in == NiAlphaProperty.GL_LEQUAL)
			return RenderingAttributes.LESS_OR_EQUAL;
		else if (in == NiAlphaProperty.GL_GREATER)
			return RenderingAttributes.GREATER;
		else if (in == NiAlphaProperty.GL_NOTEQUAL)
			return RenderingAttributes.NOT_EQUAL;
		else if (in == NiAlphaProperty.GL_GEQUAL)
			return RenderingAttributes.GREATER_OR_EQUAL;
		else if (in == NiAlphaProperty.GL_NEVER)
			return RenderingAttributes.NEVER;
		else
		{
			System.out.println("default used for convertAlphaTestMode in=" + in);
			return RenderingAttributes.ALWAYS;
		}
	}

	public static int convertStencilFunction(int in)
	{
		if (in == CompareMode.TEST_NEVER)
			return RenderingAttributes.NEVER;
		else if (in == CompareMode.TEST_LESS)
			return RenderingAttributes.LESS;
		else if (in == CompareMode.TEST_EQUAL)
			return RenderingAttributes.EQUAL;
		else if (in == CompareMode.TEST_LESS_EQUAL)
			return RenderingAttributes.LESS_OR_EQUAL;
		else if (in == CompareMode.TEST_GREATER)
			return RenderingAttributes.GREATER;
		else if (in == CompareMode.TEST_NOT_EQUAL)
			return RenderingAttributes.NOT_EQUAL;
		else if (in == CompareMode.TEST_GREATER_EQUAL)
			return RenderingAttributes.GREATER_OR_EQUAL;
		else if (in == CompareMode.TEST_ALWAYS)
			return RenderingAttributes.ALWAYS;
		else
		{
			System.out.println("default used for convertStencilFunction in=" + in);
			return RenderingAttributes.ALWAYS;
		}

	}

	public static int convertStencilAction(int in)
	{
		if (in == StencilAction.ACTION_KEEP)
			return RenderingAttributes.STENCIL_KEEP;
		else if (in == StencilAction.ACTION_ZERO)
			return RenderingAttributes.STENCIL_ZERO;
		else if (in == StencilAction.ACTION_REPLACE)
			return RenderingAttributes.STENCIL_REPLACE;
		else if (in == StencilAction.ACTION_INCREMENT)
			return RenderingAttributes.STENCIL_INCR;
		else if (in == StencilAction.ACTION_DECREMENT)
			return RenderingAttributes.STENCIL_DECR;
		else if (in == StencilAction.ACTION_INVERT)
			return RenderingAttributes.STENCIL_INVERT;
		else
		{
			System.out.println("default used for convertStencilAction in=" + in);
			return RenderingAttributes.STENCIL_KEEP;
		}
	}

	public static int convertVertexMode(int in)
	{
		if (in == VertMode.VERT_MODE_SRC_IGNORE)
			return Material.AMBIENT_AND_DIFFUSE; // there is no none flag renderingattributes has an ignore vertex colors option
		else if (in == VertMode.VERT_MODE_SRC_EMISSIVE)
			return Material.EMISSIVE;
		else if (in == VertMode.VERT_MODE_SRC_AMB_DIF)
			return Material.AMBIENT_AND_DIFFUSE;
		else
		{
			System.out.println("default used for convertVertexMode in=" + in);
			return Material.AMBIENT_AND_DIFFUSE;
		}
	}
}
