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
package a.b.c;

/**
 * Tests valid tags on nested inner types
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class test4 {

	/**
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	class InnerNoRef4 {
		
	}
	
	class InnerNoRef4_1 {
		/**
		 * @noextend This class is not intended to be subclassed by clients.
		 * @noinstantiate This class is not intended to be instantiated by clients.
		 */
		class Inner2NoRef4 {
			
		}
	}
	
	class InnerNoRef4_2 {
		
	}
}

class OuterNoRef4 {
	
	/**
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	class InnerNoRef4 {
		
	}
}
