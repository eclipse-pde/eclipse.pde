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
 * Test unsupported @noinstantiate tag on fields in outer / inner interface
 */
public interface test3 {
	interface inner {
		/**
		 * @noinstantiate
		 */
		public int f2 = 0;
		interface inner2 {
			/**
			 * @noinstantiate
			 */
			public char[] f3 = {};
		}
	}
}

interface outer {
	/**
	 * @noinstantiate
	 */
	public static Object f1 = null;
}
