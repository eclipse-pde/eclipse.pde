package x.y.z;

import i.IExtInterface1;
import i.IExtInterface2;
import i.IExtInterface3;
import i.IExtInterface4;
import c.BaseImpl1_2_3_4;

/**
 * Multi-indirect implementations with an 
 * implementing parent class that implements all @noimplement
 * interfaces
 */
public class testC6 extends BaseImpl1_2_3_4 implements IExtInterface1,
		IExtInterface2, IExtInterface3, IExtInterface4 {

}
