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
 * Test supported @noimplement tag on constructors in the default package
 */
public class test55 {
	/**
	 * Constructor
	 * @noimplement This constructor is not intended to be referenced by clients.
	 */
	public test55() {
		
	}
	
	/**
	 * Constructor
	 * @noimplement This constructor is not intended to be referenced by clients.
	 */
	protected test55(int i) {
		
	}
}
