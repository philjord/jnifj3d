package nif.j3d;

import javax.media.j3d.BranchGroup;

import tools3d.utils.scenegraph.Fadable;

public class J3dRootNode extends BranchGroup implements Fadable
{
	private J3dNiAVObject j3dNiAVObject;

	public J3dRootNode(J3dNiAVObject j3dNiAVObject)
	{
		this.j3dNiAVObject = j3dNiAVObject;
	}

	public J3dNiAVObject getJ3dNiAVObject()
	{
		return j3dNiAVObject;
	}

	@Override
	public void fade(float percent)
	{
		if (j3dNiAVObject instanceof Fadable)
		{
			((Fadable) j3dNiAVObject).fade(percent);
		}
	}
}
