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

import f.FieldUsageClass;

public class testFStringSwitch {

	public void m1(){
		FieldUsageClass c = new FieldUsageClass();
		int i1, i3;
		String testSwitch = "accept";
		switch (testSwitch) {
		case "accept":
			i1 = c.f1;
			i3 = FieldUsageClass.f3;  // Constants are inlined, no marker is created
			break;
		case "reject":
			i1 = c.f1;
			i3 = FieldUsageClass.f3;  // Constants are inlined, no marker is created
			break;
		default:
			i1 = c.f1;
			i3 = FieldUsageClass.f3;  // Constants are inlined, no marker is created
			break;
		}
		i1 = i3 + i1;
	}
	
}