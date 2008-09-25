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
 * Test unsupported tags on fields in outer / inner interface
 */
public interface test21 {
	interface inner {
		/**
		 * @nooverride
		 * @noimplement
		 * @noinstantiate
		 * @noextend
		 */
		public int f2 = 0;
		interface inner2 {
			/**
			 * @nooverride
			 * @noimplement
			 * @noinstantiate
			 * @noextend
			 */
			public char[] f3 = {};
		}
	}
}

interface outer {
	/**
	 * @nooverride
	 * @noimplement
	 * @noinstantiate
	 * @noextend
	 */
	public static Object f1 = null;
}
