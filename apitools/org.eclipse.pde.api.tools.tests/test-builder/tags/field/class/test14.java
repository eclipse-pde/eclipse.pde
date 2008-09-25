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
 * Test unsupported @noextend tag on fields in inner / outer classes
 */
public class test14 {
	/**
	 * @noextend
	 */
	public Object f1 = null;
	/**
	 * @noextend
	 */
	protected int f2 = 0;
	/**
	 * @noextend
	 */
	private char[] f3 = {};
	static class inner {
		/**
		 * @noextend
		 */
		public static Object f1 = null;
		/**
		 * @noextend
		 */
		protected int f2 = 0;
		/**
		 * @noextend
		 */
		private static char[] f3 = {};
		class inner2 {
			/**
			 * @noextend
			 */
			public Object f1 = null;
			/**
			 * @noextend
			 */
			protected int f2 = 0;
			/**
			 * @noextend
			 */
			private char[] f3 = {};
		}
	}
}

class outer {
	/**
	 * @noextend
	 */
	public Object f1 = null;
	/**
	 * @noextend
	 */
	protected int f2 = 0;
	/**
	 * @noextend
	 */
	private static char[] f3 = {};
}