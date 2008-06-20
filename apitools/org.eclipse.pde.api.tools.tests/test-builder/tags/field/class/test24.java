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
 * Test unsupported @nooverride tag on fields in inner / outer classes
 */
public class test24 {
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
}