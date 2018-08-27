/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package x.y.z;

import java.io.IOException;

import c.TryWithResourcesClass;


public class testCTryWith {

	public void m1(){
		try (TryWithResourcesClass c = new TryWithResourcesClass(); TryWithResourcesClass c2 = new TryWithResourcesClass();){
		} catch (IOException e) {
		}
	}
}
