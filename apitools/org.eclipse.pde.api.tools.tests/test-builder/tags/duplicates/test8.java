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

public enum test8 {

	A;
	
	/**
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	
	public enum inner {
		A;
		
		/**
		 * @noreference This enum method is not intended to be referenced by clients.
		 * @noreference This enum method is not intended to be referenced by clients.
		 * @noreference This enum method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
	}
}

enum outer {
	B;
	
	/**
	 */
	public void m1() {
		
	}
}
