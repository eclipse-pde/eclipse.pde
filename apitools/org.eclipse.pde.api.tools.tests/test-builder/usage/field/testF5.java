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

import f.FieldUsageInterface;


/**
 * 
 */
public class testF5 {
	
	int f1 = FieldUsageInterface.f1;
	int f2 = FieldUsageInterface.f2;
	int f3 = FieldUsageInterface.f3;
	int f4 = FieldUsageInterface.f4;
	int f5 = FieldUsageInterface.f5;
	
	public static class inner {
		/**
		 * Constructor
		 */
		public inner() {
			int f1 = FieldUsageInterface.f1;
			int f2 = FieldUsageInterface.f2;
			int f3 = FieldUsageInterface.f3;
			int f4 = FieldUsageInterface.f4;
			int f5 = FieldUsageInterface.f5;
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 */
		public inner2() {
			int f1 = FieldUsageInterface.f1;
			int f2 = FieldUsageInterface.f2;
			int f3 = FieldUsageInterface.f3;
			int f4 = FieldUsageInterface.f4;
			int f5 = FieldUsageInterface.f5;
		}
	}
}

class outer {
	/**
	 * Constructor
	 */
	public outer() {
		int f1 = FieldUsageInterface.f1;
		int f2 = FieldUsageInterface.f2;
		int f3 = FieldUsageInterface.f3;
		int f4 = FieldUsageInterface.f4;
		int f5 = FieldUsageInterface.f5;
	}
}
