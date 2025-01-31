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
 * Tests invalid @nooverride tags on nested inner records
 * @nooverride
 */
public record test5(int a) {

	/**
	 * @nooverride
	 */
	record inner() {
		
	}
	
	record inner1(int a) {
		/**
		 * @nooverride
		 */
		record inner2() {
			
		}
	}
	
	record inner2() {
		
	}
}

record outer(int a) {
	/**
	 * @nooverride
	 */
	record InnerNoRef4() {
		
	}
}
