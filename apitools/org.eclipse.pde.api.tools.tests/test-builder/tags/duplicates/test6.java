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
 */
public class test6 {
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	/**
	 */
	public class Inner {
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
	}
}

/**
 */
class outer {
	/**
	 */
	public void m1() {
		
	}
}
