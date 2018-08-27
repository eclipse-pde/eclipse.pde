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
 * Test unsupported @NoOverride annotation on fields in inner / outer classes
 */
public class test9 {
	@NoOverride
	public Object f1 = null;
	@NoOverride
	protected int f2 = 0;
	@NoOverride
	private char[] f3 = {};
	@NoOverride
	long f4 = 0L;
	static class inner {
		@NoOverride
		public static Object f1 = null;
		@NoOverride
		protected int f2 = 0;
		@NoOverride
		private static char[] f3 = {};
		@NoOverride
		long f4 = 0L;
		class inner2 {
			@NoOverride
			public Object f1 = null;
			@NoOverride
			protected int f2 = 0;
			@NoOverride
			private char[] f3 = {};
			@NoOverride
			long f4 = 0L;
		}
	}
}

class outer {
	@NoOverride
	public Object f1 = null;
	@NoOverride
	protected int f2 = 0;
	@NoOverride
	private static char[] f3 = {};
	@NoOverride
	long f4 = 0L;
}