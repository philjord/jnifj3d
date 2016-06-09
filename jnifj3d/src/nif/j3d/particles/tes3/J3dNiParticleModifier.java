package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiGravity;
import nif.niobject.particle.NiParticleColorModifier;
import nif.niobject.particle.NiParticleGrowFade;
import nif.niobject.particle.NiParticleModifier;
import nif.niobject.particle.NiParticleRotation;

public abstract class J3dNiParticleModifier
{
	protected J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData;

	public J3dNiParticleModifier(NiParticleModifier niParticleModifier, J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData,
			NiToJ3dData niToJ3dData)
	{
		this.j3dNiAutoNormalParticlesData = j3dNiAutoNormalParticlesData;
	}

	// Called by particle system to get the modifier to apply it's effect, to a newly created particle
	public void particleCreated(int pId)
	{
		//default ignore
	}

	// Called by particle system to get the modifier to apply it's effect
	public abstract void updatePSys(long elapsedMillisec);

	public static J3dNiParticleModifier createJ3dNiParticleModifier(NiParticleModifier niParticleModifier,
			J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData, NiToJ3dData niToJ3dData)
	{
		if (niParticleModifier instanceof NiParticleGrowFade)
		{
			return new J3dNiParticleGrowFade((NiParticleGrowFade) niParticleModifier, j3dNiAutoNormalParticlesData, niToJ3dData);
		}
		else if (niParticleModifier instanceof NiGravity)
		{
			return new J3dNiGravity((NiGravity) niParticleModifier, j3dNiAutoNormalParticlesData, niToJ3dData);
		}
		else if (niParticleModifier instanceof NiParticleRotation)
		{
			return new J3dNiParticleRotation((NiParticleRotation) niParticleModifier, j3dNiAutoNormalParticlesData, niToJ3dData);
		}
		else if (niParticleModifier instanceof NiParticleColorModifier)
		{
			return new J3dNiParticleColorModifier((NiParticleColorModifier) niParticleModifier, j3dNiAutoNormalParticlesData, niToJ3dData);
		}
		else
		{
			System.out.println("J3dNiParticleModifier createJ3dNiParticleModifier unhandled NiParticleModifier " + niParticleModifier);
		}

		return null;
	}

	protected static float var(float range)
	{
		return (float) (Math.random() * range) - range / 2f;
	}
}
