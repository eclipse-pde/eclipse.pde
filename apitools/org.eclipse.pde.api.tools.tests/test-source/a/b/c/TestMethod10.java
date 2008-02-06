/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

import java.util.List;

/**
 * Tests that methods with many Object parameters are processed correctly
 * @since
 */
public class TestMethod10 {

	
	/**
	 * @noreference
	 * @param name
	 * @param number
	 */
	public void one(String name, Integer number) {
		
	}
	
	/**
	 * @noextend
	 * @param list
	 * @param runnable
	 */
	protected void two(List list, Runnable runnable) {
		
	}
}
