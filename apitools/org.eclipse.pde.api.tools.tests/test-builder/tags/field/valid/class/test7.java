/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 * Test supported @noreference tag on static final fields in a class
 */
public class test7 {
	public static class inner {
		/**
		 * @noreference
		 */
		public static Object f1 = null;
		/**
		 * @noreference
		 */
		protected static int f2 = 0;
		
		class inner2 {
		}
	}
}