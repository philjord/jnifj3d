package nif.j3d.particles;

import nif.niobject.particle.NiPSysEmitterLifeSpanCtlr;

public class J3dNiPSysEmitterLifeSpanCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterLifeSpanCtlr(NiPSysEmitterLifeSpanCtlr niPSysEmitterLifeSpanCtlr, J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterLifeSpanCtlr, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateLifeSpan(value);
	}

}
