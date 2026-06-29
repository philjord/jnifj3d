package nif.j3d.particles;

import org.jogamp.vecmath.Color4f;

import nif.compound.NifKeyGroup.NifKeyGroupNifColor4;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiPSysColorModifier;

public class J3dNiPSysColorModifier extends J3dNiPSysModifier {
	private NiColorData niColorData;

	public J3dNiPSysColorModifier(NiPSysColorModifier niPSysColorModifier, NiToJ3dData niToJ3dData) {
		super(niPSysColorModifier, niToJ3dData);
		niColorData = (NiColorData)niToJ3dData.get(niPSysColorModifier.data);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			NifKeyGroupNifColor4 kgc = niColorData.data;
			System.out.println("J3dNiPSysColorModifier");
			System.out.print(" numKeys " + kgc.numKeys);
			System.out.println(" interpolation " + kgc.interpolation.type);

			int i = 0;
			float t = kgc.time[i];
			Color4f c = new Color4f(kgc.value[i * 4 + 0], kgc.value[i * 4 + 1], kgc.value[i * 4 + 2],
					kgc.value[i * 4 + 3]);
			System.out.println(" key " + i + " time=" + t + " color=" + c);
			i = (kgc.numKeys / 2);
			t = kgc.time[i];
			c = new Color4f(kgc.value[i * 4 + 0], kgc.value[i * 4 + 1], kgc.value[i * 4 + 2], kgc.value[i * 4 + 3]);
			System.out.println(" key " + i + " time=" + t + " color=" + c);
			i = (kgc.numKeys - 1);
			t = kgc.time[i];
			c = new Color4f(kgc.value[i * 4 + 0], kgc.value[i * 4 + 1], kgc.value[i * 4 + 2], kgc.value[i * 4 + 3]);
			System.out.println(" key " + i + " time=" + t + " color=" + c);
		}
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		// this has a keygroup, which could be controlled by an interpolator
		// but if not then time values should be from 0 to 1 which strongly suggests I should use the age versus life span here	
		NifKeyGroupNifColor4 kgc = niColorData.data;
		// output a bad range (not inside 0 to 1)
		if (kgc.time[0] < 0 || kgc.time[(kgc.numKeys - 1)] > 1) {
			System.err.println("J3dNiPSysColorModifier key data times not 0 to 1 : "	+ kgc.time[0] + " to "
								+ kgc.time[(kgc.numKeys - 1)]);
		}

		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in ms

		float[] cs = j3dPSysData.particleColors;

		for (int pId = 0; pId < j3dPSysData.activeParticleCount; pId++) {

			float age = as[pId];
			float lifeSpan = lss[pId];
			float alphaValue = age / lifeSpan; // and normalised value from 0 to 1

			// from Point3Interpolator this is knots work, so go through time and find the one I am less than or equal to
			// then grab the one before and get an inter dist and apply
			float currentInterpolationValue = 0;
			int currentKnotIndex = 0;
			float[] knots = kgc.time;
			for (int i = 0; i < knots.length; i++) {
				if ((i == 0 && alphaValue <= knots[i])
					|| (i > 0 && alphaValue >= knots[i - 1] && alphaValue <= knots[i])) {
					if (i == 0) {
						currentInterpolationValue = 0f;
						currentKnotIndex = 0;
					} else {
						currentInterpolationValue = (alphaValue - knots[i - 1]) / (knots[i] - knots[i - 1]);
						currentKnotIndex = i - 1;
					}
					break;
				}
			}

			float[] values = niColorData.data.value;

			if (currentKnotIndex != 0 || currentInterpolationValue != 0f) {
				cs[pId * 4 + 0] = values[currentKnotIndex * 4 + 0]
									+ ((values[(currentKnotIndex + 1) * 4 + 0] - values[currentKnotIndex * 4 + 0])
										* currentInterpolationValue);
				cs[pId * 4 + 1] = values[currentKnotIndex * 4 + 1]
									+ ((values[(currentKnotIndex + 1) * 4 + 1] - values[currentKnotIndex * 4 + 1])
										* currentInterpolationValue);
				cs[pId * 4 + 2] = values[currentKnotIndex * 4 + 2]
									+ ((values[(currentKnotIndex + 1) * 4 + 2] - values[currentKnotIndex * 4 + 2])
										* currentInterpolationValue);
				cs[pId * 4 + 3] = values[currentKnotIndex * 4 + 3]
									+ ((values[(currentKnotIndex + 1) * 4 + 3] - values[currentKnotIndex * 4 + 3])
										* currentInterpolationValue);
			} else {
				// set it to the first value (just saves a bit of maths really)
				cs[pId * 4 + 0] = values[currentKnotIndex * 4 + 0];
				cs[pId * 4 + 1] = values[currentKnotIndex * 4 + 1];
				cs[pId * 4 + 2] = values[currentKnotIndex * 4 + 2];
				cs[pId * 4 + 3] = values[currentKnotIndex * 4 + 3];
			}

		}
		// Note j3dPSysData.recalcAllGaColors();will be called once by the particle system after all modifiers have run
	}

}
