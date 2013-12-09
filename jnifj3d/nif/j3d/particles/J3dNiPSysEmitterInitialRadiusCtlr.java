package nif.j3d.particles;

import nif.niobject.particle.NiPSysEmitterInitialRadiusCtlr;

public class J3dNiPSysEmitterInitialRadiusCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterInitialRadiusCtlr(NiPSysEmitterInitialRadiusCtlr niPSysEmitterInitialRadiusCtlr,
			J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterInitialRadiusCtlr, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updateInitialRadius(value);
	}

}
