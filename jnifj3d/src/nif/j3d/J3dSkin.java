package nif.j3d;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.GeometryUpdater;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Color3f;

import tools3d.utils.scenegraph.Fadable;

public abstract class J3dSkin extends Group implements GeometryUpdater, Fadable
{

	protected J3dNiNode[] skeletonBonesInSkinBoneIdOrder;//prelookups

	protected GeometryArray baseIndexedGeometryArray;

	protected GeometryArray currentIndexedGeometryArray;

	protected Transform3D skinDataTrans = new Transform3D();

	protected Transform3D[] skinBonesSkinOffsetInOrder;

	protected J3dNiTriBasedGeom j3dNiTriBasedGeom;

	public J3dSkin( )
	{		

	}

	@Override
	public void fade(float percent)
	{
		j3dNiTriBasedGeom.fade(percent);
	}

	@Override
	public void setOutline(Color3f c)
	{
		j3dNiTriBasedGeom.setOutline(c);
	}

	public void updateSkin()
	{
		currentIndexedGeometryArray.updateData(this);
	}

	

}
