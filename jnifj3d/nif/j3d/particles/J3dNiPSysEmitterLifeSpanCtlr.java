package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysEmitterLifeSpanCtlr;

public class J3dNiPSysEmitterLifeSpanCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterLifeSpanCtlr(NiPSysEmitterLifeSpanCtlr niPSysEmitterLifeSpanCtlr, NiToJ3dData niToJ3dData,
			J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterLifeSpanCtlr, niToJ3dData, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateLifeSpan(value);
	}

}
