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
package x.y.z;

import c.NoRefFunctionalInterface;

/**
 * Tests whether use of a functional interface via a lambda expression will be marked as illegal use
 * Currently this is not supported (Bug 431749)
 *
 */
public class testLambdaRestrictedInterface {
	
	class MR {
		public <T> void mr(NoRefFunctionalInterface<T> supplier) {}
	};

	void m1() {
		MR mr = new MR();
		mr.mr(() -> {
			return new Object();
		});
	}

}

