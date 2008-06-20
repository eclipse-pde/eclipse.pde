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
 * Tests invalid @noextend tags on nested inner interfaces
 * @noextend
 */
public interface test9 {

	/**
	 * @noextend
	 */
	interface inner {
		
	}
	
	interface inner1 {
		/**
		 * @noextend
		 */
		interface inner2 {
			
		}
	}
	
	interface inner2 {
		
	}
}

interface outer {
	
	/**
	 * @noextend
	 */
	interface inner {
		
	}
}
