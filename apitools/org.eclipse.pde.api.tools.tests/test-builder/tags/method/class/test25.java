/*******************************************************************************
 * Copyright (c) Mar 25, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
