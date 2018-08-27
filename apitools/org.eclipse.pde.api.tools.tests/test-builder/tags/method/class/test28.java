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
 * Test supported @noreference tag on package default methods in outer / inner classes
 */
public class test28 {
	/**
	 * @noreference 
	 * @return
	 */
	int m1() {
		return 0;
	}
	static class inner {
		/**
		 * @noreference 
		 * @return
		 */
		 int m1() {
			return 0;
		}
		static class inner2 {
			/**
			 * @noreference 
			 * @return
			 */
			int m1() {
				return 0;
			}
		}
	}
}

class outer {
	/**
	 * @noreference 
	 * @return
	 */
	int m1() {
		return 0;
	}
}
