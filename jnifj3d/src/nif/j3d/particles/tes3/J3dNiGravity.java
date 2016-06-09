package nif.j3d.particles.tes3;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.enums.FieldType;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiGravity;
import utils.convert.ConvertFromNif;

public class J3dNiGravity extends J3dNiParticleModifier
{
	private Vector3f position;

	private Vector3f direction;

	private float decay;//Decay = NiExp(Decay* Distance)

	private float force;

	private int type;

	public J3dNiGravity(NiGravity niGravity, J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niGravity, j3dNiAutoNormalParticlesData, niToJ3dData);

		position = ConvertFromNif.toJ3dNoScale(niGravity.position); // normal no scale 
		direction = ConvertFromNif.toJ3dNoScale(niGravity.direction); // normal no scale 
		decay = ConvertFromNif.toJ3d(niGravity.unknownFloat1);// TODO: is this right?
		force = ConvertFromNif.toJ3d(niGravity.force);
		type = niGravity.type.type;

	}

	//deburner
	private Point3f gravityLoc = new Point3f();

	private Vector3f gravityApplied = new Vector3f();

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		if (type == FieldType.FIELD_POINT)
		{
			gravityLoc.set(0, 0, 0);

			gravityApplied.set(position);

			gravityApplied.normalize();
			Point3f loc = new Point3f();

			 
			float fractionOfSec = elapsedMillisec / 1000f;

			float[] vs = j3dNiAutoNormalParticlesData.particleVelocity;
			float[] ts = j3dNiAutoNormalParticlesData.particleTranslation;

			for (int i = 0; i < j3dNiAutoNormalParticlesData.activeParticleCount; i++)
			{
				loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
				float distFromGravity = gravityLoc.distance(loc);
				float actualDecay = (decay * distFromGravity) * (decay * distFromGravity);
				float actualStrength = force - actualDecay;
				actualStrength = actualStrength < 0 ? 0 : actualStrength;

				vs[i * 3 + 0] += gravityApplied.x * fractionOfSec * actualStrength;
				vs[i * 3 + 1] += gravityApplied.y * fractionOfSec * actualStrength;
				vs[i * 3 + 2] += gravityApplied.z * fractionOfSec * actualStrength;
			}
		}
		else if (type == FieldType.FIELD_WIND)
		{
			//TODO:
		}

	}
}
