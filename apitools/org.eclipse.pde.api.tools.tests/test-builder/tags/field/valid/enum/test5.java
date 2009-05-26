/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
	
	enum inner {
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

enum outer {
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