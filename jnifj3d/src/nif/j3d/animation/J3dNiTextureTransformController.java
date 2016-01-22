package nif.j3d.animation;

import javax.media.j3d.Appearance;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import nif.enums.TexTransform;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.animation.j3dinterp.interp.FloatInterpolator;
import nif.niobject.controller.NiTextureTransformController;
import nif.niobject.controller.NiTimeController;

public class J3dNiTextureTransformController extends J3dNiTimeController implements FloatInterpolator.Listener
{
	private TextureAttributes textureAttributes;

	private int operation;

	private Transform3D transform = new Transform3D();

	/**
	 * 
	 * @param controller
	 * @param nodeTarget
	 */
	public J3dNiTextureTransformController(NiTextureTransformController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);
		operation = controller.operation.transformType;
		config();
	}

	/**
	 * FOR TES3 only 
	 * @param controller
	 * @param nodeTarget
	 */
	public J3dNiTextureTransformController(NiTimeController controller, J3dNiAVObject nodeTarget, int operation)
	{
		super(controller, nodeTarget);
		this.operation = operation;
		config();
	}

	private void config()
	{
		if (nodeTarget instanceof J3dNiGeometry)
		{
			J3dNiGeometry j3dNiGeometry = (J3dNiGeometry) nodeTarget;
			Appearance app = j3dNiGeometry.getShape().getAppearance();
			if (app.getTextureUnitCount() > 0)
			{
				// note these MUST be shared so updating one updates for all TUS
				textureAttributes = app.getTextureUnitState(0).getTextureAttributes();
				if (textureAttributes == null)
				{
					textureAttributes = new TextureAttributes();
					for (int i = 0; i < app.getTextureUnitCount(); i++)
					{
						app.getTextureUnitState(i).setTextureAttributes(textureAttributes);
					}
				}

				if (!textureAttributes.getCapability(TextureAttributes.ALLOW_TRANSFORM_WRITE))
				{
					textureAttributes.setCapability(TextureAttributes.ALLOW_TRANSFORM_WRITE);
				}

				// for those special cases output the incomplete operation support
				if (operation == TexTransform.TT_SCALE_U)
				{
					System.out.println("texture transform.setScale(u) spotted in " + niTimeController.nVer.fileName);
				}
				else if (operation == TexTransform.TT_SCALE_V)
				{
					System.out.println("texture transform.setScale(v) spotted in  " + niTimeController.nVer.fileName);
				}
				else if (operation == TexTransform.TT_ROTATE)
				{
					System.out.println("rotate in spotted " + niTimeController.nVer.fileName);
				}

			}
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
			if (operation == TexTransform.TT_TRANSLATE_U)
			{
				transform.get(t);
				t.x = -value;
				transform.setTranslation(t);
			}
			else if (operation == TexTransform.TT_TRANSLATE_V)
			{
				transform.get(t);
				t.y = value;
				transform.setTranslation(t);
			}
			else if (operation == TexTransform.TT_SCALE_U)
			{
				//TODO: removed as particle atlas animated textures don't like being interpolated by non powers of 2
				transform.setScale(new Vector3d(0d, value, 0d));
			}
			else if (operation == TexTransform.TT_SCALE_V)
			{
				//TODO: removed as particle atlas animated textures don't like being interpolated by non powers of 2
				transform.setScale(new Vector3d(value, 0d, 0d));
			}
			else if (operation == TexTransform.TT_ROTATE)
			{
				AxisAngle4f aa = new AxisAngle4f(0, 0, -1, value);
				transform.setRotation(aa);

				//TODO:  transforms apply to the texcoords so the -0.5 should work , but I can't confirm it, needs testing
				//TT_ROTATE improve this as it's rotating around bottom left corner
				// see E:\game media\Oblivion\meshes\effects\lichbloodspray.nif
				transform.get(t);
				t.x = -0.5f;
				t.y = -0.5f;
				transform.setTranslation(t);

			}
			else
			{
				//ignore
			}

			textureAttributes.setTextureTransform(transform);
		}
	}

}
