/*******************************************************************************
 *  Copyright (c) 2025 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package verification.tests.junit6;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Test1 {

	@Test
	void test1() {
		try {
			Thread.currentThread().getContextClassLoader().loadClass("doesnt.exist");
			Assertions.fail("ClassNotFoundException expected");
		} catch (ClassNotFoundException e) {
			// expected
		}
	}

	@Test
	void test2() {

	}

}
