/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.pde.internal.runtime.registry.RegistryBrowserContentProvider.BundleFolder;
import org.osgi.framework.Bundle;

/**
 * Adapter for bundle objects.
 *
 */
public class PluginAdapter extends ParentAdapter {

	public PluginAdapter(Bundle object) {
		super(object);
	}

	protected Object[] createChildren() {
		Bundle bundle = (Bundle) getObject();

		Object[] array = new Object[7];
		array[0] = new BundleFolder(bundle, IBundleFolder.F_LOCATION);
		array[1] = new BundleFolder(bundle, IBundleFolder.F_IMPORTS);
		array[2] = new BundleFolder(bundle, IBundleFolder.F_LIBRARIES);
		array[3] = new BundleFolder(bundle, IBundleFolder.F_EXTENSION_POINTS);
		array[4] = new BundleFolder(bundle, IBundleFolder.F_EXTENSIONS);
		array[5] = new BundleFolder(bundle, IBundleFolder.F_REGISTERED_SERVICES);
		array[6] = new BundleFolder(bundle, IBundleFolder.F_SERVICES_IN_USE);
		return array;
	}
}
