package nif.j3d.particles.tes3;

import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiGravity;
import nif.niobject.particle.NiParticleColorModifier;
import nif.niobject.particle.NiParticleGrowFade;
import nif.niobject.particle.NiParticleModifier;
import nif.niobject.particle.NiParticleRotation;

public abstract class J3dNiParticleModifier
{
	protected J3dNiParticlesData j3dNiParticlesData;

	public J3dNiParticleModifier(NiParticleModifier niParticleModifier, J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		this.j3dNiParticlesData = j3dNiParticlesData;
	}

	// Called by particle system to get the modifier to apply it's effect, to a newly created particle
	public abstract void particleCreated(int pId);

	/**
	 * Called by particle system controller to get the modifier to apply it's effect
	 * elapased time is how long since the last call for fixed progress effects like gravity
	 * all other effect happen based on the life time of the particles themselves
	 * elpased time is in ms
	 */

	public abstract void updateParticles(long elapsedMillisec);

	public static J3dNiParticleModifier createJ3dNiParticleModifier(NiParticleModifier niParticleModifier,
			J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		if (niParticleModifier instanceof NiParticleGrowFade)
		{
			return new J3dNiParticleGrowFade((NiParticleGrowFade) niParticleModifier, j3dNiParticlesData, niToJ3dData);
		}
		else if (niParticleModifier instanceof NiGravity)
		{
			return new J3dNiGravity((NiGravity) niParticleModifier, j3dNiParticlesData, niToJ3dData);
		}
		else if (niParticleModifier instanceof NiParticleRotation)
		{
			return new J3dNiParticleRotation((NiParticleRotation) niParticleModifier, j3dNiParticlesData, niToJ3dData);
		}
		else if (niParticleModifier instanceof NiParticleColorModifier)
		{
			return new J3dNiParticleColorModifier((NiParticleColorModifier) niParticleModifier, j3dNiParticlesData, niToJ3dData);
		}
		else
		{
			System.out.println("J3dNiParticleModifier createJ3dNiParticleModifier unhandled NiParticleModifier " + niParticleModifier);
		}

		return null;
	}

	public static float var(float range)
	{
		return (float) (Math.random() * range * 2) - range;
	}

	public static Vector3f var(Vector3f range, Vector3f out)
	{
		out.set((float) (Math.random() * range.x * 2) - range.x, (float) (Math.random() * range.y * 2) - range.y,
				(float) (Math.random() * range.z * 2) - range.z);
		return out;
	}

	 
}
