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

/**
 * Test supported @noextend tag on constructors in the default package
 */
public class test45 {
	/**
	 * Constructor
	 * @noextend This constructor is not intended to be referenced by clients.
	 */
	public test45() {
		
	}
	
	/**
	 * Constructor
	 * @noextend This constructor is not intended to be referenced by clients.
	 */
	protected test45(int i) {
		
	}
}
