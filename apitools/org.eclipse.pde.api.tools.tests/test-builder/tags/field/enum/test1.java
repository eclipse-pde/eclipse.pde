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
 * Test unsupported @noreference tag on final fields in inner / outer enums
 */
public enum test1 {
	
	A;
	
	/**
	 * @noreference
	 */
	public final Object f1 = null;
	/**
	 * @noreference
	 */
	protected final int f2 = 0;
	/**
	 * @noreference
	 */
	private final char[] f3 = {};
	static enum inner {
		
		A;
		
		/**
		 * @noreference
		 */
		public final Object f1 = null;
		/**
		 * @noreference
		 */
		protected final int f2 = 0;
		/**
		 * @noreference
		 */
		private final char[] f3 = {};
		enum inner2 {
			
			A;
			
			/**
			 * @noreference
			 */
			public final Object f1 = null;
			/**
			 * @noreference
			 */
			protected final int f2 = 0;
			/**
			 * @noreference
			 */
			private final char[] f3 = {};
		}
	}
}

enum outer {
	
	A;
	
	/**
	 * @noreference
	 */
	public final Object f1 = null;
	/**
	 * @noreference
	 */
	protected final int f2 = 0;
	/**
	 * @noreference
	 */
	private final char[] f3 = {};
}