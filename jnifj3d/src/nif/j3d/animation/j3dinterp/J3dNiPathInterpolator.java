package nif.j3d.animation.j3dinterp;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

import nif.compound.NifKey;
import nif.compound.NifKeyGroup;
import nif.compound.NifVector3;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.interp.RotPosPathInterpolator;
import nif.niobject.NiPosData;
import nif.niobject.interpolator.NiPathInterpolator;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromNif;

public class J3dNiPathInterpolator extends J3dNiInterpolator
{
	private static Map<NifKeyGroup, PathData> pathDataMap = Collections.synchronizedMap(new WeakHashMap<NifKeyGroup, PathData>());

	public J3dNiPathInterpolator(NiPathInterpolator niPathInterp, NiToJ3dData niToJ3dData, TransformGroup targetTransform)
	{
		NifKeyGroup posData = ((NiPosData) niToJ3dData.get(niPathInterp.posData)).data;

		// check for no path
		if (posData.keys.length > 2 || (posData.keys.length == 2 && !posData.keys[0].value.equals(posData.keys[1].value)))
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
						float[] knots = new float[posData.keys.length];
						Point3f[] positions = new Point3f[posData.keys.length];
						Quat4f[] quats = new Quat4f[posData.keys.length];
						Transform3D tempTrans = new Transform3D();
						for (int i = 0; i < posData.keys.length; i++)
						{
							NifKey key = posData.keys[i];
							// times are in 0.0 to 1.0 normalized form
							knots[i] = key.time;
							positions[i] = ConvertFromNif.toJ3dP3f((NifVector3) key.value);

							//TODO: this looks like a rubbish system, why not proper forward to a quat
							tempTrans.lookAt(new Point3d(0, 0, 0), ConvertFromNif.toJ3dP3d((NifVector3) key.forward),
									new Vector3d(0, 0, 1));

							quats[i] = new Quat4f();
							Utils3D.safeGetQuat(tempTrans, quats[i]);
						}
						data = new PathData(knots, positions, quats);
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

	public class PathData
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
