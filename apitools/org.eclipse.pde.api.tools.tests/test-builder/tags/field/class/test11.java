/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 * Test unsupported @noinstantiate tag on fields in inner / outer classes
 */
public class test11 {
	/**
	 * @noinstantiate
	 */
	public Object f1 = null;
	/**
	 * @noinstantiate
	 */
	protected int f2 = 0;
	/**
	 * @noinstantiate
	 */
	private char[] f3 = {};
	/**
	 * @noinstantiate
	 */
	long f4 = 0L;
	static class inner {
		/**
		 * @noinstantiate
		 */
		public static Object f1 = null;
		/**
		 * @noinstantiate
		 */
		protected int f2 = 0;
		/**
		 * @noinstantiate
		 */
		private static char[] f3 = {};
		/**
		 * @noinstantiate
		 */
		long f4 = 0L;
		class inner2 {
			/**
			 * @noinstantiate
			 */
			public Object f1 = null;
			/**
			 * @noinstantiate
			 */
			protected int f2 = 0;
			/**
			 * @noinstantiate
			 */
			private char[] f3 = {};
			/**
			 * @noinstantiate
			 */
			long f4 = 0L;
		}
	}
}

class outer {
	/**
	 * @noinstantiate
	 */
	public Object f1 = null;
	/**
	 * @noinstantiate
	 */
	protected int f2 = 0;
	/**
	 * @noinstantiate
	 */
	private static char[] f3 = {};
	/**
	 * @noinstantiate
	 */
	long f4 = 0L;
}