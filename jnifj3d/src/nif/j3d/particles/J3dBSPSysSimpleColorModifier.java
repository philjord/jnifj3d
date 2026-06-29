package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysSimpleColorModifier;

public class J3dBSPSysSimpleColorModifier extends J3dNiPSysModifier {
	private BSPSysSimpleColorModifier	sysSimpleColorModifier;
	float								fadeInPercent;
	float								fadeOutPercent;
	int									color1EndPercent;
	int									color1StartPercent;
	int									color2EndPercent;
	int									color2StartPercent;

	public J3dBSPSysSimpleColorModifier(BSPSysSimpleColorModifier sysSimpleColorModifier, NiToJ3dData niToJ3dData) {
		super(sysSimpleColorModifier, niToJ3dData);
		this.sysSimpleColorModifier = sysSimpleColorModifier;
		fadeInPercent = sysSimpleColorModifier.fadeInPercent;
		fadeOutPercent = sysSimpleColorModifier.fadeOutPercent;
		color1EndPercent = sysSimpleColorModifier.color1EndPercent;
		color1StartPercent = sysSimpleColorModifier.color1StartPercent;
		color2EndPercent = sysSimpleColorModifier.color2EndPercent;
		color2StartPercent = sysSimpleColorModifier.color2StartPercent;
		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dBSPSysSimpleColorModifier");
			System.out.print(" fadeInPercent " + fadeInPercent);
			System.out.print(" fadeOutPercent " + fadeOutPercent);
			System.out.print(" color1EndPercent " + color1EndPercent);
			System.out.print(" color1StartPercent " + color1StartPercent);
			System.out.print(" color2EndPercent " + color2EndPercent);
			System.out.print(" color2StartPercent " + color2StartPercent);
			System.out.print(" sysSimpleColorModifier.colors[0] " + sysSimpleColorModifier.colors[0]);
			System.out.print(" sysSimpleColorModifier.colors[1] " + sysSimpleColorModifier.colors[1]);
			System.out.println(" sysSimpleColorModifier.colors[2] " + sysSimpleColorModifier.colors[2]);
		}
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		float[] cs = j3dPSysData.particleColors;
		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in seconds (I presume)

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
			float ageAlpha = (float)as[i] / (float)lss[i];

			float c0 = 0;
			float c1 = 1;
			float c2 = 0;

			if (fadeInPercent > 0 && ageAlpha < fadeInPercent) {
				c1 = ageAlpha / fadeInPercent;
				c0 = 1 - c1;
			} else if (fadeOutPercent < 1 && ageAlpha > fadeOutPercent) {
				c1 = (1 - ageAlpha) / (1 - fadeOutPercent);
				c2 = 1 - c1;
			}

			cs[i * 4 + 0] = sysSimpleColorModifier.colors[0].r * c0;
			cs[i * 4 + 1] = sysSimpleColorModifier.colors[0].g * c0;
			cs[i * 4 + 2] = sysSimpleColorModifier.colors[0].b * c0;
			cs[i * 4 + 3] = sysSimpleColorModifier.colors[0].a * c0;

			cs[i * 4 + 0] += sysSimpleColorModifier.colors[1].r * c1;
			cs[i * 4 + 1] += sysSimpleColorModifier.colors[1].g * c1;
			cs[i * 4 + 2] += sysSimpleColorModifier.colors[1].b * c1;
			cs[i * 4 + 3] += sysSimpleColorModifier.colors[1].a * c1;

			cs[i * 4 + 0] += sysSimpleColorModifier.colors[2].r * c2;
			cs[i * 4 + 1] += sysSimpleColorModifier.colors[2].g * c2;
			cs[i * 4 + 2] += sysSimpleColorModifier.colors[2].b * c2;
			cs[i * 4 + 3] += sysSimpleColorModifier.colors[2].a * c2;

		}
		// Note j3dPSysData.recalcAllGaColors();will be called once by the particle system after all modifiers have run

	}

	@Override
	public void particleCreated(int id) {
	}
}
