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

import org.eclipse.pde.api.tools.annotations.NoReference;

/**
 * Test supported @NoReference annotation on static final fields in a class
 */
public class test7 {
	public static class inner {
		@NoReference
		public static Object f1 = null;
		@NoReference
		protected static int f2 = 0;
		
		class inner2 {
		}
	}
}