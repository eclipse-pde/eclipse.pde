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
 * Test unsupported @noreference tag on fields in outer / inner annotation
 */
public @interface test25 {
	@interface inner {
		/**
		 * @noreference
		 */
		public final int f2 = 0;
		@interface inner2 {
			/**
			 * @noreference
			 */
			public final char[] f3 = {};
		}
	}
}

@interface outer {
	/**
	 * @noreference
	 */
	public final Object f1 = null;
}
