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
 * Test unsupported @NoInstantiate annotation on fields in inner / outer classes
 */
public class test11 {
	@NoInstantiate
	public Object f1 = null;
	@NoInstantiate
	protected int f2 = 0;
	@NoInstantiate
	private char[] f3 = {};
	@NoInstantiate
	long f4 = 0L;
	static class inner {
		@NoInstantiate
		public static Object f1 = null;
		@NoInstantiate
		protected int f2 = 0;
		@NoInstantiate
		private static char[] f3 = {};
		@NoInstantiate
		long f4 = 0L;
		class inner2 {
			@NoInstantiate
			public Object f1 = null;
			@NoInstantiate
			protected int f2 = 0;
			@NoInstantiate
			private char[] f3 = {};
			@NoInstantiate
			long f4 = 0L;
		}
	}
}

class outer {
	@NoInstantiate
	public Object f1 = null;
	@NoInstantiate
	protected int f2 = 0;
	@NoInstantiate
	private static char[] f3 = {};
	/**
	 * 
	 */
	@NoInstantiate
	long f4 = 0L;
}