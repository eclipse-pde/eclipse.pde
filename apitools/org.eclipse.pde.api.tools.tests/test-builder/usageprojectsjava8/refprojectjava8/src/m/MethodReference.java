
/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package m;

public class MethodReference {
	/**
	 * @noreference This method is not intended to be referenced by clients.
	
	 * @return
	 */
	public static void method1() {
		return ;
	}
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return
	 */
	public  void method2() {
		return ;
	}


}
