package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.Material;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;

import nif.enums.TargetColor;
import nif.j3d.J3dNiAVObject;
import nif.j3d.animation.j3dinterp.interp.BoolInterpolator;
import nif.niobject.controller.NiVisController;

public class J3dNiVisController extends J3dNiTimeController implements BoolInterpolator.Listener
{
	private ArrayList<RenderingAttributes> targetRenderingAttributes = new ArrayList<RenderingAttributes>();

	private boolean currentVis = true;

	//TODO: why is this not working for acsended sleeper in morrowind skull?

	
	// need to pull out the Appearance and use the randring attributes to set visible false
	public J3dNiVisController(NiVisController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);
		for (int i = 0; i < nodeTarget.numChildren(); i++)
		{
			if (nodeTarget.getChild(i) instanceof Shape3D)
			{
				Shape3D shape = (Shape3D) nodeTarget.getChild(i);

				RenderingAttributes targetRenderingAttribute = shape.getAppearance().getRenderingAttributes();
				if (targetRenderingAttribute == null)
				{
					targetRenderingAttribute = new RenderingAttributes();
					shape.getAppearance().setRenderingAttributes(targetRenderingAttribute);
				}
				targetRenderingAttribute.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
				targetRenderingAttributes.add(targetRenderingAttribute);
			}
		}
		//System.out.println("J3dNiVisController");
	}

	private void setVis(boolean isVis)
	{
//System.out.println("isVis "+isVis);
		if (isVis != currentVis)
		{
			for (RenderingAttributes targetRenderingAttribute : targetRenderingAttributes)
			{
				targetRenderingAttribute.setVisible(isVis);
			}
		}
		currentVis = isVis;
	}

	@Override
	public void update(boolean isVis)
	{
		setVis(isVis);
	}

}
