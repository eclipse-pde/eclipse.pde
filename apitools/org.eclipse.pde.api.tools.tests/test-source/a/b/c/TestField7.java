/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
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
 * Used to test that a field with inherited restrictions will not inherit the class restrictions
 * as fields do not support 'no extend'
 * 
 * @noextend
 * @since
 */
public class TestField7 {
	/**
	 * @noreference
	 */
	public Object field1 = null;
}
