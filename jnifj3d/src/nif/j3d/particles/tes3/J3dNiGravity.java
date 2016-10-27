package nif.j3d.particles.tes3;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

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

	public J3dNiGravity(NiGravity niGravity, J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niGravity, j3dNiParticlesData, niToJ3dData);

		position = ConvertFromNif.toJ3d(niGravity.position); // point  
		direction = ConvertFromNif.toJ3dNoScale(niGravity.direction); // normal no scale 
		decay = 1;//ConvertFromNif.toJ3d(niGravity.unknownFloat1);// TODO: is this right?
		force = ConvertFromNif.toJ3d(niGravity.force);
		type = niGravity.type.type;

	}

	//deburner
	private Point3f gravityLoc = new Point3f();

	private Vector3f gravityApplied = new Vector3f();

	private Point3f loc = new Point3f();

	@Override
	public void updateParticles(long elapsedMillisec)
	{

		if (type == FieldType.FIELD_POINT)
		{
			gravityLoc.set(position);

			float fractionOfSec = elapsedMillisec / 1000f;

			float[] vs = j3dNiParticlesData.particleVelocity;
			float[] ts = j3dNiParticlesData.particleTranslation;

			for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
			{
				loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
				gravityApplied.sub(gravityLoc, loc);
				float distFromGravity = gravityApplied.length();
				gravityApplied.normalize();
				float actualDecay = 0;// TODO: do  I have decay? (decay * distFromGravity) * (decay * distFromGravity);
				float actualStrength = force - actualDecay;
				actualStrength = actualStrength < 0 ? 0 : actualStrength;

				vs[i * 3 + 0] += gravityApplied.x * fractionOfSec * actualStrength;
				vs[i * 3 + 1] += gravityApplied.y * fractionOfSec * actualStrength;
				vs[i * 3 + 2] += gravityApplied.z * fractionOfSec * actualStrength;
			}
		}
		else if (type == FieldType.FIELD_WIND)
		{
			gravityApplied.set(direction);

			gravityApplied.normalize();

			float fractionOfSec = elapsedMillisec / 1000f;

			float[] vs = j3dNiParticlesData.particleVelocity;
			//long[] as = j3dNiParticlesData.particleAge;

			for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
			{
				float actualStrength = force;
				actualStrength = actualStrength < 0 ? 0 : actualStrength;

				// this is what gravity should be
				// multipler of life to gravity effect, why not
				//float secs = as[i] / 1000f;
				//actualStrength = actualStrength * secs * secs;
				// without the fractionOfSec below, however this ruins candles and seems a bit strong

				vs[i * 3 + 0] += gravityApplied.x * fractionOfSec * actualStrength;
				vs[i * 3 + 1] += gravityApplied.y * fractionOfSec * actualStrength;
				vs[i * 3 + 2] += gravityApplied.z * fractionOfSec * actualStrength;				
			}
		}

	}

	@Override
	public void particleCreated(int pId)
	{
		//no gravity just yet		
	}
}
