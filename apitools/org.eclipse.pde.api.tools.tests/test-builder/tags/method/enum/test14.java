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
package a.b.c;

/**
 * Test unsupported @noimplement tag on methods in outer / inner enums
 */
public enum test14 {
	A;
	enum inner {
		A;
		/**
		 * @noimplement
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		enum inner2 {
			A;
			/**
			 * @noimplement
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noimplement
			 * @return
			 */
			public final char m2() {
				return 's';
			}
		}
	}
}

enum outer {
	A;
	/**
	 * @noimplement
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	public final char m2() {
		return 's';
	}
}
