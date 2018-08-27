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
 * Test unsupported @nooverride tag on fields in inner / outer classes
 */
public class test9 {
	/**
	 * @nooverride
	 */
	public Object f1 = null;
	/**
	 * @nooverride
	 */
	protected int f2 = 0;
	/**
	 * @nooverride
	 */
	private char[] f3 = {};
	/**
	 * @nooverride
	 */
	long f4 = 0L;
	static class inner {
		/**
		 * @nooverride
		 */
		public static Object f1 = null;
		/**
		 * @nooverride
		 */
		protected int f2 = 0;
		/**
		 * @nooverride
		 */
		private static char[] f3 = {};
		/**
		 * @nooverride
		 */
		long f4 = 0L;
		class inner2 {
			/**
			 * @nooverride
			 */
			public Object f1 = null;
			/**
			 * @nooverride
			 */
			protected int f2 = 0;
			/**
			 * @nooverride
			 */
			private char[] f3 = {};
			/**
			 * @nooverride
			 */
			long f4 = 0L;
		}
	}
}

class outer {
	/**
	 * @nooverride
	 */
	public Object f1 = null;
	/**
	 * @nooverride
	 */
	protected int f2 = 0;
	/**
	 * @nooverride
	 */
	private static char[] f3 = {};
	/**
	 * @nooverride
	 */
	long f4 = 0L;
}