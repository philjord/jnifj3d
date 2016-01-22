package nif.j3d.animation;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.J3dNiTriShape;
import nif.j3d.animation.j3dinterp.interp.BoolInterpolator;
import nif.j3d.particles.J3dNiParticleSystem;
import nif.niobject.controller.NiVisController;

public class J3dNiVisController extends J3dNiTimeController implements BoolInterpolator.Listener
{
	//private ModelClip modelClip;

	private boolean currentVis = true;

	//TODO: ModelClip no longer supported called in FO3 for sure! find a new system 
	// can't just disconnect as animation would stop, perhaps set a null appearance?
	// F:\game media\Morrowind\Meshes\r\xascendedsleeper.nif good example
	// but seems to be set in FO3 wasteland
	public J3dNiVisController(NiVisController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);
		if (nodeTarget instanceof J3dNiNode || nodeTarget instanceof J3dNiParticleSystem || nodeTarget instanceof J3dNiTriShape)
		{

		/*	Vector4d[] planes = new Vector4d[6];
			for (int i = 0; i < 6; i++)
			{//Ax + By + Cz + D <= 0
				planes[i] = new Vector4d(0, 0, 0, Double.NEGATIVE_INFINITY);
			}
			modelClip = new ModelClip(planes);
			modelClip.setInfluencingBounds(null);
			modelClip.setCapability(ModelClip.ALLOW_INFLUENCING_BOUNDS_WRITE);
			modelClip.addScope(nodeTarget);

			nodeTarget.addChild(modelClip);*/

		}
		else
		{
			new Throwable("node target is not allowed for vis controller " + nodeTarget + " in " + controller.nVer.fileName)
					.printStackTrace();
		}

	}

	private void setVis(boolean isVis)
	{
		 
		if (isVis && !currentVis)
		{
//			modelClip.setInfluencingBounds(null);
		}
		else if (!isVis && currentVis)
		{
//			modelClip.setInfluencingBounds(new BoundingSphere(new Point3d(), Double.POSITIVE_INFINITY));
		}
		currentVis = isVis;
	}

	@Override
	public void update(boolean isVis)
	{
		setVis(isVis);
	}

}
