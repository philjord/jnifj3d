package nif.j3d.particles;

import java.util.ArrayList;

import javax.vecmath.Point3f;

import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.particle.NiPSysMeshEmitter;
import utils.convert.ConvertFromNif;

public class J3dNiPSysMeshEmitter extends J3dNiPSysEmitter
{
	private NiPSysMeshEmitter niPSysMeshEmitter;

	private ArrayList<J3dNiTriBasedGeom> geoms = new ArrayList<J3dNiTriBasedGeom>();

	public J3dNiPSysMeshEmitter(NiPSysMeshEmitter niPSysMeshEmitter, NiToJ3dData niToJ3dData)
	{
		super(niPSysMeshEmitter, niToJ3dData);
		this.niPSysMeshEmitter = niPSysMeshEmitter;

		for (int i = 0; i < niPSysMeshEmitter.numEmitterMeshes; i++)
		{
			NiTriBasedGeom niTriBasedGeom = (NiTriBasedGeom) niToJ3dData.get(niPSysMeshEmitter.emitterMeshes[i]);
			J3dNiTriBasedGeom j3dNiTriBasedGeom = (J3dNiTriBasedGeom) niToJ3dData.get(niTriBasedGeom);
			if (j3dNiTriBasedGeom != null)
			{
				geoms.add(j3dNiTriBasedGeom);
			}
		}
	}

	@Override
	protected void getCreationPoint(Point3f pos)
	{
		//TODO: pre compute the surface area of each triangle and then use that to randomly pick an emit point

		//NOTE it is only from the vertices,edge or faces! not the interior!

		//System.out.println("Mesh emitter emitted a particle");
		float x = var(1f);
		x = ConvertFromNif.toJ3d(x);
		float y = var(1f);
		y = ConvertFromNif.toJ3d(y);
		float z = var(1f);
		z = ConvertFromNif.toJ3d(z);

		pos.set(x, y, z);
	}
}
