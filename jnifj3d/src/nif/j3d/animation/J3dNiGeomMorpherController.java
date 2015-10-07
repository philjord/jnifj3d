package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Node;
import javax.vecmath.Point3d;

import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifMorph;
import nif.compound.NifMorphWeight;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.J3dNiTriShape;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.interp.J3dNiFloatInterpolator;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.NiMorphData;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.interpolator.NiInterpolator;
import tools3d.utils.scenegraph.VaryingLODBehaviour;
import utils.ESConfig;

public class J3dNiGeomMorpherController extends J3dNiTimeController
{
	private GeometryArray itsa;

	private float[] baseCoords;

	private NiMorphData niMorphData;

	private NifMorph currentNifMorph = null;

	private J3dNiInterpolator currentJ3dNiInterpolator = null;

	private SequenceBehavior sequenceBehavior = new SequenceBehavior(this);

	private float startTimeS;

	private float stopTimeS;

	private SequenceAlpha sequenceAlpha;

	private ArrayList<J3dNiInterpolator> j3dNiInterpolators = new ArrayList<J3dNiInterpolator>();

	public J3dNiGeomMorpherController(NiGeomMorpherController controller, NiToJ3dData niToJ3dData)
	{
		super(controller, null);

		niMorphData = (NiMorphData) niToJ3dData.get(controller.data);

		if (niMorphData != null)
		{
			startTimeS = controller.startTime;
			stopTimeS = controller.stopTime;

			NiAVObject target = (NiAVObject) niToJ3dData.get(controller.target);
			nodeTarget = niToJ3dData.get(target);
			if (nodeTarget != null)
			{
				if (controller.nVer.LOAD_VER >= NifVer.VER_10_1_0_106)
				{
					if (controller.nVer.LOAD_VER <= NifVer.VER_20_0_0_5)
					{
						for (int i = 0; i < controller.interpolators.length; i++)
						{
							// build and attach intrerps for later use, note index order match morph
							NifRef nr = controller.interpolators[i];
							createInterp(nr, niToJ3dData);
						}
					}
					else if (controller.nVer.LOAD_VER >= NifVer.VER_20_1_0_3)
					{
						for (int i = 0; i < controller.interpolatorWeights.length; i++)
						{
							// build and attach interps for later use, note index order match morph
							NifMorphWeight nmw = controller.interpolatorWeights[i];

							NifRef nr = nmw.interpolator;
							if (nmw.weight != 0)
								System.out.println("non 0 nifmorphweight " + nmw.weight + " " + controller.nVer);

							createInterp(nr, niToJ3dData);
						}
					}
				}
				else
				{
					for (int i = 0; i < niMorphData.numMorphs; i++)
					{
						createInterp(niMorphData.morphs[i], niToJ3dData);
					}
				}

				if (nodeTarget instanceof J3dNiTriBasedGeom)
				{
					// this bad boy is not connect to scene graph!
					J3dNiTriBasedGeom j3dNiTriBasedGeom = (J3dNiTriBasedGeom) nodeTarget;
					j3dNiTriBasedGeom.makeMorphable();
					// trishape use a base and current as then are truely animateable
					if (nodeTarget instanceof J3dNiTriShape)
					{
						itsa = ((J3dNiTriShape) j3dNiTriBasedGeom).getCurrentGeometryArray();
					}
					else
					{
						// should be a tristriparray
						itsa = j3dNiTriBasedGeom.getBaseGeometryArray();
					}

					// take a copy of the base vert coords
					float[] coords = itsa.getCoordRefFloat();
					baseCoords = new float[coords.length];
					System.arraycopy(coords, 0, baseCoords, 0, coords.length);
				}
				else
				{
					System.out.println("J3dNiGeomMorpherController target not J3dNiTriBasedGeom but " + nodeTarget + " " + controller.nVer);
				}

				sequenceBehavior.setEnable(false);
				sequenceBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
				addChild(sequenceBehavior);
			}
		}
	}

	public float getLength()
	{
		return stopTimeS - startTimeS;
	}

	/**
	 * For TES version the interpolators are just stuck in teh NifMorph as keys
	 * @param nifMorph
	 * @param niToJ3dData
	 */
	private void createInterp(NifMorph nifMorph, NiToJ3dData niToJ3dData)
	{
		float lengthS = stopTimeS - startTimeS;
		J3dNiFloatInterpolator j3dNiInterpolator = new J3dNiFloatInterpolator(nifMorph, startTimeS, lengthS, this);

		j3dNiInterpolators.add(j3dNiInterpolator);
		addChild(j3dNiInterpolator);

	}

	private void createInterp(NifRef nr, NiToJ3dData niToJ3dData)
	{
		NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(nr);
		if (niInterpolator != null)
		{
			J3dNiInterpolator j3dNiInterpolator = J3dNiTimeController.createInterpForController(this, niInterpolator, niToJ3dData,
					startTimeS, stopTimeS);
			if (j3dNiInterpolator != null)
			{
				j3dNiInterpolators.add(j3dNiInterpolator);
				addChild(j3dNiInterpolator);
			}
		}
	}

