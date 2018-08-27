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

import m.GenericMethodUsageClass;

/**
 * 
 */
public class testM6 {
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			GenericMethodUsageClass<String> o = new GenericMethodUsageClass<String>();
			String s = o.m1();
			System.out.println(s);
			o.m2(s);
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			GenericMethodUsageClass<String> o = new GenericMethodUsageClass<String>();
			String s = o.m1();
			System.out.println(s);
			o.m2(s);
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		GenericMethodUsageClass<String> o = new GenericMethodUsageClass<String>();
		String s = o.m1();
		System.out.println(s);
		o.m2(s);
	}
}
