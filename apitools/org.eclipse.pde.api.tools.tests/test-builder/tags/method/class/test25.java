/*******************************************************************************
 * Copyright (c) Mar 25, 2013 IBM Corporation and others.
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

/**
 * Test supported @nooverride and @noreference tags on private/default constructors
 */
public class test25 {

	/**
	 * @nooverride
	 * @noreference
	 */
	private test25() {
		
	}
	
	/**
	 * @nooverride
	 * @noreference
	 * @param num
	 */
	test25(int num) {
		
	}
}