	/**
	 * Fires off in a single loop of the fire in question
	 * @param action
	 */
	public void fireFrameName(String action)
	{
		for (int i = 0; i < niMorphData.numMorphs; i++)
		{
			if (niMorphData.nVer.LOAD_VER <= NifVer.VER_10_1_0_0)
			{
				//TODO: b_n_dark_elf_head_06 has a NiTextKeyExtraData 
				// that shows the pieces of teh geomorph animation by time slice
				if (("Frame_" + i).equals(action))
				{
					sequenceBehavior.setEnable(false);
					currentNifMorph = niMorphData.morphs[i];
					currentJ3dNiInterpolator = j3dNiInterpolators.get(i);
					sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, false);

					sequenceBehavior.setEnable(true);// disables after loop if required
					return;
				}
			}
			else
			{
				if (niMorphData.morphs[i].frameName.equalsIgnoreCase(action))
				{
					sequenceBehavior.setEnable(false);
					currentNifMorph = niMorphData.morphs[i];
					currentJ3dNiInterpolator = j3dNiInterpolators.get(i);
					sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, false);

					sequenceBehavior.setEnable(true);// disables after loop if required
					return;
				}
			}
		}
	}

	/**
	 *  note process must be called externally from now on,  
	 *   this just preps up teh morph name
	 *   Using an external interp handing updates to this
	 * @param action
	 * @return 
	 */
	public void setFrameName(String action)
	{
		//only set if different
		if (currentNifMorph == null || !currentNifMorph.frameName.equals(action))
		{
			for (int i = 0; i < niMorphData.numMorphs; i++)
			{
				if (niMorphData.nVer.LOAD_VER <= NifVer.VER_10_1_0_0)
				{
					if (("Frame_" + i).equals(action))
					{
						sequenceBehavior.setEnable(false);
						currentNifMorph = niMorphData.morphs[i];
						sequenceAlpha = null;
						currentJ3dNiInterpolator = null;
						return;
					}
				}
				else
				{
					if (niMorphData.morphs[i].frameName.equals(action))
					{
						sequenceBehavior.setEnable(false);
						currentNifMorph = niMorphData.morphs[i];
						sequenceAlpha = null;
						currentJ3dNiInterpolator = null;
						return;
					}
				}
			}
		}
	}

	public String[] getAllMorphFrameNames()
	{
		String[] strings = new String[niMorphData.numMorphs];
		for (int i = 0; i < niMorphData.numMorphs; i++)
		{
			if (niMorphData.nVer.LOAD_VER <= NifVer.VER_10_1_0_0)
			{
				strings[i] = "Frame_" + i;
			}
			else
			{
				strings[i] = niMorphData.morphs[i].frameName;
			}
		}
		return strings;
	}

	@Override
	public void update(float value)
	{
		//the current morph needs to simply interp between start and morph vertex locations
		if (currentNifMorph != null && itsa != null)
		{
			final float interpValue = value;

			// use a local as the member can be swapped out anytime
			final NifMorph localCurrentNifMorph = currentNifMorph;

			itsa.updateData(new GeometryUpdater()
			{
				public void updateData(Geometry geometry)
				{
					float[] coordRefFloat = itsa.getCoordRefFloat();

					for (int i = 0; i < (coordRefFloat.length / 3); i++)
					{
						float x1 = baseCoords[(i * 3) + 0];
						float y1 = baseCoords[(i * 3) + 1];
						float z1 = baseCoords[(i * 3) + 2];

						// notice ConvertFromNif work here
						float x2 = localCurrentNifMorph.vectors[i].x * ESConfig.ES_TO_METERS_SCALE;
						float y2 = localCurrentNifMorph.vectors[i].z * ESConfig.ES_TO_METERS_SCALE;
						float z2 = -localCurrentNifMorph.vectors[i].y * ESConfig.ES_TO_METERS_SCALE;

						coordRefFloat[(i * 3) + 0] = x1 + (x2 * (interpValue));
						coordRefFloat[(i * 3) + 1] = y1 + (y2 * (interpValue));
						coordRefFloat[(i * 3) + 2] = z1 + (z2 * (interpValue));
					}

				}
			});
		}

	}

	/**
	 * Notice copy of J3dNiCOntrollerSequence
	 * @author philip
	 *
	 */
	public class SequenceBehavior extends VaryingLODBehaviour
	{
		public SequenceBehavior(Node node)
		{
			// NOTE!!!! these MUST be active, otherwise the headless locale that might be running physics doesn't continuously render
			super(node, new float[]
			{ 40, 120, 280 }, true, true);
		}

		@Override
		public void process()
		{

			// will be null when another interp is running this
			if (sequenceAlpha != null)
			{
				float alphaValue = sequenceAlpha.value();

				currentJ3dNiInterpolator.process(alphaValue);

				//turn off at the end
				if (sequenceAlpha.finished())
					setEnable(false);
			}
			else
			{
				setEnable(false);
			}
		}
	}
}
