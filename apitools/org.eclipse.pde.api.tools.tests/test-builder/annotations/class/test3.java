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

import org.eclipse.pde.api.tools.annotations.NoImplement;

/**
 * Tests invalid @NoImplement tags on nested inner types
 */
@NoImplement
public class test3 {

	/**
	 */
	@NoImplement
	class InnerNoRef4 {
		
	}
	
	/**
	 */
	@NoImplement
	private class Inner2NoRef4 {
		
	}
	
	class InnerNoRef4_2 {
		
	}
}

class OuterNoRef4 {
	
	/**
	 */
	@NoImplement
	class InnerNoRef4 {
		
	}
}
