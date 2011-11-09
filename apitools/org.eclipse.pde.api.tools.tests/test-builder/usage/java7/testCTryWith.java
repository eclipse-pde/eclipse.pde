/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import java.io.IOException;

import d.TryWithResourcesClass;

public class testCTryWith {

	public void m1(){
		try (TryWithResourcesClass c = new TryWithResourcesClass(); TryWithResourcesClass c2 = new TryWithResourcesClass();){
		} catch (IOException e) {
		}
	}
}
