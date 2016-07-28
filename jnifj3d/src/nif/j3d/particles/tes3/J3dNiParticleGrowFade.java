package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticleGrowFade;
import utils.convert.ConvertFromNif;

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

		/*
		 size = 1.0;
		
		if ( grow > 0 && p.lifetime < grow )
			size *= p.lifetime / grow;
		
		if ( fade > 0 && p.lifespan - p.lifetime < fade )
			size *= ( p.lifespan - p.lifetime ) / fade;
		 */
		//TODO: what exactly is the calculation
		//I'm going to say the particle
		//goes from 0 to 1 in size from t=0 to t=grow
		//goes from 1 to 0 in size from t=lifespan-fade to t=lifespan

		float size = 1;
		float ageSec = j3dNiParticlesData.particleAge[pId] / 1000f;
		if (grow > 0 && ageSec < grow)
		{
			size *= ageSec / grow;
		}

		float lifeSpanSec = j3dNiParticlesData.particleLifeSpan[pId] / 1000f;
		if (fade > 0 && lifeSpanSec - ageSec < fade)
			size *= (lifeSpanSec - ageSec) / fade;

		float[] ss = j3dNiParticlesData.particleRadius;
		ss[pId] = ConvertFromNif.toJ3d(size * j3dNiParticlesData.particlesBaseRadius * J3dNiParticleEmitter.SIZE_MULTIPLY);

	}

}
