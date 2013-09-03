package nif.j3d.interp;

import java.util.Enumeration;

import javax.media.j3d.Appearance;
import javax.media.j3d.Interpolator;
import javax.media.j3d.TransparencyAttributes;

public class FadeInInterpolator extends Interpolator
{
	private long startTime = 0;

	private long fadeTime;

	private TransparencyAttributes originalTransparencyAttributes;

	private TransparencyAttributes transparencyAttributes;

	private Appearance appearance;

	public FadeInInterpolator(long fadeTime, Appearance appearance)
	{
		this.fadeTime = fadeTime;
		this.appearance = appearance;
		originalTransparencyAttributes = appearance.getTransparencyAttributes();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);

		transparencyAttributes = new TransparencyAttributes();
		transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		transparencyAttributes.setTransparencyMode(TransparencyAttributes.NICEST);
		transparencyAttributes.setTransparency(1.0f);
		appearance.setTransparencyAttributes(transparencyAttributes);
	}

	public void initialize()
	{
		super.initialize();
		startTime = System.currentTimeMillis();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processStimulus(Enumeration arg0)
	{
		long fadingInTimeElapsed = System.currentTimeMillis() - startTime;

		if (fadingInTimeElapsed < fadeTime)
		{
			float alpha = 1.0f - ((float) fadingInTimeElapsed / (float) fadeTime);
			transparencyAttributes.setTransparency(alpha);
			wakeupOn(defaultWakeupCriterion);
		}
		else
		{
			appearance.setTransparencyAttributes(originalTransparencyAttributes);
			this.setEnable(false);
		}

	}

}
