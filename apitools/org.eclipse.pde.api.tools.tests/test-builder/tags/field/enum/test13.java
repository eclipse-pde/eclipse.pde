/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * Test unsupported @noreference tag on enum constants in inner / outer enums
 */
public enum test13 {
	
	/**
	 * @noreference
	 */
	A;
	static enum inner {
		
		/**
		 * @noreference
		 */
		A;
		enum inner2 {
			
			/**
			 * @noreference
			 */
			A;
		}
	}
}

enum outer {
	
	/**
	 * @noreference
	 */
	A;
}