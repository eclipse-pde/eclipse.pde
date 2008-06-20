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
 * Tests invalid @noextend tags on nested inner enums
 * @noextend
 */
public enum test9 {

	A;
	/**
	 * @noextend
	 */
	enum inner {
		
	}
	
	enum inner1 {
		A;
		/**
		 * @noextend
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
	 * @noextend
	 */
	enum inner {
		
	}
}
