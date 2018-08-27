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

import org.eclipse.pde.api.tools.annotations.NoImplement;

/**
 * Test unsupported @NoImplement annotation on fields in inner / outer classes
 */
public class test7 {
	@NoImplement
	public Object f1 = null;
	@NoImplement
	protected int f2 = 0;
	@NoImplement
	private char[] f3 = {};
	@NoImplement
	long f4 = 0L;
	static class inner {
		@NoImplement
		public static Object f1 = null;
		@NoImplement
		protected int f2 = 0;
		@NoImplement
		private static char[] f3 = {};
		@NoImplement
		long f4 = 0L;
		class inner2 {
			@NoImplement
			public Object f1 = null;
			@NoImplement
			protected int f2 = 0;
			@NoImplement
			private char[] f3 = {};
			@NoImplement
			long f4 = 0L;
		}
	}
}

class outer {
	@NoImplement
	public Object f1 = null;
	@NoImplement
	protected int f2 = 0;
	@NoImplement
	private static char[] f3 = {};
	@NoImplement
	long f4 = 0L;
}