package nif;

import java.util.List;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.SimpleCamera;

public class NifJ3dVisRoot
{
	private NiToJ3dData niToJ3dData;

	private List<SimpleCamera> cameras;

	private J3dNiAVObject visualRoot;

	public NifJ3dVisRoot(J3dNiAVObject visualRoot, NiToJ3dData niAVObjects)
	{
		this.visualRoot = visualRoot;
		niToJ3dData = niAVObjects;

	}

	public List<SimpleCamera> getCameras()
	{
		return cameras;
	}

	public void setCameras(List<SimpleCamera> cameras)
	{
		this.cameras = cameras;
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
