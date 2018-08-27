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
 * Test supported @nooverride tag on protected class methods in the default package
 */
public final class test14 {
	
	/**
	 * @nooverride
	 */
	public void m1() {
		
	}

	/**
	 * @nooverride
	 */
	protected void m2() {
		
	}
	
	public static final class inner {
		
		/**
		 * @nooverride
		 */
		public void m1() {
			
		}

		/**
		 * @nooverride
		 */
		protected void m2() {
			
		}
		
	}
}
