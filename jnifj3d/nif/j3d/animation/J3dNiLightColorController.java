package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.Light;
import javax.media.j3d.PointLight;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import nif.j3d.J3dNiAVObject;
import nif.niobject.controller.NiLightColorController;

public class J3dNiLightColorController extends J3dNiTimeController
{
	private ArrayList<PointLight> targetPointLights = new ArrayList<PointLight>();

	public J3dNiLightColorController(NiLightColorController controller, J3dNiAVObject nodeTarget)
	{
		super(controller);
		for (int i = 0; i < nodeTarget.numChildren(); i++)
		{
			if (nodeTarget.getChild(i) instanceof PointLight)
			{
				PointLight pointLight = (PointLight) nodeTarget.getChild(i);
				pointLight.setCapability(Light.ALLOW_COLOR_WRITE);
				targetPointLights.add(pointLight);
			}
		}
	}

	@Override
	public void update(Point3f value)
	{
		for (PointLight pointLight : targetPointLights)
			pointLight.setColor(new Color3f(value));
	}

}
