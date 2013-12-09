package nif.j3d.particles;

import nif.niobject.particle.NiPSysEmitterDeclinationVarCtlr;

public class J3dNiPSysEmitterDeclinationVarCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterDeclinationVarCtlr(NiPSysEmitterDeclinationVarCtlr niPSysEmitterDeclinationVarCtlr,
			J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterDeclinationVarCtlr, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateDeclinationVar(value);
	}

}
