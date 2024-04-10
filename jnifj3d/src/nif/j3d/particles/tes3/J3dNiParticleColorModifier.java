package nif.j3d.particles.tes3;

import org.jogamp.vecmath.Color4f;

import nif.compound.NifColor4;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiParticleColorModifier;

public class J3dNiParticleColorModifier extends J3dNiParticleModifier
{
	private NiColorData niColorData;

	public J3dNiParticleColorModifier(NiParticleColorModifier niParticleColorModifier, J3dNiParticlesData j3dNiParticlesData,
			NiToJ3dData niToJ3dData)
	{
		super(niParticleColorModifier, j3dNiParticlesData, niToJ3dData);
		niColorData = (NiColorData) niToJ3dData.get(niParticleColorModifier.colorData);
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
		NifColor4 ic = niColorData.data.value[niColorData.data.value.length - 1];
		Color4f ret = new Color4f(ic.r, ic.g, ic.b, ic.a);
		for (int i = 0; i < niColorData.data.value.length; i++)
		{
			if ((i == 0 && ageSec <= niColorData.data.time[i]) || (i > 0 && ageSec >= niColorData.data.time[i - 1] && ageSec <= niColorData.data.time[i]))
			{
				if (i == 0)
				{
					NifColor4 nc = niColorData.data.value[0];
					ret.set(nc.r, nc.g, nc.b, nc.a);
				}
				else
				{
					float currentInterpolationValue = (ageSec - niColorData.data.time[i - 1]) / (niColorData.data.time[i] - niColorData.data.time[i - 1]);

					NifColor4 nc0 = niColorData.data.value[i - 1];
					NifColor4 nc1 = niColorData.data.value[i];
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

	@Override
	public void particleCreated(int pId)
	{
		NifColor4 nc = niColorData.data.value[0];
		float[] cs = j3dNiParticlesData.particleColors;
		cs[pId * 4 + 0] = nc.r;
		cs[pId * 4 + 1] = nc.g;
		cs[pId * 4 + 2] = nc.b;
		cs[pId * 4 + 3] = nc.a;

	}
}
