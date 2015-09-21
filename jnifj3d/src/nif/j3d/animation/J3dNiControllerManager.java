package nif.j3d.animation;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;

import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiControllerSequence;
import nif.niobject.NiDefaultAVObjectPalette;
import nif.niobject.controller.NiControllerManager;

public class J3dNiControllerManager extends J3dNiTimeController
{
	private J3dNiControllerSequence[] sequences;

	public J3dNiControllerManager(NiControllerManager controllerManager, NiToJ3dData niToJ3dData)
	{
		super(controllerManager, null);

		if (controllerManager.objectPalette.ref == -1 || niToJ3dData.get(controllerManager.objectPalette) == null)
		{
			new Throwable("*******************NULL objectPalette DETECTED!!").printStackTrace();
		}

		NiDefaultAVObjectPalette niDefaultAVObjectPalette = (NiDefaultAVObjectPalette) niToJ3dData.get(controllerManager.objectPalette);
		J3dNiDefaultAVObjectPalette allAnimatedNodes = new J3dNiDefaultAVObjectPalette(niDefaultAVObjectPalette, niToJ3dData);

		sequences = new J3dNiControllerSequence[controllerManager.numControllerSequences];
		for (int i = 0; i < sequences.length; i++)
		{
			NiControllerSequence niControllerSequence = (NiControllerSequence) niToJ3dData.get(controllerManager.controllerSequences[i]);
			J3dNiControllerSequence j3dNiControllerSequence = new J3dNiControllerSequence(niControllerSequence, niToJ3dData);
			j3dNiControllerSequence.setAnimatedNodes(allAnimatedNodes);
			sequences[i] = j3dNiControllerSequence;
			addChild(j3dNiControllerSequence);
		}
	}

	@Override
	public Bounds getBounds()
	{
		BoundingSphere ret = new BoundingSphere((BoundingSphere) null);
		for (int i = 0; i < sequences.length; i++)
		{
			ret.combine(sequences[i].getBounds());
		}

		// if we hit nothing below us (e.g. just animated bones) give it a plenty big radius
		if (ret.isEmpty())
			ret.setRadius(50);

		return ret;
	}

	public J3dNiControllerSequence getSequence(String action)
	{
		for (int i = 0; i < sequences.length; i++)
		{
			if (sequences[i].getFireName().equals(action))
			{
				return sequences[i];
			}
		}
		return null;
	}

	public String[] getAllSequences()
	{
		String[] strings = new String[sequences.length];
		for (int i = 0; i < sequences.length; i++)
		{
			strings[i] = sequences[i].getFireName();
		}
		return strings;
	}

	public long[] getAllSequenceLengths()
	{
		long[] lengths = new long[sequences.length];
		for (int i = 0; i < sequences.length; i++)
		{
			lengths[i] = sequences[i].getLengthMS();
		}
		return lengths;
	}
}
