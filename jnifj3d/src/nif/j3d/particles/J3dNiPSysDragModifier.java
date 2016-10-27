package nif.j3d.particles;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Transform3D;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiPSysDragModifier;
import utils.convert.ConvertFromNif;

public class J3dNiPSysDragModifier extends J3dNiPSysModifier
{
	private J3dNiNode dragJ3dNiNode;

	// we use this so all teh transforms on teh ninode abvoe are taken into account properly
	private Group dragNode = new Group();

	private Vector3f dragAxis;

	private float percentage;

	private float range;

	private float rangeFalloff;

	public J3dNiPSysDragModifier(NiPSysDragModifier niPSysDragModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysDragModifier, niToJ3dData);

		dragJ3dNiNode = (J3dNiNode) niToJ3dData.get((NiAVObject) niToJ3dData.get(niPSysDragModifier.parent));
		dragAxis = ConvertFromNif.toJ3dNoScale(niPSysDragModifier.dragAxis);
		percentage = ConvertFromNif.toJ3d(niPSysDragModifier.percentage) / 100f;
		range = ConvertFromNif.toJ3d(niPSysDragModifier.range);
		rangeFalloff = niPSysDragModifier.rangeFalloff;

		//we'll need this later
		dragJ3dNiNode.addChild(dragNode);
		dragNode.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
	}

	//deburner
	private Transform3D trans = new Transform3D();

	private Point3f dragLoc = new Point3f();

	private Vector3f dragApplied = new Vector3f();

	private Vector3f drag = new Vector3f();

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		dragLoc.set(0, 0, 0);
		if (dragNode.isCompiled() && !dragNode.isLive())
		{
			System.out
					.println("dragNode that can't be used " + dragJ3dNiNode.getName() + " " + dragJ3dNiNode.getNiAVObject().nVer.fileName);
			// something something getBone Accum Node, then add root??
		}
		else
		{
			dragNode.getLocalToVworld(trans);
		}
		dragApplied.set(dragAxis);
		trans.transform(dragApplied);
		trans.transform(dragLoc);

		dragApplied.normalize();
		Point3f loc = new Point3f();

		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float fractionOfSec = elapsedMillisec / 1000f;

		float[] vs = j3dPSysData.particleVelocity;
		float[] ts = j3dPSysData.particleTranslation;

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
			float distFromDrag = dragLoc.distance(loc);

			float actualPercent = percentage;
			if (distFromDrag < rangeFalloff)
			{
				if (distFromDrag > range)
				{
					actualPercent = percentage * (distFromDrag - range) / (rangeFalloff - range);
				}

				drag.set(-vs[i * 3 + 0], -vs[i * 3 + 1], -vs[i * 3 + 2]);
				drag.scale(fractionOfSec * actualPercent);
				//TODO: now apply the dragAxis as a projection to reduce it futher

				vs[i * 3 + 0] -= drag.x;
				vs[i * 3 + 1] -= drag.y;
				vs[i * 3 + 2] -= drag.z;
			}
		}
	}
}
