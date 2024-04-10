package nif.j3d.animation.j3dinterp;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3d;

import nif.compound.NifKeyGroup.NifKeyGroupNifVector3;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.interp.RotPosPathInterpolator;
import nif.niobject.NiPosData;
import nif.niobject.interpolator.NiPathInterpolator;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromNif;

public class J3dNiPathInterpolator extends J3dNiInterpolator
{
	public static boolean CACHE_WEAK = true;
	private static Map<NifKeyGroupNifVector3, PathData> pathDataMap = Collections.synchronizedMap(new WeakHashMap<NifKeyGroupNifVector3, PathData>());

	public J3dNiPathInterpolator(NiPathInterpolator niPathInterp, NiToJ3dData niToJ3dData, TransformGroup targetTransform)
	{
		NifKeyGroupNifVector3 posData = ((NiPosData) niToJ3dData.get(niPathInterp.posData)).data;

		// check for no path
		if (posData.time.length > 2 || (posData.time.length == 2 && (
				posData.value[0*3+0] != posData.value[1*3+0] && posData.value[0*3+1] != posData.value[1*3+1] && posData.value[0*3+2] != posData.value[1*3+2]
						)))
		{
			if (posData.interpolation.type == 2)
			{
				// don't let 2 threads load up the data into weak map
				PathData data = null;
				synchronized (posData)
				{
					data = pathDataMap.get(posData);
					if (data == null)
					{
						float[] knots = new float[posData.time.length];
						Point3f[] positions = new Point3f[posData.time.length];
						Quat4f[] quats = new Quat4f[posData.time.length];
						Transform3D tempTrans = new Transform3D();
						for (int i = 0; i < posData.time.length; i++)
						{
							// times are in 0.0 to 1.0 normalized form
							knots[i] = posData.time[i];
							positions[i] = ConvertFromNif.toJ3dP3f(posData.value[i*3+0],posData.value[i*3+1],posData.value[i*3+2]);

							//TODO: this looks like a rubbish system, why not proper forward to a quat
							tempTrans.lookAt(new Point3d(0, 0, 0), ConvertFromNif.toJ3dP3d(posData.forward[i*3+0],posData.forward[i*3+1],posData.forward[i*3+2]),
									new Vector3d(0, 0, 1));

							quats[i] = new Quat4f();
							Utils3D.safeGetQuat(tempTrans, quats[i]);
						}
						data = new PathData(knots, positions, quats);
						if(CACHE_WEAK)
						pathDataMap.put(posData, data);
					}
				}
				RotPosPathInterpolator interpolator = new RotPosPathInterpolator(J3dNiInterpolator.prepTransformGroup(targetTransform),
						data.knots, data.quats, data.positions);
				setInterpolator(interpolator);
			}
			else
			{
				//TODO: the TCBKeyFrame class and interpolator in the j3dcore jar should be used here!!
				System.out.println("J3dNiPathInterpolator - posData.interpolation.type != 2 : " + posData.interpolation.type);
			}

		}
	}

	public static class PathData
	{
		public float[] knots;

		public Point3f[] positions;

		public Quat4f[] quats;

		public PathData(float[] knots, Point3f[] positions, Quat4f[] quats)
		{
			this.knots = knots;
			this.positions = positions;
			this.quats = quats;
		}
	}
}
