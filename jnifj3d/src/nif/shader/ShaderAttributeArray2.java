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
		String ret = "ShaderAttributeArray2: " + this.getAttributeName() + "={";
		for (Object v : (Object[])getValue())
			System.out.print(v + ", ");
		System.out.println("}");		
		return ret;
	}
}
