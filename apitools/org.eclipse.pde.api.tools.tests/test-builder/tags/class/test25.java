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
 * Tests invalid @noextend tags on nested final inner classes
 */
public class test25 {

	/**
	 * @noextend
	 */
	final class inner {
		
	}
	
	class inner2 {
		/**
		 * @noextend
		 */
		final class inner3 {
			
		}
	}
}

class outer {
	
	/**
	 * @noextend
	 */
	final class inner {
		
	}
}
