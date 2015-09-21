package nif.j3d.animation;

import nif.j3d.J3dNiAVObject;
import nif.j3d.interp.FloatInterpolator;
import nif.niobject.controller.NiExtraDataController;

public class J3dNiExtraDataController extends J3dNiTimeController implements FloatInterpolator.Listener
{
	//This is for the extra data track in animations (for say doing damage as the animation progresses)
	public J3dNiExtraDataController(NiExtraDataController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);
	}

	@Override
	public void update(float value)
	{
		//TODO: how do I publish this out? attach it to the model some how?
	}

}
