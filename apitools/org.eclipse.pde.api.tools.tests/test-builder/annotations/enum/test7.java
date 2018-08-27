/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.annotations.NoInstantiate;

/**
 * Tests invalid @NoInstantiate annotations on nested inner enums
 */
@NoInstantiate
public enum test7 {

	A;
	/**
	 */
	@NoInstantiate
	enum inner {
		
	}
	
	enum inner1 {
		A;
		/**
		 */
		@NoInstantiate
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
	@NoInstantiate
	enum inner {
		
	}
}
