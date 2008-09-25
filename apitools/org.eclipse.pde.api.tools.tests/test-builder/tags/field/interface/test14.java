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
 * Test unsupported @noimplement tag on fields in outer / inner interface
 */
public interface test14 {
	interface inner {
		/**
		 * @noimplement
		 */
		public int f2 = 0;
		interface inner2 {
			/**
			 * @noimplement
			 */
			public char[] f3 = {};
		}
	}
}

interface outer {
	/**
	 * @noimplement
	 */
	public static Object f1 = null;
}
