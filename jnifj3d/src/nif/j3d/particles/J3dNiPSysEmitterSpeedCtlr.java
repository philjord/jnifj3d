package nif.j3d.particles;

import nif.niobject.particle.NiPSysEmitterSpeedCtlr;

public class J3dNiPSysEmitterSpeedCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterSpeedCtlr(NiPSysEmitterSpeedCtlr niPSysEmitterSpeedCtlr, J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterSpeedCtlr, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateSpeed(value);
	}

}
