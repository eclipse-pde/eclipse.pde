/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 * Test valid tags on an interface default method
 */
public interface test1 {

	/**
	 * @noreference
	 * @nooverride
	 * @return
	 */
	default int m1() {
		return 1;
	}
	
	interface inner {
		/**
		 * @noreference
		 * @nooverride
		 * @return
		 */
		default int m1() {
			return 1;
		}
	}
}
