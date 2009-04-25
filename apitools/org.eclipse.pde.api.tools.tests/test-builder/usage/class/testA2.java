package x.y.z;

import c.ClassUsageClass;

public class testA2 {

	public Object m1() {
		return new ClassUsageClass() {
			public String toString() {
				return super.toString();
			}
		};
	}
}
