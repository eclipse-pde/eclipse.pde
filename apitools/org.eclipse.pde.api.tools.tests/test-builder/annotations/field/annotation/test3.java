/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

import org.eclipse.pde.api.tools.annotations.NoInstantiate;

/**
 * Test unsupported @NoInstantiate tag on fields in outer / inner annotation
 */
public @interface test3 {
	@interface inner {
		/**
		 */
		@NoInstantiate
		public int f2 = 0;
		@interface inner2 {
			/**
			 */
			@NoInstantiate
			public char[] f3 = {};
		}
	}
}

@interface outer {
	/**
	 */
	@NoInstantiate
	public static Object f1 = null;
}
