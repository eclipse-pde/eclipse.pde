/*******************************************************************************
 * Copyright (c) 2009, 2017 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import junit.framework.TestCase;
import org.eclipse.pde.internal.launching.launcher.LaunchConfigurationHelper;

/**
 * Tests LaunchConfigurationHelper convenience methods
 * @since 3.5
 */
public class LaunchConfigurationHelperTestCase extends TestCase {

	public void testgetStartData() {
		assertEquals(LaunchConfigurationHelper.getStartData("", false), "");
		assertEquals(LaunchConfigurationHelper.getStartData("", true), "@start");
		assertEquals(LaunchConfigurationHelper.getStartData("1:true", false), "@1:start");
		assertEquals(LaunchConfigurationHelper.getStartData("1:true", true), "@1:start");
		assertEquals(LaunchConfigurationHelper.getStartData("1:start", false), "@1:start");
		assertEquals(LaunchConfigurationHelper.getStartData("1:start", true), "@1:start");
		assertEquals(LaunchConfigurationHelper.getStartData("1:default", false), "@1");
		assertEquals(LaunchConfigurationHelper.getStartData("1:default", true), "@1:start");
		assertEquals(LaunchConfigurationHelper.getStartData("start", false), "@start");
		assertEquals(LaunchConfigurationHelper.getStartData("start", true), "@start");
		assertEquals(LaunchConfigurationHelper.getStartData(":start", false), "@start");
		assertEquals(LaunchConfigurationHelper.getStartData(":start", true), "@start");
		assertEquals(LaunchConfigurationHelper.getStartData("default", false), "");
		assertEquals(LaunchConfigurationHelper.getStartData("default", true), "@start");
	}

}
