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
package x.y.z;

import c.ClassUsageClass;

/**
 * 
 */
public class testC1 extends ClassUsageClass {

	public static class inner extends ClassUsageClass {
		
	}
	
	class inner2 extends ClassUsageClass {
		
	}
}

class outer extends ClassUsageClass {
	
}
