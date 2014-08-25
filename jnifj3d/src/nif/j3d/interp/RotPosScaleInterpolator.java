package nif.j3d.interp;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import tools3d.utils.Utils3D;
import nif.j3d.NifTransformGroup;
import nif.j3d.animation.J3dNiSingleInterpController;
import nif.j3d.animation.interp.J3dNiTransformInterpolator;
import nif.niobject.interpolator.NiTransformInterpolator;

public class RotPosScaleInterpolator extends TransformInterpolator
{
	private PositionPathInterpolator positionPathInterpolator;

	private ScalePathInterpolator scalePathInterpolator;

	private XYZRotPathInterpolator xYZRotPathInterpolator;

	private RotationPathInterpolator quatRotInterpolator;

	private Vector3f defaultTrans = null;

	private Quat4f defaultRot = null;

	private float defaultScale = 1;

	private Transform3D baseTransform = null;

	public RotPosScaleInterpolator(NifTransformGroup target, float startTimeS, float lengthS,
			PositionPathInterpolator positionPathInterpolator, ScalePathInterpolator scalePathInterpolator,
			XYZRotPathInterpolator xYZRotPathInterpolator, RotationPathInterpolator quatRotInterpolator, Vector3f defaultTrans,
			Quat4f defaultRot, float defaultScale)
	{
		super(target, startTimeS, lengthS);
		this.positionPathInterpolator = positionPathInterpolator;

		this.scalePathInterpolator = scalePathInterpolator;

		this.xYZRotPathInterpolator = xYZRotPathInterpolator;

		this.quatRotInterpolator = quatRotInterpolator;
		 

	}

