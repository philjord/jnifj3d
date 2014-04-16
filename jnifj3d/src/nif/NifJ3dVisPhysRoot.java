package nif;

import java.util.List;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.SimpleCamera;

public class NifJ3dVisPhysRoot
{
	private NiToJ3dData niToJ3dData;

	private List<SimpleCamera> cameras;

	private J3dNiAVObject visualRoot;

	private J3dNiAVObject havokRoot;

	public NifJ3dVisPhysRoot(J3dNiAVObject visualRoot, J3dNiAVObject havokRoot, NiToJ3dData niToJ3dData)
	{
		this.visualRoot = visualRoot;
		this.havokRoot = havokRoot;
		this.niToJ3dData = niToJ3dData;
	}

	public List<SimpleCamera> getCameras()
	{
		return cameras;
	}

	public void setCameras(List<SimpleCamera> cameras)
	{
		this.cameras = cameras;
	}

	public J3dNiAVObject getHavokRoot()
	{
		return havokRoot;
	}

	public J3dNiAVObject getVisualRoot()
	{
		return visualRoot;
	}

	public NiToJ3dData getNiToJ3dData()
	{
		return niToJ3dData;
	}

}
