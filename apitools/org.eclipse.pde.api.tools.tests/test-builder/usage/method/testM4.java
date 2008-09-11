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

import m.MethodUsageImpl;
import m.MethodUsageInterface;

/**
 * 
 */
public class testM4 {
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			MethodUsageInterface i = new MethodUsageImpl();
			i.m1();
			i.m3();
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			MethodUsageInterface i = new MethodUsageImpl();
			i.m1();
			i.m3();
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		MethodUsageInterface i = new MethodUsageImpl();
		i.m1();
		i.m3();
	}
}
