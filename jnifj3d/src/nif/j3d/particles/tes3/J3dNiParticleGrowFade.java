package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticleGrowFade;

public class J3dNiParticleGrowFade extends J3dNiParticleModifier
{

	public J3dNiParticleGrowFade(NiParticleGrowFade niParticleGrowFade, J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleGrowFade, j3dNiParticlesData, niToJ3dData);

	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{
//TODO: this is garbage fix it
		/*	float fractionOfSec = elapsedMillisec / 1000f;
		
			float[] rss = j3dNiAutoNormalParticlesData.particleRotationSpeed;
			float[] ras = j3dNiAutoNormalParticlesData.particleRotationAngle;
			for (int i = 0; i < j3dNiAutoNormalParticlesData.activeParticleCount; i++)
			{
				ras[i] += rss[i] * fractionOfSec;
			} 
		j3dNiParticlesData.recalcSizes();*/
	}

	@Override
	public void particleCreated(int id)
	{

	}
}
