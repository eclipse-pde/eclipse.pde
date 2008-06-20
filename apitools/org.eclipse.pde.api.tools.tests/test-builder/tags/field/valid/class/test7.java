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
 * Test supported @noreference tag on static final fields in a class
 */
public class test7 {
	static class inner {
		/**
		 * @noreference
		 */
		public static Object f1 = null;
		/**
		 * @noreference
		 */
		protected static int f2 = 0;
		
		class inner2 {
			/**
			 * @noreference
			 */
			private char[] f3 = {};
		}
	}
}

class outer {
	/**
	 * @noreference
	 */
	public static Object f1 = null;
	/**
	 * @noreference
	 */
	private char[] f3;
}