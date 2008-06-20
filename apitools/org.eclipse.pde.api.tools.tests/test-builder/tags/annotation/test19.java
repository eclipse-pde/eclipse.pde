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
 * Tests invalid tags on nested inner annotations
 * @noreference
 * @nooverride
 * @noextend
 * @noinstantiate
 */
public @interface test19 {

	/**
	 * @noreference
	 * @nooverride
	 * @noextend
	 * @noinstantiate
	 */
	@interface inner {
		
	}
	
	@interface inner1 {
		/**
		 * @noreference
		 * @nooverride
		 * @noextend
		 * @noinstantiate
		 */
		@interface inner2 {
			
		}
	}
	
	@interface inner2 {
		
	}
}

@interface outer {
	
	/**
	 * @noreference
	 * @nooverride
	 * @noextend
	 * @noinstantiate
	 */
	@interface inner {
		
	}
}
