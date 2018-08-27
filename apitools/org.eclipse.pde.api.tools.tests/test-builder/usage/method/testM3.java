/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
