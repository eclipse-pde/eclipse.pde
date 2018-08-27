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
 * Test unsupported @noextend tag on fields in inner / outer enums
 */
public enum test5 {
	
	A;
	
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
	static enum inner {
		
		A;
		
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
		enum inner2 {
			
			A;
			
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

enum outer {
	
	A;
	
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