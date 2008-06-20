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
 * 
 */
public @interface test4 {
	@interface inner {
		public Object f1 = null;
		@interface inner2 {
			public Object f1 = null;
		}
	}
}

@interface outer {
	public Object f1 = null;
}