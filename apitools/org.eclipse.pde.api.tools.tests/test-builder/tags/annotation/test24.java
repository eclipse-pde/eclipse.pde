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
 * Tests invalid @noinstantiate tags on nested inner annotations
 * @noinstantiate
 */
public @interface test24 {

	/**
	 * @noinstantiate
	 */
	@interface inner {
		
	}
	
	@interface inner1 {
		/**
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
	 * @noinstantiate
	 */
	@interface inner {
		
	}
}
