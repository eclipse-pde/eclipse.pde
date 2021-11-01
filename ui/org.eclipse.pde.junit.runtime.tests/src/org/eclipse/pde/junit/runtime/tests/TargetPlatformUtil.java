/*******************************************************************************
 *  Copyright (c) 2019, 2021 Julian Honnen and others.
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
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 *******************************************************************************/
package org.eclipse.pde.junit.runtime.tests;

import java.util.function.Predicate;

import org.osgi.framework.Bundle;

public class TargetPlatformUtil {

	private static final String TARGET_NAME = TargetPlatformUtil.class + "_target";

	public static void setRunningPlatformAsTarget() throws Exception {
		Predicate<Bundle> filter = b -> !isJunitRuntime(b);
		org.eclipse.pde.ui.tests.util.TargetPlatformUtil.setRunningPlatformSubSetAsTarget(TARGET_NAME, filter);
	}

	private static boolean isJunitRuntime(Bundle bundle) {
		// filter out junit.runtime bundles from the target platform
		// this tests the scenario where PDE supplies them from the host installation

		// XXX: this filter does not match the junit5.runtime bundle
		// JUnitLaunchConfigurationDelegate::getRequiredPlugins currently does not
		// handle junit5 (which is a bug), so that's fine for now
		return bundle.getSymbolicName().contains("junit.runtime");
	}

}
