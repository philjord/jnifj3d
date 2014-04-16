package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysSimpleColorModifier;

public class J3dBSPSysSimpleColorModifier extends J3dNiPSysModifier
{
	private BSPSysSimpleColorModifier sysSimpleColorModifier;

	public J3dBSPSysSimpleColorModifier(BSPSysSimpleColorModifier sysSimpleColorModifier, NiToJ3dData niToJ3dData)
	{
		super(sysSimpleColorModifier, niToJ3dData);
		this.sysSimpleColorModifier = sysSimpleColorModifier;
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{ 
		float fadeInPercent = sysSimpleColorModifier.fadeInPercent;
		float fadeOutPercent = sysSimpleColorModifier.fadeOutPercent;
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		float[] cs = j3dPSysData.particleColors;
		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in seconds (I presume)

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			float ageRatio = (float) as[i] / (float) lss[i];

			float c0 = 0;
			float c1 = 1;
			float c2 = 0;

			if (fadeInPercent > 0 && ageRatio < fadeInPercent)
			{
				c1 = ageRatio / fadeInPercent;
				c0 = 1 - c1;
			}
			else if (fadeOutPercent < 1 && ageRatio > fadeOutPercent)
			{
				c1 = (1 - ageRatio) / (1 - fadeOutPercent);
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
		j3dPSysData.resetAllGaColors();

	}

	@Override
	public void particleCreated(int id)
	{
	}
}
