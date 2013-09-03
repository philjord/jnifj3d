package nif.j3d;

import utils.source.TextureSource;
import nif.niobject.bs.BSFadeNode;

public class J3dBSFadeNode extends J3dNiNode
{
	protected J3dBSFadeNode(BSFadeNode bSFadeNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(bSFadeNode, niToJ3dData, textureSource, onlyNiNodes);
	}
}
