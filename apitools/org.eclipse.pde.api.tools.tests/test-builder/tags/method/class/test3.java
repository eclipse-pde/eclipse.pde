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
 * Test supported @noimplement tag on class methods
 */
public class test3 {
	
	static class inner {
		/**
		 * @noimplement
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noimplement
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
	}
}
