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
 * Test supported @noreference tag on static final fields in inner / outer classes
 */
public class test3 {
	/**
	 * @noreference
	 */
	public static final Object f1 = null;
	/**
	 * @noreference
	 */
	protected static final int f2 = 0;
	/**
	 * @noreference
	 */
	private static final char[] f3 = {};
	/**
	 * @noreference
	 */
	static final long f4 = 0L;
	static class inner {
		/**
		 * @noreference
		 */
		public static final Object f1 = null;
		/**
		 * @noreference
		 */
		protected static final int f2 = 0;
		/**
		 * @noreference
		 */
		private static final char[] f3 = {};
		/**
		 * @noreference
		 */
		static final long f4 = 0L;
		static class inner2 {
			/**
			 * @noreference
			 */
			public static final Object f1 = null;
			/**
			 * @noreference
			 */
			protected static final int f2 = 0;
			/**
			 * @noreference
			 */
			private static final char[] f3 = {};
			/**
			 * @noreference
			 */
			static final long f4 = 0L;
		}
	}
}

class outer {
	/**
	 * @noreference
	 */
	public static final Object f1 = null;
	/**
	 * @noreference
	 */
	protected static final int f2 = 0;
	/**
	 * @noreference
	 */
	private static final char[] f3 = {};
	/**
	 * @noreference
	 */
	static final long f4 = 0L;
}