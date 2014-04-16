package nif.j3d.animation;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransparencyAttributes;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.interp.FloatInterpolator;
import nif.niobject.controller.NiAlphaController;

public class J3dNiAlphaController extends J3dNiTimeController implements FloatInterpolator.Listener
{
	private TransparencyAttributes transparencyAttributes;

	public J3dNiAlphaController(NiAlphaController controller, J3dNiAVObject nodeTarget)
	{
		super(controller);

		if (nodeTarget instanceof J3dNiGeometry)
		{
			Appearance app = ((J3dNiGeometry) nodeTarget).getShape().getAppearance();
			this.transparencyAttributes = app.getTransparencyAttributes();
			if (transparencyAttributes == null)
			{
				transparencyAttributes = new TransparencyAttributes();
				app.setTransparencyAttributes(transparencyAttributes);
			}

			transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);

		}

	}

	@Override
	public void update(float value)
	{
		transparencyAttributes.setTransparency(value);
	}

}
