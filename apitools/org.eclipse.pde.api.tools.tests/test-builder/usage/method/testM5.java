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

import m.MethodUsageEnum;

/**
 * 
 */
public class testM5 {
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			MethodUsageEnum.A.m1();
			MethodUsageEnum.A.m2();
			MethodUsageEnum.A.m3();
			MethodUsageEnum.m4();
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			MethodUsageEnum.A.m1();
			MethodUsageEnum.A.m2();
			MethodUsageEnum.A.m3();
			MethodUsageEnum.m4();
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		MethodUsageEnum.A.m1();
		MethodUsageEnum.A.m2();
		MethodUsageEnum.A.m3();
		MethodUsageEnum.m4();
	}
}
