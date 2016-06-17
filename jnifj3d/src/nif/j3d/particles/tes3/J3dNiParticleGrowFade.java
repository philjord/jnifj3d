package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticleGrowFade;

public class J3dNiParticleGrowFade extends J3dNiParticleModifier
{
	private float grow = 0;
	private float fade = 0;

	public J3dNiParticleGrowFade(NiParticleGrowFade niParticleGrowFade, J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleGrowFade, j3dNiParticlesData, niToJ3dData);
		grow = niParticleGrowFade.grow;
		fade = niParticleGrowFade.fade;
	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{

		//TODO: what exactly is the calculation
		//I'm going to say the particle
		//goes from 0 to 1 in size from t=0 to t=grow
		//goes from 1 to 0 in size from t=lifespan-fade to t=lifespan

		for (int pId = 0; pId < j3dNiParticlesData.activeParticleCount; pId++)
		{
			process(pId);
		}

	}

	@Override
	public void particleCreated(int pId)
	{
		process(pId);
	}

	private void process(int pId)
	{

		float ageSec = j3dNiParticlesData.particleAge[pId] / 1000f;

		float s = 1;

		if (ageSec < grow)

		{
			float gr = grow / ageSec;
			gr = gr < 0 ? 0 : gr > 1 ? 1 : gr;//clamp
			s = gr;
		}

		float lifeSpanSec = j3dNiParticlesData.particleLifeSpan[pId] / 1000f;
		if (ageSec - (lifeSpanSec - fade) > 0)

		{
			float fr = 1 - ((ageSec - (lifeSpanSec - fade)) / fade);
			fr = fr < 0 ? 0 : fr > 1 ? 1 : fr;//clamp
			s = fr;
		}

		float[] ss = j3dNiParticlesData.particleRadius;
		ss[pId] = s * j3dNiParticlesData.particlesBaseRadius;

	}

}
