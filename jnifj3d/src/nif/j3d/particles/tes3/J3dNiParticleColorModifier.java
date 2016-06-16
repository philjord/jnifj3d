package nif.j3d.particles.tes3;

import javax.vecmath.Color4f;

import nif.compound.NifColor4;
import nif.compound.NifKey;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiParticleColorModifier;

public class J3dNiParticleColorModifier extends J3dNiParticleModifier
{
	private NiColorData niColorData;

	private NifKey[] keys = null;

	public J3dNiParticleColorModifier(NiParticleColorModifier niParticleColorModifier, J3dNiParticlesData j3dNiParticlesData,
			NiToJ3dData niToJ3dData)
	{
		super(niParticleColorModifier, j3dNiParticlesData, niToJ3dData);
		niColorData = (NiColorData) niToJ3dData.get(niParticleColorModifier.colorData);
		keys = niColorData.data.keys;
	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{
		for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
		{
			float ageSec = j3dNiParticlesData.particleAge[i] / 1000f;
			Color4f nc = interpolateColor(ageSec);

			float[] cs = j3dNiParticlesData.particleColors;
			cs[i * 4 + 0] = nc.x;
			cs[i * 4 + 1] = nc.y;
			cs[i * 4 + 2] = nc.z;
			cs[i * 4 + 3] = nc.w; 
		}
		

	}

	private Color4f interpolateColor(float ageSec)
	{
		NifColor4 ic = (NifColor4) niColorData.data.keys[keys.length - 1].value;
		Color4f ret = new Color4f(ic.r, ic.g, ic.b, ic.a);
		for (int i = 0; i < keys.length; i++)
		{
			if ((i == 0 && ageSec <= keys[i].time) || (i > 0 && ageSec >= keys[i - 1].time && ageSec <= keys[i].time))
			{
				if (i == 0)
				{
					NifColor4 nc = (NifColor4) niColorData.data.keys[0].value;
					ret.set(nc.r, nc.g, nc.b, nc.a);
				}
				else
				{
					float currentInterpolationValue = (ageSec - keys[i - 1].time) / (keys[i].time - keys[i - 1].time);

					NifColor4 nc0 = (NifColor4) niColorData.data.keys[i - 1].value;
					NifColor4 nc1 = (NifColor4) niColorData.data.keys[i].value;
					ret.set((nc0.r * currentInterpolationValue) + (nc1.r * (1 - currentInterpolationValue)), //
							(nc0.g * currentInterpolationValue) + (nc1.g * (1 - currentInterpolationValue)), //
							(nc0.b * currentInterpolationValue) + (nc1.b * (1 - currentInterpolationValue)), //
							(nc0.a * currentInterpolationValue) + (nc1.a * (1 - currentInterpolationValue)));
				}
				break;
			}
		}

		return ret;
	}
}
