/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class BuildActivator extends Plugin {
	@Override
	public void start(BundleContext ctx) throws Exception {
		new BundleHelper(ctx);
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		BundleHelper.close();
	}
}
