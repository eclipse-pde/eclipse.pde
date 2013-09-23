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

import org.eclipse.pde.api.tools.annotations.NoReference;
import org.eclipse.pde.api.tools.annotations.NoOverride;
import org.eclipse.pde.api.tools.annotations.NoExtend;
import org.eclipse.pde.api.tools.annotations.NoInstantiate;

/**
 * Tests invalid tags on nested inner annotations
 */
@NoReference
@NoExtend
@NoInstantiate
public @interface test7 {

	@NoReference
	@NoExtend
	@NoInstantiate
	@interface inner {
		
	}
	
	@interface inner1 {

		@NoReference
		@NoExtend
		@NoInstantiate
		@interface inner2 {
			
		}
	}
	
	@interface inner2 {
		
	}
}

@interface outer {
	@NoReference
	@NoExtend
	@NoInstantiate
	@interface inner {
		
	}
}
