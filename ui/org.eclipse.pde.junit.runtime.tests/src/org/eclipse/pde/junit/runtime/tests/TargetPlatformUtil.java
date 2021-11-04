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
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;

public class TargetPlatformUtil {

	private static final String TARGET_NAME = TargetPlatformUtil.class + "_target";

	public static void setRunningPlatformAsTarget() throws Exception {
		Predicate<Bundle> filter = b -> !isJunitRuntime(b);
		org.eclipse.pde.ui.tests.util.TargetPlatformUtil.setRunningPlatformSubSetAsTarget(TARGET_NAME, filter);
	}

	private static final Pattern JUNIT_RUNTIME_IDS = Pattern
			.compile("\\.junit\\d*\\.runtime|junit\\..+\\.engine$|org.junit.platform.launcher");

	private static boolean isJunitRuntime(Bundle bundle) {
		// filter out junit.runtime and test engine bundles from the target platform
		// this tests the scenario where PDE supplies them from the host installation
		return JUNIT_RUNTIME_IDS.matcher(bundle.getSymbolicName()).find();
	}

}
