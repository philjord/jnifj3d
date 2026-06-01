package org.jogamp.java3d;


public class MyOrderedGroup extends OrderedGroup {
	@Override
    void createRetained() {
	this.retained = new MyOrderedGroupRetained();
	this.retained.setSource(this);
    }
}
