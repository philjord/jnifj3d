package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticleGrowFade;

public class J3dNiParticleGrowFade extends J3dNiParticleModifier
{

	public J3dNiParticleGrowFade(NiParticleGrowFade niParticleGrowFade, J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData,
			NiToJ3dData niToJ3dData)
	{
		super(niParticleGrowFade, j3dNiAutoNormalParticlesData, niToJ3dData);

	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{
		// growing and shrinking
		// float growTime;
		// short growGeneration;
		// float fadeTime;
		// short fadeGeneration;
		// float baseScale;
	}

	@Override
	public void particleCreated(int id)
	{

	}
}
