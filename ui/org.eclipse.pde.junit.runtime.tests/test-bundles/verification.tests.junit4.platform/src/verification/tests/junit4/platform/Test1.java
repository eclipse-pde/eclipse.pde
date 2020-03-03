/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
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
package verification.tests.junit4.platform;

import org.junit.Assert;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
public class Test1 {

	@Test
	public void test1() {
		try {
			Thread.currentThread().getContextClassLoader().loadClass("doesnt.exist");
			Assert.fail("ClassNotFoundException expected");
		} catch (ClassNotFoundException e) {
			// expected
		}
	}

	@Test
	public void test2() {

	}

}
