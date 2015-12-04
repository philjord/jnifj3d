package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import nif.enums.TargetColor;
import nif.j3d.J3dNiAVObject;
import nif.niobject.controller.NiMaterialColorController;

public class J3dNiMaterialColorController extends J3dNiTimeController
{
	private ArrayList<Material> targetMaterials = new ArrayList<Material>();

	private int targetColor = TargetColor.TC_DIFFUSE;

	public J3dNiMaterialColorController(NiMaterialColorController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);

		targetColor = controller.targetColor.color;
		for (int i = 0; i < nodeTarget.numChildren(); i++)
		{
			if (nodeTarget.getChild(i) instanceof Shape3D)
			{
				Shape3D shape = (Shape3D) nodeTarget.getChild(i);

				Material targetMaterial = shape.getAppearance().getMaterial();
				if (targetMaterial == null)
				{
					targetMaterial = new Material();
					shape.getAppearance().setMaterial(targetMaterial);
				}
				targetMaterial.setCapability(Material.ALLOW_COMPONENT_WRITE);
				targetMaterials.add(targetMaterial);
			}
		}

	}

	public void updateColor(Color3f newColor)
	{
		for (Material targetMaterial : targetMaterials)
		{
			if (targetColor == TargetColor.TC_DIFFUSE)
			{
				targetMaterial.setDiffuseColor(newColor);
			}
			else if (targetColor == TargetColor.TC_AMBIENT)
			{
				targetMaterial.setAmbientColor(newColor);
			}
			else if (targetColor == TargetColor.TC_SELF_ILLUM)
			{
				targetMaterial.setEmissiveColor(newColor);
			}
			else if (targetColor == TargetColor.TC_SPECULAR)
			{
				targetMaterial.setSpecularColor(newColor);
			}
		}
	}

	private Color3f c = new Color3f();//deburner

	@Override
	public void update(Point3f value)
	{
		c.set(value);
		updateColor(c);
	}

	@Override
	public void update(float value)
	{
		c.set(value, value, value);
		updateColor(c);
	}

}
