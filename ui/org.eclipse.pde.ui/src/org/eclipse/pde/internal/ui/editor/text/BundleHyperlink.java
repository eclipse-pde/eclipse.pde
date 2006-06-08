/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	protected void open2() {
		ManifestEditor.openPluginEditor(fElement);
	}

}
