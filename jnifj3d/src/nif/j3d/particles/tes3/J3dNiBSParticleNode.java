package nif.j3d.particles.tes3;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiNode;
import utils.source.TextureSource;

public class J3dNiBSParticleNode extends J3dNiNode
{
	// no extra info at this time, just a root indicator of particles
	public J3dNiBSParticleNode(NiNode niNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niNode, niToJ3dData, textureSource, onlyNiNodes);
	}

}
