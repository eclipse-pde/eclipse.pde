/*******************************************************************************
 * Copyright (c) 2025 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/

package a.b.c;

/**
 * Tests invalid @noextend tags on nested inner records
 * @noextend
 */
public record test3(int a) {

	/**
	 * @noextend
	 */
	record inner() {
		
	}
	
	record inner1(int a) {
		/**
		 * @noextend
		 */
		record inner2() {
			
		}
	}
	
	record inner2() {
		
	}
}

record outer(int a) {
	/**
	 * @noextend
	 */
	record inner() {
		
	}
}