	/**
	 * Method overrride as we have 3 elements to update and the rotation can be one of 2 types
	 * @see nif.j3d.animation.interp.J3dNiInterpolator#process(float)
	 */
	@Override
	public void process(float alphaValue)
	{
		//sack bad spine good! sack has rots disabled
		if (!target.getOwner().getName().equals("Bip01 Sack"))
		//&& !target.getOwner().getName().equals("Bip01 Spine"))
		//	&& !target.getOwner().getName().equals("Bip01 Pelvis"))
		{

			//Bip01 HeadR has a bhkBlendController that has xyz below it
			// which is animationy
			return;
		}

		// preserve the target values if interps have no defaults		
		if (baseTransform == null)
		{
			baseTransform = new Transform3D();
			target.getTransform(baseTransform);
		}

		// convert to an offsetted time in seconds
		float normAlphaValue = alphaValue * lengthS;
		normAlphaValue += startTimeS;

		// set to the base target
		targetTransform.set(baseTransform);

		

		//OK so skel have transfroms that I think I want
		// any given anim looks aside through a huge cast system and grabs it out
		// right now I see that my translate are bad and my rotates are bad 

		//new info, can't multiply 2 trans forms as that change frame for trasnlation, need to add rots together
		// so now a mult of quat seperate from an add of translate seems ok?
		// but only usigna very fixed skel quat

		// but note that teh anims should only really supply rotations not translations,
		// as the translation come from the bones and never alter, so I should supply 
		//them from before all this here, which is like a get before we begin!

		// don't run skel we look aside for them in the main animation
		if (((NiTransformInterpolator) ((J3dNiTransformInterpolator) this.getOwner()).getOwner()).nVer.fileName.contains("skeleton"))
		{
			//TODO: after must stop skel animations totally, and discover what anims appear in skels
			System.out.println("ignore skel ");
			return;
		}
		
		System.out.println("doing " + target.getOwner().getName());
		Quat4f qb = new Quat4f();
		Vector3f vb = new Vector3f();
		Utils3D.safeGetQuat(baseTransform, qb);
		System.out.println("base ypr " + Utils3D.toStringQuat(qb));
		baseTransform.get(vb);
		System.out.println("base translate " + vb);

		if (alphaValue != prevAlphaValue)
		{

			Transform3D skelT = null;
			Vector3f skelMod = new Vector3f();
			;
			try
			{
				//if (target.getOwner().getName().equals("Bip01 Sack"))
				{
					J3dNiSingleInterpController skelInterp = (J3dNiSingleInterpController) target.getOwner().getJ3dNiTimeController();
					if (skelInterp != null)
					{
						J3dNiTransformInterpolator jtc = (J3dNiTransformInterpolator) skelInterp.getJ3dNiInterpolator();
						if (jtc != null)
						{
							RotPosScaleInterpolator rpsiskel = (RotPosScaleInterpolator) jtc.getInterpolator();
							if (rpsiskel != null)
							{
								System.out.println("rpsiskel.xYZRotPathInterpolator " + rpsiskel.xYZRotPathInterpolator);
								skelT = new Transform3D();
								if (rpsiskel.xYZRotPathInterpolator != null)
								{
									rpsiskel.xYZRotPathInterpolator.computeTransform(alphaValue);
									rpsiskel.xYZRotPathInterpolator.applyTransform(skelT);
									skelMod.set(rpsiskel.xYZRotPathInterpolator.getInterpedRot());
								}
								else if (rpsiskel.quatRotInterpolator != null)
								{
									rpsiskel.quatRotInterpolator.computeTransform(alphaValue);
									//rpsiskel.quatRotInterpolator.applyTransform(skelT);
								}
								//note we DON'T use skel default (it's the bones one so it would double up)

								if (rpsiskel.positionPathInterpolator != null)
								{
									rpsiskel.positionPathInterpolator.computeTransform(alphaValue);
									//rpsiskel.positionPathInterpolator.applyTransform(skelT);
								}
								//note we DON'T use skel default (it's the bones one so it would double up)

								if (rpsiskel.scalePathInterpolator != null)
								{
									rpsiskel.scalePathInterpolator.computeTransform(alphaValue);
									//rpsiskel.scalePathInterpolator.applyTransform(skelT);
								}
								//note we DON'T use skel default (it's the bones one so it would double up)
							}
						}
					}
				}
			}
			catch (Exception e)
			{
			}

			if (xYZRotPathInterpolator != null)
			{
				xYZRotPathInterpolator.computeTransform(normAlphaValue);

				//xYZRotPathInterpolator.addInterpedRot(skelMod);// add skel bit

				xYZRotPathInterpolator.applyTransform(targetTransform);

			}
			else if (quatRotInterpolator != null)
			{
				quatRotInterpolator.computeTransform(normAlphaValue);
				quatRotInterpolator.applyTransform(targetTransform);
			}
			else if (defaultRot != null)
			{
				targetTransform.setRotation(defaultRot);
			}

			if (positionPathInterpolator != null)
			{
				positionPathInterpolator.computeTransform(normAlphaValue);
				positionPathInterpolator.applyTransform(targetTransform);
			}
			else if (defaultTrans != null)
			{
				targetTransform.setTranslation(defaultTrans);
			}

			if (scalePathInterpolator != null)
			{
				scalePathInterpolator.computeTransform(normAlphaValue);
				scalePathInterpolator.applyTransform(targetTransform);
			}
			else if (defaultScale != Float.MIN_VALUE)
			{
				//targetTransform.setScale(defaultScale);
			}

			//TODO: finalT used here to seperate away from target but target can be done in one hit
			// possibly if target is taken out at constructor then the default system can be removed?
			Transform3D finalT = new Transform3D();
			if (skelT != null)
			{
				Quat4f q1 = new Quat4f();
				Vector3f v1 = new Vector3f();
				Utils3D.safeGetQuat(targetTransform, q1);
				System.out.println("anim ypr " + Utils3D.toStringQuat(q1));
				targetTransform.get(v1);
				System.out.println("anim translate " + v1);

				Quat4f q2 = new Quat4f();
				Vector3f v2 = new Vector3f();
				Utils3D.safeGetQuat(skelT,q2);
				System.out.println("skel ypr " + Utils3D.toStringQuat(q2));
				skelT.get(v2);
				System.out.println("skel translate " + v2);
				
				//skelMod

				//	q1.add(q2);
				//	v1.add(v2);
				//	finalT.set(q1,v1,1);
				
				//finalT.set(targetTransform);
						
				finalT.mul(targetTransform ,skelT );// note skel after?

				Utils3D.safeGetQuat(finalT, q1);
				System.out.println("final ypr " + Utils3D.toStringQuat(q1));
				finalT.get(v1);
				System.out.println("final translate " + v1);

			}
			else
			{

				Quat4f q1 = new Quat4f();
				Vector3f v1 = new Vector3f();

				Utils3D.safeGetQuat(targetTransform,q1);
				System.out.println("anim ypr " + Utils3D.toStringQuat(q1));
				targetTransform.get(v1);
				System.out.println("anim translate " + v1);

				Utils3D.safeGetQuat(finalT, q1);
				System.out.println("final ypr " + Utils3D.toStringQuat(q1));
				finalT.get(v1);
				System.out.println("final translate " + v1);

				finalT.set(targetTransform);
			}

			if (!isAffine(targetTransform))
			{
				System.out.println("rps this bummed it up " + this);
			}

			//only set on a change
			if (!finalT.equals(prevTargetTransform))
			{
				System.out.println("and set");
				target.setTransform(finalT);
				prevTargetTransform.set(finalT);
			}

			prevAlphaValue = alphaValue;
		}

	}

	private static boolean isAffine(Transform3D t)
	{
		float[] matrix = new float[16];
		t.get(matrix);
		boolean hasNAN = false;
		for (int i = 0; i < 16; i++)
			hasNAN = hasNAN || Float.isNaN(matrix[i]);
		boolean byPrim = (matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1);
		boolean byMeth = ((t.getType() & Transform3D.AFFINE) != 0);

		if (hasNAN)
			System.out.println("hasNAN");
		if (byPrim != byMeth)
			System.out.println("differs? " + t);
		return byMeth;
	}

	@Override
	public void computeTransform(float alphaValue)
	{
		//dummy as process does it special
		throw new UnsupportedOperationException();
	}

	@Override
	public void applyTransform(Transform3D t)
	{
		//dummy as process does it special		
		throw new UnsupportedOperationException();
	}
}
