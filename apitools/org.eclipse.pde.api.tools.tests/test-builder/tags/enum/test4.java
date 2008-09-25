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
 * Tests invalid @noreference tags on nested inner enums
 * @noreference
 */
public enum test4 {

	A;
	/**
	 * @noreference
	 */
	enum inner {
		
	}
	
	enum inner1 {
		A;
		/**
		 * @noreference
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
	 * @noreference
	 */
	enum inner {
		
	}
}
