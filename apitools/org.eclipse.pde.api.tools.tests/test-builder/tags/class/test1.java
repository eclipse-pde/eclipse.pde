/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

/**
 * Tests invalid @noreference tags on nested inner classes
 * @noreference
 */
public class test1 {

	/**
	 * @noreference
	 */
	class inner {
		
	}
	
	/**
	 * @noreference
	 */
	private class inner2 {
		
	}
	
	class inner3 {
		
	}
}

class outer {
	
	/**
	 * @noreference
	 */
	class inner {
		
	}
}
