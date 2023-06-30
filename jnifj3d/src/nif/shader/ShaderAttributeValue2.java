package nif.shader;

import org.jogamp.java3d.ShaderAttributeValue;

public class ShaderAttributeValue2 extends ShaderAttributeValue
{
	public ShaderAttributeValue2(String attrName, Object value)
	{
		super(attrName, value);
		// for easy equals
		setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
	}

	public String toString()
	{
		return "ShaderAttributeValue2: " + this.getAttributeName() + " " + this.getValue();
	}
}
