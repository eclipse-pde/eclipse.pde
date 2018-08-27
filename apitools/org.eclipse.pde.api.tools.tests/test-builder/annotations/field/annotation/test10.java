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

import org.eclipse.pde.api.tools.annotations.NoReference;

/**
 * Test unsupported @NoReference tag on fields in outer / inner annotation
 */
public @interface test10 {
	@interface inner {
		/**
		 */
		@NoReference
		public final int f2 = 0;
		@interface inner2 {
			/**
			 */
			@NoReference
			public final char[] f3 = {};
		}
	}
}

@interface outer {
	/**
	 */
	@NoReference
	public final Object f1 = null;
}
