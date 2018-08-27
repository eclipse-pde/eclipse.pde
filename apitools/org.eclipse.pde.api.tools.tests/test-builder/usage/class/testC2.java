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

import c.ClassUsageClass;

/**
 * 
 */
public class testC2 {
	/**
	 * Constructor
	 */
	public testC2() {
		ClassUsageClass c = new ClassUsageClass();
	}

	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			ClassUsageClass c = new ClassUsageClass();
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			ClassUsageClass c = new ClassUsageClass();
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		ClassUsageClass c = new ClassUsageClass();
	}
}
