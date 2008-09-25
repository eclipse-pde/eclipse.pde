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

/**
 * 
 */
public class test57 {

	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public static void m1() {
		
	}
	
	static class inner {
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 */
		public static final void m1() {
			
		}
	}
}

class outer {
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	static void m3() {
		
	}
}
