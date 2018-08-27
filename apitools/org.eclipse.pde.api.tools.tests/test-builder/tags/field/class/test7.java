/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 * Test unsupported @noimplement tag on fields in inner / outer classes
 */
public class test7 {
	/**
	 * @noimplement
	 */
	public Object f1 = null;
	/**
	 * @noimplement
	 */
	protected int f2 = 0;
	/**
	 * @noimplement
	 */
	private char[] f3 = {};
	/**
	 * @noimplement
	 */
	long f4 = 0L;
	static class inner {
		/**
		 * @noimplement
		 */
		public static Object f1 = null;
		/**
		 * @noimplement
		 */
		protected int f2 = 0;
		/**
		 * @noimplement
		 */
		private static char[] f3 = {};
		/**
		 * @noimplement
		 */
		long f4 = 0L;
		class inner2 {
			/**
			 * @noimplement
			 */
			public Object f1 = null;
			/**
			 * @noimplement
			 */
			protected int f2 = 0;
			/**
			 * @noimplement
			 */
			private char[] f3 = {};
			/**
			 * @noimplement
			 */
			long f4 = 0L;
		}
	}
}

class outer {
	/**
	 * @noimplement
	 */
	public Object f1 = null;
	/**
	 * @noimplement
	 */
	protected int f2 = 0;
	/**
	 * @noimplement
	 */
	private static char[] f3 = {};
	/**
	 * @noimplement
	 */
	long f4 = 0L;
}