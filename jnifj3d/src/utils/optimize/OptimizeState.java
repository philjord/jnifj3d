package utils.optimize;

import java.util.ArrayList;

import nif.niobject.NiNode;
import nif.niobject.NiTriShape;

public class OptimizeState
{
	public class MergeTriShape
	{
		public ArrayList<NiNode> path = new ArrayList<NiNode>();
		public NiTriShape niTriShape = null;
	}

	public ArrayList<NiNode> currentPath = new ArrayList<NiNode>();
	public ArrayList<MergeTriShape> shapesToMerge = new ArrayList<MergeTriShape>();

	public void addShape(NiTriShape niTriShape)
	{
		MergeTriShape mergeTriShape = new MergeTriShape();
		mergeTriShape.niTriShape = niTriShape;
		mergeTriShape.path.addAll(currentPath);
		shapesToMerge.add(mergeTriShape);
	}

}
