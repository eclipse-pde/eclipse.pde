/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.views.plugins.ImportActionGroup;
import org.eclipse.pde.internal.ui.views.plugins.JavaSearchActionGroup;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;

public class PluginSearchResultPage extends AbstractSearchResultPage {

	class SearchLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			if (element instanceof IFeaturePlugin) {
				return getImage(((IFeaturePlugin) element).getFeature().getModel());
			}

			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		@Override
		public String getText(Object object) {
			if (object instanceof IPluginBase)
				return ((IPluginBase) object).getId();

			if (object instanceof IPluginImport) {
				IPluginImport dep = (IPluginImport) object;
				return dep.getId() + " - " //$NON-NLS-1$
						+ dep.getPluginBase().getId();
			}

			if (object instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension) object;
				return extension.getPoint() + " - " + extension.getPluginBase().getId(); //$NON-NLS-1$
			}

			if (object instanceof IPluginExtensionPoint) {
				return ((IPluginExtensionPoint) object).getFullId();
			}

			if (object instanceof IFeaturePlugin) {
				final IFeaturePlugin featurePlugin = (IFeaturePlugin) object;
				final String pluginId = featurePlugin.getId();
				final String featureId = featurePlugin.getFeature().getId();
				return pluginId + " - " + featureId; //$NON-NLS-1$
			}

			return PDEPlugin.getDefault().getLabelProvider().getText(object);
		}
	}

	public PluginSearchResultPage() {
		super();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		mgr.add(new Separator());
		IStructuredSelection selection = getViewer().getStructuredSelection();
		ActionContext context = new ActionContext(selection);
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(context);
		actionGroup.fillContextMenu(mgr);
		if (ImportActionGroup.canImport(selection)) {
			mgr.add(new Separator());
			ImportActionGroup importActionGroup = new ImportActionGroup();
			importActionGroup.setContext(context);
			importActionGroup.fillContextMenu(mgr);
		}
		mgr.add(new Separator());

		JavaSearchActionGroup jsActionGroup = new JavaSearchActionGroup();
		jsActionGroup.setContext(new ActionContext(selection));
		jsActionGroup.fillContextMenu(mgr);
	}

	@Override
	protected ILabelProvider createLabelProvider() {
		return new SearchLabelProvider();
	}

	@Override
	protected ViewerComparator createViewerComparator() {
		return new ViewerComparator();
	}

	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		if (match.getElement() instanceof IFeaturePlugin) {
			IFeaturePlugin featurePlugin = (IFeaturePlugin) match.getElement();
			FeatureEditor.openFeatureEditor(featurePlugin);
			return;
		}
		ManifestEditorOpener.open(match, activate);
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

}
