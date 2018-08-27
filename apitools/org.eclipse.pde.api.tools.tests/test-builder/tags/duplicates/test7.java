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
public interface test7 {

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @noreference This method is not intended to be referenced by clients. 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1();
	
	/**
	 */
	interface Inner {
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m1();
	}
	
}

/**
 */
interface outer {
	/**
	 */
	public void m1();
}
