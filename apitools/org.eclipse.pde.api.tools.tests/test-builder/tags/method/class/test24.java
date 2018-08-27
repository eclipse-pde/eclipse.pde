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

/**
 * Test supported @noimplement tag on constructors in the default package
 */
public class test24 {
	/**
	 * Constructor
	 * @noimplement This constructor is not intended to be referenced by clients.
	 */
	public test24() {
		
	}
	
	/**
	 * Constructor
	 * @noimplement This constructor is not intended to be referenced by clients.
	 */
	protected test24(int i) {
		
	}
}
