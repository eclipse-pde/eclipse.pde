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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.build.BuildSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.plugin.BundleSourcePage;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.ManifestConfiguration;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;

public class SourceViewerConfigurationFactory {

	public static ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(PDESourcePage page, IColorManager manager) {
		if (page instanceof XMLSourcePage)
			return new XMLConfiguration(manager, page);
		if (page instanceof BundleSourcePage)
			return new ManifestConfiguration(manager, page);
		if (page instanceof BuildSourcePage) {
			IPreferenceStore store = JavaPlugin.getDefault().getCombinedPreferenceStore();
			((BuildSourcePage)page).setPreferenceStore(store);
			return new BuildSourceViewerConfiguration(manager, store, page);
		}
		return null;
	}
	
}
