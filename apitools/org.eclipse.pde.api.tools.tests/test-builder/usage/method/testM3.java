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

import m.MethodUsageClass;

/**
 * 
 */
public class testM3 extends MethodUsageClass {
	/**
	 * Constructor
	 */
	public testM3() {
		MethodUsageClass.m4();
		MethodUsageClass.m5();
		MethodUsageClass.m6();
	}
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			MethodUsageClass.m4();
			MethodUsageClass.m5();
			MethodUsageClass.m6();
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			MethodUsageClass.m4();
			MethodUsageClass.m5();
			MethodUsageClass.m6();
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		MethodUsageClass.m4();
		MethodUsageClass.m5();
		MethodUsageClass.m6();
	}
}
