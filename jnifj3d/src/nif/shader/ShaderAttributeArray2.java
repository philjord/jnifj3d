package nif.shader;

import org.jogamp.java3d.ShaderAttributeArray;
import org.jogamp.java3d.ShaderAttributeValue;

public class ShaderAttributeArray2 extends ShaderAttributeArray {
	public ShaderAttributeArray2(String attrName, Object value) {
		super(attrName, value);
		// for easy equals
		setCapability(ShaderAttributeValue.ALLOW_VALUE_READ);
	}

	@Override
	public String toString() {
		return "ShaderAttributeArray2: " + this.getAttributeName() + " " + this.getValue();
	}
}
