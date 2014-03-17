/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 * Tests invalid @nooverride tags on nested inner interfaces
 */
public interface test1 {

	/**
	 * @nooverride
	 * @return
	 */
	int m1();

	interface inner {
		/**
		 * @nooverride
		 * @return
		 */
		int m1();
	}
	
}

