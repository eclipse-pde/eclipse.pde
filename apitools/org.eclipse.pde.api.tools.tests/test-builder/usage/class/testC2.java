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
