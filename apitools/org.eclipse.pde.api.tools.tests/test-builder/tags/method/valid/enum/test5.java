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
 * Test supported @noreference tag on enum methods in the default package
 */
public enum test5 {
	A;
	/**
	 * @noreference
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noreference
	 * @return
	 */
	public final char m2() {
		return 's';
	}
}
