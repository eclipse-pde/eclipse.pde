/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;

public class BundleHyperlink extends ManifestElementHyperlink {

	public BundleHyperlink(IRegion region, String pluginID) {
		super(region, pluginID);
	}

	@Override
	protected void open2() {
		ManifestEditor.openPluginEditor(fElement);
	}

}
