package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysGrowFadeModifier;

public class J3dNiPSysGrowFadeModifier extends J3dNiPSysModifier {
	private float	growTime;
	private short	growGeneration;
	private float	fadeTime;
	private short	fadeGeneration;
	private float	baseScale;

	public J3dNiPSysGrowFadeModifier(NiPSysGrowFadeModifier niPSysGrowFadeModifier, NiToJ3dData niToJ3dData) {
		super(niPSysGrowFadeModifier, niToJ3dData);
		growTime = niPSysGrowFadeModifier.growTime;
		growGeneration = niPSysGrowFadeModifier.growGeneration;
		fadeTime = niPSysGrowFadeModifier.fadeTime;
		fadeGeneration = niPSysGrowFadeModifier.fadeGeneration;
		baseScale = niPSysGrowFadeModifier.baseScale;
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		// growing and shrinking
		// float growTime;
		// short growGeneration;
		// float fadeTime;
		// short fadeGeneration;
		// float baseScale;

		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in ms

		for (int pId = 0; pId < j3dPSysData.activeParticleCount; pId++) {
			as [pId] += elapsedMillisec;

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
			float ageSec = as [pId] / 1000f;
			if (growTime > 0 && ageSec < growTime) {
				size *= ageSec / growTime;
			}

			float lifeSpanSec = lss [pId] / 1000f;
			if (fadeTime > 0 && lifeSpanSec - ageSec < fadeTime)
				size *= (lifeSpanSec - ageSec) / fadeTime;

			j3dPSysData.particleSize[pId] = size;
						 
		}
	}

	

}
