/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpModel;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

/**
 * The outline page for the context help editor
 * @since 3.4
 * @see CtxHelpEditor
 */
public class CtxHelpFormOutlinePage extends FormOutlinePage {

	public CtxHelpFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	/**
	 * Label provider for the outline
	 */
	private class CtxHelpLabelProvider extends BasicLabelProvider {
		public CtxHelpLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		@Override
		public String getText(Object obj) {
			if (obj instanceof CtxHelpObject) {
				return PDETextHelper.translateReadText(((CtxHelpObject) obj).getName());
			}
			return super.getText(obj);
		}
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof CtxHelpPage) {
			CtxHelpModel model = (CtxHelpModel) fEditor.getAggregateModel();
			if (model != null && model.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = model.getCtxHelpRoot();
				return list;
			}
		} else if (parent instanceof CtxHelpObject) {
			List<CtxHelpObject> list = ((CtxHelpObject) parent).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}

		return super.getChildren(parent);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		return new CtxHelpLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	@Override
	protected String getParentPageId(Object item) {
		return CtxHelpPage.PAGE_ID;
	}
}
