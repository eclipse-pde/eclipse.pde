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
 * Tests invalid tags on nested inner enums
 * @noreference
 * @nooverride
 * @noextend
 * @noinstantiate
 * @noimplement
 */
public enum test19 {

	A;
	/**
	 * @noreference
	 * @nooverride
	 * @noextend
	 * @noinstantiate
	 * @noimplement
	 */
	enum inner {
		
	}
	
	enum inner1 {
		A;
		/**
		 * @noreference
		 * @nooverride
		 * @noextend
		 * @noinstantiate
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
	 * @noreference
	 * @nooverride
	 * @noextend
	 * @noinstantiate
	 * @noimplement
	 */
	enum inner {
		
	}
}
