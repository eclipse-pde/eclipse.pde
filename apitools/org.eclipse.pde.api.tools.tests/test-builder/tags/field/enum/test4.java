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
 * Test unsupported @noreference tag on final fields in inner / outer enums
 */
public enum test4 {
	
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
	public final char[] f3 = {};
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