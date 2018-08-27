/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

package a.b.c;

/**
 * Tests invalid tags on default scope nested inner types
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class test11 {

	/**
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	class inner {
		
	}
	
	/**
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	private class innerP {
		
	}
	
	class inner2 {
		/**
		 * @noextend This class is not intended to be subclassed by clients.
		 * @noinstantiate This class is not intended to be instantiated by clients.
		 */
		class inner3 {
			
		}
		
		/**
		 * @noextend This class is not intended to be subclassed by clients.
		 * @noinstantiate This class is not intended to be instantiated by clients.
		 */
		private class inner3P {
			
		}
	}
}

class outer {
	
	/**
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	class inner {
		
	}
	
	/**
	 * @noextend This class is not intended to be subclassed by clients.
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	private class innerP {
		
	}
}
