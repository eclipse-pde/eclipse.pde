/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.annotations.NoOverride;

/**
 * Test unsupported @NoOverride tag on fields in outer / inner annotation
 */
public @interface test7 {
	@interface inner {
		/**
		 */
		@NoOverride
		public int f2 = 0;
		@interface inner2 {
			/**
			 */
			@NoOverride
			public char[] f3 = {};
		}
	}
}

@interface outer {
	/**
	 */
	@NoOverride
	public static Object f1 = null;
}
