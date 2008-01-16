/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.ui.editor.plugin.BundleFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.plugin.PluginFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.toc.TocFoldingStructureProvider;

public class FoldingStructureProviderFactory {

	public static IFoldingStructureProvider createProvider(PDESourcePage editor, IEditingModel model) {
		if (model instanceof PluginModel) {
			return new PluginFoldingStructureProvider(editor, model);
		}
		if (model instanceof BundleModel) {
			return new BundleFoldingStructureProvider(editor, model);
		}
		if (model instanceof TocModel) {
			return new TocFoldingStructureProvider(editor, model);
		}
		return null;
	}

}
