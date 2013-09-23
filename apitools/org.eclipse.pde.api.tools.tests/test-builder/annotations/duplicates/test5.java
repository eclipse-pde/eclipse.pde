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

public enum test5 {

	A;
	
	/**
	 */
	@NoReference
	@NoReference
	@NoReference
	public String s = null;
	
	public enum inner {
		A;
		
		/**
		 */
		@NoReference
		@NoReference
		public Object o = null;
	}
}

enum outer {
	B;
	
	/**
	 */
	protected int i = -1;
}
