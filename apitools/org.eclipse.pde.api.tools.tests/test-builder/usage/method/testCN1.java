/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import m.ConstructorUsageClass;

/**
 * 
 */
public class testCN1 {
	/**
	 * Constructor
	 */
	public testCN1() {
		ConstructorUsageClass c = new ConstructorUsageClass();
		ConstructorUsageClass c1 = new ConstructorUsageClass(1, new Object(), new char[] {});
		ConstructorUsageClass.inner c2 = new ConstructorUsageClass.inner();
	}

	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			ConstructorUsageClass c = new ConstructorUsageClass();
			ConstructorUsageClass c1 = new ConstructorUsageClass(1, new Object(), new char[] {});
			ConstructorUsageClass.inner c2 = new ConstructorUsageClass.inner();
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			ConstructorUsageClass c = new ConstructorUsageClass();
			ConstructorUsageClass c1 = new ConstructorUsageClass(1, new Object(), new char[] {});
			ConstructorUsageClass.inner c2 = new ConstructorUsageClass.inner();
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		ConstructorUsageClass c = new ConstructorUsageClass();
		ConstructorUsageClass c1 = new ConstructorUsageClass(1, new Object(), new char[] {});
		ConstructorUsageClass.inner c2 = new ConstructorUsageClass.inner();
	}
}
