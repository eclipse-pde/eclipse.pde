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
 * Test unsupported tags on enum constants in inner / outer enums
 */
public enum test56 {
	
	/**
	 * @noinstantiate
	 * @noreference
	 * @noextend
	 * @noimplement
	 */
	A;
	static enum inner {
		
		/**
		 * @noinstantiate
		 * @noreference
		 * @noextend
		 * @noimplement
		 */
		A;
		enum inner2 {
			
			/**
			 * @noinstantiate
			 * @noreference
			 * @noextend
			 * @noimplement
			 */
			A;
		}
	}
}

enum outer {
	
	/**
	 * @noinstantiate
	 * @noreference
	 * @noextend
	 * @noimplement
	 */
	A;
}