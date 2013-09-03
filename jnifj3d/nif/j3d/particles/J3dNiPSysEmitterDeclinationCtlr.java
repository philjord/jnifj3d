package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysEmitterDeclinationCtlr;

public class J3dNiPSysEmitterDeclinationCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterDeclinationCtlr(NiPSysEmitterDeclinationCtlr niPSysEmitterDeclinationCtlr, NiToJ3dData niToJ3dData,
			J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterDeclinationCtlr, niToJ3dData, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateDeclination(value);
	}

}
