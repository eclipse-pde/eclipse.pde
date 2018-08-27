/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * Tests invalid @noimplement tags on nested inner enums
 * @noimplement
 */
public enum test9 {

	A;
	/**
	 * @noimplement
	 */
	enum inner {
		
	}
	
	enum inner1 {
		A;
		/**
		 * @noimplement
		 */
		enum inner2 {
			
		}
	}
	
	enum inner2 {
		
	}
}

enum outer {
	A;
	/**
	 * @noimplement
	 */
	enum inner {
		
	}
}
