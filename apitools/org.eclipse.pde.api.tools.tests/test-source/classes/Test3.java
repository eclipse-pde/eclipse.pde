package classes;
/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
