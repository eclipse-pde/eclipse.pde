/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
public class testM2 extends MethodUsageClass {
	/**
	 * @see x.y.z.MethodUsageClass#m2()
	 */
	public void m2() {
		super.m2();
	}
	
	public static class inner extends MethodUsageClass{
		/**
		 * @see x.y.z.MethodUsageClass#m2()
		 */
		public void m2() {
			super.m2();
		}
	}
	
	class inner2 extends MethodUsageClass {
		/**
		 * @see x.y.z.MethodUsageClass#m2()
		 */
		public void m2() {
			super.m2();
		}
	}
}

class outermu2 extends MethodUsageClass {
	/**
	 * @see x.y.z.MethodUsageClass#m2()
	 */
	public void m2() {
		super.m2();
	}
}
