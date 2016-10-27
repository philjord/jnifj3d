package nif.j3d;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.DistanceLOD;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;

import nif.compound.NifLODRange;
import nif.niobject.NiLODData;
import nif.niobject.NiLODNode;
import nif.niobject.NiRangeLODData;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

public class J3dLODNode extends J3dNiSwitchNode
{
	//Note super will call addchild before this node is inited, so must be constructed on first addchild call

	private DistanceLOD dl;

	private NiLODData niLODData;

	protected J3dLODNode(NiLODNode niLODNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niLODNode, niToJ3dData, textureSource, onlyNiNodes);
		System.out.println("NiLODNode detected! " + niLODNode.name);
		this.niLODData = (NiLODData) niToJ3dData.get(niLODNode.lODLevelData);
	}

	protected void configureOrderedGroup()
	{
		super.configureOrderedGroup();
		if (dl == null)
		{
			if (niLODData instanceof NiRangeLODData)
			{
				NiRangeLODData niRangeLODData = (NiRangeLODData) niLODData;

				Point3f position = ConvertFromNif.toJ3dP3f(niRangeLODData.lODCenter);

				float[] dist = new float[niRangeLODData.numLODLevels];
				for (int i = 0; i < niRangeLODData.numLODLevels; i++)
				{
					NifLODRange nifLODRange = niRangeLODData.lODLevels[i];
					dist[i] = nifLODRange.far;
				}

				dl = new DistanceLOD(dist, position);
				dl.addSwitch(switchGroup);
				super.addChild(dl);
				dl.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
				dl.setEnable(true);
			}
			else
			{
				//must be screen size one for gui elements?? disable the whole thing
				dl = new DistanceLOD(new float[]
				{ 5 });
			}

		}
	}
}
