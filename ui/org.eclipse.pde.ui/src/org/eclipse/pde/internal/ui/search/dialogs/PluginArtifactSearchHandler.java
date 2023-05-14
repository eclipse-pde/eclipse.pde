/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262564
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.BundleInputContext;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.editor.plugin.PluginInputContext;
import org.eclipse.pde.internal.ui.search.ManifestEditorOpener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class PluginArtifactSearchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		FilteredPluginArtifactsSelectionDialog dialog = new FilteredPluginArtifactsSelectionDialog(window.getShell());
		int status = dialog.open();
		if (status == Window.OK) {
			Object[] result = dialog.getResult();
			Object object = result[0];
			if (object instanceof IFeatureModel) {
				FeatureEditor.openFeatureEditor((IFeatureModel) object);
			} else {
				IEditorPart editorPart = ManifestEditor.open(object, true);
				if (editorPart != null && editorPart instanceof ManifestEditor) {
					ManifestEditor editor = (ManifestEditor) editorPart;
					InputContext context = getInputContext(object, editor);
					IDocument document = context.getDocumentProvider().getDocument(context.getInput());
					IRegion region = ManifestEditorOpener.getAttributeMatch(editor, object, document);
					editor.openToSourcePage(object, region.getOffset(), region.getLength());
				} else {
					ManifestEditor.openPluginEditor((IPluginModelBase) object);
				}
			}
		}
		return null;
	}

	private InputContext getInputContext(Object object, ManifestEditor editor) {
		if (object instanceof BaseDescription)
			return editor.getContextManager().findContext(BundleInputContext.CONTEXT_ID);
		return editor.getContextManager().findContext(PluginInputContext.CONTEXT_ID);
	}

}
