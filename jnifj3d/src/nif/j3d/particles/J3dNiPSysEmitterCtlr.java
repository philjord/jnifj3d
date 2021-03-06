package nif.j3d.particles;

import nif.niobject.particle.NiPSysEmitterCtlr;

public class J3dNiPSysEmitterCtlr extends J3dNiPSysModifierCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterCtlr(NiPSysEmitterCtlr niPSysModifierCtlr, J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysModifierCtlr, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateBirthRate(value);
	}

	@Override
	public void update(boolean value)
	{
		// Note there is also an active controller but this is used as well for emitters
		j3dNiPSysEmitter.active = value;
	}

}
