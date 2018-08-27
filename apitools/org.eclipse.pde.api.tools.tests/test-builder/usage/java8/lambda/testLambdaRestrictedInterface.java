/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import i.NoRefFunctionalInterface;

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

