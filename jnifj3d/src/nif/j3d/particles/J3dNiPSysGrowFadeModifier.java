package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysGrowFadeModifier;

public class J3dNiPSysGrowFadeModifier extends J3dNiPSysModifier {
	private float	growTime;
	private short	growGeneration;
	private float	fadeTime;
	private short	fadeGeneration;
	private float	baseScale;

	/**
	 * <niobject name="NiPSysGrowFadeModifier" abstract="0" inherit="NiPSysModifier" ver1="10.1.0.0">
	 * 
	 * Particle modifier that controls the time it takes to grow a particle from Size=0 to the specified Size in the
	 * emitter, and then back to 0. This modifer has no control over alpha settings.
	 * 
	 * <add name="Grow Time" type="float">Time in seconds to fade in.</add>
	 * <add name="Grow Generation" type="ushort">Unknown.</add> <add name="Fade Time" type="float">Time in seconds to
	 * fade out.</add> <add name="Fade Generation" type="ushort">Unknown.</add>
	 * <add name="Base Scale" type="float" ver1="20.2.0.7" userver="11">Unknown</add> </niobject>
	 */

	public J3dNiPSysGrowFadeModifier(NiPSysGrowFadeModifier niPSysGrowFadeModifier, NiToJ3dData niToJ3dData) {
		super(niPSysGrowFadeModifier, niToJ3dData);
		growTime = niPSysGrowFadeModifier.growTime;
		growGeneration = niPSysGrowFadeModifier.growGeneration;
		fadeTime = niPSysGrowFadeModifier.fadeTime;
		fadeGeneration = niPSysGrowFadeModifier.fadeGeneration;
		baseScale = niPSysGrowFadeModifier.baseScale;

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysGrowFadeModifier");
			System.out.print(" growTime " + growTime);
			System.out.print(" growGeneration " + growGeneration);
			System.out.print(" fadeTime " + fadeTime);
			System.out.print(" fadeGeneration " + fadeGeneration);
			System.out.println(" baseScale " + baseScale);
		}

	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		//System.out.println("J3dNiPSysGrowFadeModifier updatePSys");
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		// note that radius is set in a particle as spawn, if this modifier doesn't exist then that is the particle size
		// otherwise this modifier just varies size from 0-1 and back again to make it grow to it's set size

		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in ms

		for (int pId = 0; pId < j3dPSysData.activeParticleCount; pId++) {

			// the particle
			//goes from 0 to particleRadius in size from age=0 to age=grow
			//goes from particleRadius to 0 in size from age=lifespan-fade to age=lifespan

			float size = 1; // default to no grow or fade
			float ageSec = as[pId] / 1000f;
			float lifeSpanSec = lss[pId] / 1000f;
			if (ageSec > 0 && ageSec < lifeSpanSec) {
				if (growTime > 0 && ageSec < growTime) {
					size *= ageSec / growTime;
				}

				//Notice fade and grow are multiplied together so fade happens mid way through grow for short lifespan
				float lifeRemainSec = (lifeSpanSec - ageSec);
				if (fadeTime > 0 && lifeRemainSec < fadeTime)
					size *= lifeRemainSec / fadeTime;
			} else {
				size = 0;// 0 in case age falls outside the ranges, easily done for an update of 50ms acting on a life span of 225ms
			}

			if (size < 0 || size > 1)
				System.err.println("J3dNiPSysGrowFadeModifier bad size " + size);

			j3dPSysData.particleSize[pId] = size;

		}
	}

}
