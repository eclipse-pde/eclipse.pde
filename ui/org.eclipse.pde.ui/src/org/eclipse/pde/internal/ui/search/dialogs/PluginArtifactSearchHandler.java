/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import org.eclipse.core.commands.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.editor.plugin.PluginInputContext;
import org.eclipse.pde.internal.ui.search.ManifestEditorOpener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class PluginArtifactSearchHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		FilteredPluginArtifactsSelectionDialog dialog = new FilteredPluginArtifactsSelectionDialog(window.getShell());
		int status = dialog.open();
		if (status == Window.OK) {
			Object[] result = dialog.getResult();
			Object object = result[0];
			IEditorPart editorPart = ManifestEditor.open(object, true);
			if (editorPart != null && editorPart instanceof ManifestEditor) {
				ManifestEditor editor = (ManifestEditor) editorPart;
				InputContext context = editor.getContextManager().findContext(PluginInputContext.CONTEXT_ID);
				IDocument document = context.getDocumentProvider().getDocument(context.getInput());
				IRegion region = ManifestEditorOpener.getAttributeMatch(editor, (IPluginObject) object, document);
				editor.openToSourcePage(object, region.getOffset(), region.getLength());
			} else {
				ManifestEditor.openPluginEditor((IPluginModelBase) object);
			}
		}

		return null;
	}
}
