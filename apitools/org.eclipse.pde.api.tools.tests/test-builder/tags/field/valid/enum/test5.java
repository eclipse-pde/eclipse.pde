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
 * Tests the valid use of @noreference field tags on inner / outer enums
 * 
 * @since 3.4
 */
public enum test5 {
	
	A,
	B;
	
	/**
	 * @noreference This enum field is not intended to be referenced by clients.
	 */
	public Object f1 = null;
	/**
	 * @noreference This enum field is not intended to be referenced by clients.
	 */
	protected int f2 = 0;
	/**
	 * @noreference This enum field is not intended to be referenced by clients.
	 */
	protected static char g = 'd';
	
	public enum inner {
		A,
		B;
		
		/**
		 * @noreference This enum field is not intended to be referenced by clients.
		 */
		public Object f1 = null;
		/**
		 * @noreference This enum field is not intended to be referenced by clients.
		 */
		protected int f2 = 0;
		/**
		 * @noreference This enum field is not intended to be referenced by clients.
		 */
		protected static char g = 'd';
	}
}
