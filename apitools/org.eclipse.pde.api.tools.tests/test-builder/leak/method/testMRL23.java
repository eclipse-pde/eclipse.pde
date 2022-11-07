/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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


import x.y.z.classDefault;
import java.util.List;

public  class testMRL23 {

	public List<classDefault> m1() {
		return null;	
	}

	public List<List<classDefault>> m2() {
		return null;	
	}

	// This is not a return type leak.
	public void m3(List<classDefault> x) {
	}
}