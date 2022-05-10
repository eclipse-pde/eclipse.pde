/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.pde.spy.event.internal.util;

import org.eclipse.pde.spy.event.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class PluginUtils {
	public static String getContributorURI() {
		return String.format("platform:/plugin/%s", Constants.PLUGIN_ID); //$NON-NLS-1$
	}

	public static String getContributionURI(Class<?> contributionCls) {
		return String.format("bundleclass://%s/%s", Constants.PLUGIN_ID, contributionCls.getName()); //$NON-NLS-1$
	}

	public static String getBundleId(Class<?> cls) {
		Bundle bundle = FrameworkUtil.getBundle(cls);
		if (bundle == null) {
			throw new IllegalArgumentException(Messages.PluginUtils_CannotFindBundleForClass + cls.getName());
		}
		return bundle.getSymbolicName();
	}
}
