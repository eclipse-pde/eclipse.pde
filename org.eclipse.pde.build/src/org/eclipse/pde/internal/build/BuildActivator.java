/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class BuildActivator extends Plugin {
	public void start(BundleContext ctx) throws Exception {
		new BundleHelper(ctx);
	}

	public void stop(BundleContext ctx) throws Exception {
		BundleHelper.close();
	}
}
