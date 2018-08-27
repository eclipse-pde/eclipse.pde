package invokedynamic;
import java.util.HashSet;
import java.util.function.Supplier;

/*******************************************************************************
 * Copyright (c) Mar 26, 2014 IBM Corporation and others.
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
 * Tests an invoke dynamic reference to a constructor method ref
 */
public class test4 {
	class MR {
		public <T> void mr(Supplier<T> supplier) {}
	};
	
	void m1() {
		MR mr = new MR();
		mr.mr(HashSet<String>::new);
	}
}
