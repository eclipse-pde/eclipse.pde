/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.plugin.rows;

import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.util.PDESchemaHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.editor.plugin.PluginInputContext;
import org.eclipse.pde.internal.ui.search.ManifestEditorOpener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class IdAttributeRow extends ButtonAttributeRow {

	private class IdAttributeLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof Map.Entry) {
				Map.Entry<?, ?> entry = (Entry<?, ?>) element;
				String text = (String) entry.getKey();
				if (entry.getValue() instanceof IConfigurationElement) {
					IConfigurationElement value = (IConfigurationElement) entry.getValue();
					String name = value.getAttribute("name"); //$NON-NLS-1$
					if (name == null) {
						name = value.getAttribute("label"); //$NON-NLS-1$
						if (name == null) {
							name = value.getAttribute("description"); //$NON-NLS-1$
						}
					}

					String contributor = value.getContributor().getName();

					if (input != null && name != null && name.startsWith("%") && contributor != null) { //$NON-NLS-1$
						IPluginModelBase model = PluginRegistry.findModel(contributor);
						name = model.getResourceString(name);
					}

					if (name != null) {
						text += " - " + name; //$NON-NLS-1$
					}
					if (contributor != null)
						text += " [" + contributor + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return text;
			}
			return super.getText(element);
		}
	}

	public IdAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	@Override
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}

	@Override
	protected void browse() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new IdAttributeLabelProvider());

		dialog.setTitle(PDEUIMessages.IdAttributeRow_title);
		dialog.setMessage(PDEUIMessages.IdAttributeRow_message);
		dialog.setEmptyListMessage(PDEUIMessages.IdAttributeRow_emptyMessage);
		Map<String, IConfigurationElement> attributeMap = PDESchemaHelper.getValidAttributes(getAttribute());
		dialog.setElements(attributeMap.entrySet().toArray());
		dialog.setFilter("*"); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			Map.Entry<?, ?> entry = (Entry<?, ?>) dialog.getFirstResult();
			text.setText(entry.getKey().toString());
		}
	}

	@Override
	protected void openReference() {
		Map<String, IConfigurationElement> attributeMap = PDESchemaHelper.getValidAttributes(getAttribute());
		String id = text.getText();
		// TODO this is hackish
		IConfigurationElement element = attributeMap.get(id);
		if (element != null) {
			String pluginId = element.getContributor().getName();
			IPluginModelBase model = PluginRegistry.findModel(pluginId);
			IEditorPart editorPart = ManifestEditor.open(model.getPluginBase(), true);
			ManifestEditor editor = (ManifestEditor) editorPart;
			if (editor != null) {
				InputContext context = editor.getContextManager().findContext(PluginInputContext.CONTEXT_ID);
				IDocument document = context.getDocumentProvider().getDocument(context.getInput());
				IRegion region = ManifestEditorOpener.getAttributeMatch(editor, id, document);
				if (region == null) {
					// see bug 248248 for why we have this check
					id = id.substring(id.lastIndexOf('.') + 1, id.length());
					region = ManifestEditorOpener.getAttributeMatch(editor, id, document);
				}
				editor.openToSourcePage(context, region.getOffset(), region.getLength());
			}
		}
	}
}
