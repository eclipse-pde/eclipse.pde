/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

import org.eclipse.pde.api.tools.annotations.NoOverride;

/**
 * Tests invalid @NoOverride annotation on nested inner enums
 */
@NoOverride
public enum test5 {

	A;
	/**
	 */
	@NoOverride
	enum inner {
		
	}
	
	enum inner1 {
		A;
		/**
		 */
		@NoOverride
		enum inner2 {
			
		}
	}
	
	enum inner2 {
		
	}
}

enum outer {
	A;
	/**
	 */
	@NoOverride
	enum InnerNoRef4 {
		
	}
}
