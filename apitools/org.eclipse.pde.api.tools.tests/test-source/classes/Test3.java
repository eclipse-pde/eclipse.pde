package classes;
/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Tests that various arrangements of non-generic classes are scanned properly 
 */
public class Test3 {
	class Inner {
		
	}
	
	static class Inner2 {
		class Inner3 {
			
		}
	}
}

class Test3Outer {
	static class Inner {
		
	}
}
