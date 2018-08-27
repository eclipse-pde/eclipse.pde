/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.model.xml;

import org.eclipse.pde.core.plugin.IPluginExtension;

public class ExtensionTestCase extends XMLModelTestCase {

	protected final IPluginExtension reloadModel() {
		reload();
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		return extensions[0];
	}
}
