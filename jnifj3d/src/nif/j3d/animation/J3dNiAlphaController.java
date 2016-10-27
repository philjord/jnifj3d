package nif.j3d.animation;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.TransparencyAttributes;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.animation.j3dinterp.interp.FloatInterpolator;
import nif.niobject.controller.NiAlphaController;

public class J3dNiAlphaController extends J3dNiTimeController implements FloatInterpolator.Listener
{
	private TransparencyAttributes transparencyAttributes;

	public J3dNiAlphaController(NiAlphaController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);

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
		//TODO: constants hand this to me, should do the divide at construction time
		if (value > 1)
			value = value / 255f;
		
		if (value < 0 || value > 1)
		{
			System.out.println("J3dNiAlphaController.update bum alpha " + value);
			//new Throwable().printStackTrace();
		}
		else
		{
			transparencyAttributes.setTransparency(value);
		}
	}

}
