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

import f.FieldUsageClass;

/**
 * Tests field usage, where the accessed field is tagged with a noreference tag
 */
public class testF1 {
	
	/**
	 * Constructor
	 */
	public testF1() {
		FieldUsageClass c = new FieldUsageClass();
		int f1 = c.f1;
		int f2 = FieldUsageClass.f2;
	}
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			FieldUsageClass c = new FieldUsageClass();
			int f1 = c.f1;
			int f2 = FieldUsageClass.f2;
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			FieldUsageClass c = new FieldUsageClass();
			int f1 = c.f1;
			int f2 = FieldUsageClass.f2;
		}
	}
}
class outer {
	/**
	 * Constructor
	 */
	public outer() {
		FieldUsageClass c = new FieldUsageClass();
		int f1 = c.f1;
		int f2 = FieldUsageClass.f2;
	}
}
