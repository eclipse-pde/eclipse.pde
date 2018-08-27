/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

/**
 * Tests invalid @noextend tags on nested final inner classes
 */
public class test7 {

	/**
	 * @noextend
	 */
	final class inner {
		
	}
	
	class inner2 {
		/**
		 * @noextend
		 */
		private final class inner3 {
			
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
