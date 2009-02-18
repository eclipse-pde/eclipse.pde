package x.y.z;

import java.util.List;

import c.ClassUsageClass;

public class testA9 {

	public List<String> m1() {
		class inner extends ClassUsageClass {
			
		}
		new inner();
		return null;
	}
}
