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

import f.FieldUsageClass;

/**
 * Tests field usage, where the accessed field is tagged with a noreference tag
 */
public class testF2 {
	
	/**
	 * Constructor
	 */
	public testF2() {
		FieldUsageClass.inner c = new FieldUsageClass.inner();
		int f1 = c.f1;
		int f2 = FieldUsageClass.inner.f2;
	}
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			FieldUsageClass.inner c = new FieldUsageClass.inner();
			int f1 = c.f1;
			int f2 = FieldUsageClass.inner.f2;
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			FieldUsageClass.inner c = new FieldUsageClass.inner();
			int f1 = c.f1;
			int f2 = FieldUsageClass.inner.f2;
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		FieldUsageClass.inner c = new FieldUsageClass.inner();
		int f1 = c.f1;
		int f2 = FieldUsageClass.inner.f2;
	}
}
