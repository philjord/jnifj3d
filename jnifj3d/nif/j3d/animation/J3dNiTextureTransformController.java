package nif.j3d.animation;

import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3f;

import nif.enums.TexTransform;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.interp.FloatInterpolator;
import nif.niobject.controller.NiTextureTransformController;

public class J3dNiTextureTransformController extends J3dNiTimeController implements FloatInterpolator.Listener
{
	private TextureAttributes textureAttributes;

	private TexTransform operation;

	private Transform3D transform = new Transform3D();

	public J3dNiTextureTransformController(NiTextureTransformController controller, J3dNiAVObject nodeTarget)
	{
		super(controller);
		if (nodeTarget instanceof J3dNiGeometry)
		{
			J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) nodeTarget;
			operation = controller.operation;
			textureAttributes = j3dNiGeometry.getShape().getAppearance().getTextureAttributes();
			if (textureAttributes == null)
			{
				textureAttributes = new TextureAttributes();
				j3dNiGeometry.getShape().getAppearance().setTextureAttributes(textureAttributes);
			}
			if (!textureAttributes.getCapability(TextureAttributes.ALLOW_TRANSFORM_WRITE))
				textureAttributes.setCapability(TextureAttributes.ALLOW_TRANSFORM_WRITE);
		}
		else
		{
			System.out.println("bad target for J3dNiTextureTransformController " + nodeTarget);
		}

	}

	private Vector3f t = new Vector3f(); //deburner

	@Override
	public void update(float value)
	{
		if (textureAttributes != null)
		{
			if (operation.transform == TexTransform.TT_TRANSLATE_U)
			{
				transform.get(t);
				t.x = -value;
				transform.setTranslation(t);
			}
			else if (operation.transform == TexTransform.TT_TRANSLATE_V)
			{
				transform.get(t);
				t.y = -value;
				transform.setTranslation(t);
			}
			else if (operation.transform == TexTransform.TT_SCALE_U)
			{
				//TODO: this can only do uniform scales for now
				//TODO: removed as particle animated textures don't like being interpolated by non powers of 2
				//			transform.setScale(value);
			}
			else if (operation.transform == TexTransform.TT_SCALE_V)
			{
				//TODO: this can only do uniform scales for now
				//TODO: removed as particle animated textures don't like being interpolated by non powers of 2
				//			transform.setScale(value);
			}
			else if (operation.transform == TexTransform.TT_ROTATE)
			{
				//TODO: TT_ROTATE
			}
			else
			{
				System.out.println("J3dNiTextureTransformController - unsupported operation value : " + operation.transform);
			}

			textureAttributes.setTextureTransform(transform);
		}
	}

}
