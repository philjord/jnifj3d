package nif.j3d.particles;

import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiPSysGravityModifier;
import utils.convert.ConvertFromNif;

public class J3dNiPSysGravityModifier extends J3dNiPSysModifier
{

	private J3dNiNode gravityJ3dNiNode;

	// we use this so all the transforms on teh ninode above are taken into account properly
	private Group gravityNode = new Group();

	private Vector3f gravityAxis;

	private float decay;//Decay = NiExp(Decay* Distance)

	private float strength;

	private int forceType;

	private float turbulence;

	private float turbulenceScale;

	public J3dNiPSysGravityModifier(NiPSysGravityModifier niPSysGravityModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysGravityModifier, niToJ3dData);
		gravityJ3dNiNode = (J3dNiNode) niToJ3dData.get((NiAVObject) niToJ3dData.get(niPSysGravityModifier.gravityObject));
		gravityAxis = ConvertFromNif.toJ3dNoScale(niPSysGravityModifier.gravityAxis); // normal no scale 
		decay = ConvertFromNif.toJ3d(niPSysGravityModifier.decay);
		strength = ConvertFromNif.toJ3d(niPSysGravityModifier.strength);
		forceType = niPSysGravityModifier.forceType;
		turbulence = niPSysGravityModifier.turbulence;
		turbulenceScale = niPSysGravityModifier.turbulenceScale;

		//we'll need this later
		gravityJ3dNiNode.addChild(gravityNode);
		gravityNode.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);

	}

	public void updateStrength(float value)
	{
		strength = value;
	}

	//deburner
	private Transform3D trans = new Transform3D();

	private Point3f gravityLoc = new Point3f();

	private Vector3f gravityApplied = new Vector3f();

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		if (forceType == NiPSysGravityModifier.FORCE_PLANAR)
		{
			gravityLoc.set(0, 0, 0);
			if (gravityNode.isCompiled() && !gravityNode.isLive())
			{
				System.out.println("gravityNode that can't be used " + gravityJ3dNiNode.getName() + " "
						+ gravityJ3dNiNode.getNiAVObject().nVer.fileName);
			}
			else
			{
				gravityNode.getLocalToVworld(trans);
			}

			gravityApplied.set(gravityAxis);
			trans.transform(gravityApplied);
			trans.transform(gravityLoc);

			gravityApplied.normalize();
			Point3f loc = new Point3f();

			J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
			float fractionOfSec = elapsedMillisec / 1000f;

			float[] vs = j3dPSysData.particleVelocity;
			float[] ts = j3dPSysData.particleTranslation;

			for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
			{
				loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
				float distFromGravity = gravityLoc.distance(loc);
				float actualDecay = (decay * distFromGravity) * (decay * distFromGravity);
				float actualStrength = strength - actualDecay;
				actualStrength = actualStrength < 0 ? 0 : actualStrength;

				vs[i * 3 + 0] += gravityApplied.x * fractionOfSec * actualStrength;
				vs[i * 3 + 1] += gravityApplied.y * fractionOfSec * actualStrength;
				vs[i * 3 + 2] += gravityApplied.z * fractionOfSec * actualStrength;
			}
		}

	}
}
