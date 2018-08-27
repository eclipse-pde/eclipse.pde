/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package f;

/**
 * 
 */
public interface FieldUsageInterface {

	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	int f1 = -1;
	
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public int f2 = 0; 
	
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public static int f3 = 1;
	
	/**
	 * @noreference
	 */
	public final int f4 = 2;
	
	/**
	 * @noreference
	 */
	public static final int f5 = 3;
}
