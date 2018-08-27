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
package a.b.c;

/**
 * Test unsupported @noimplement tag on fields in outer / inner interface
 */
public interface test5 {
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
