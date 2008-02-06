/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * 
 * @since
 */
public class TestMethod3 {

	static class Inner {
		/**
		 * @noreference
		 */
		public void one() {}
		/**
		 * @noextend
		 */
		protected void two() {}
		/**
		 * @noreference
		 */
		public static void three() {}
	}
}
