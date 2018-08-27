/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class TocFormOutlinePage extends FormOutlinePage {
	public TocFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	private class TocLabelProvider extends BasicLabelProvider {
		public TocLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		@Override
		public String getText(Object obj) {
			if (obj instanceof TocObject) {
				return getObjectText((TocObject) obj);
			}
			return super.getText(obj);
		}
	}

	protected String getObjectText(TocObject obj) {
		return PDETextHelper.translateReadText(obj.getName());
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof TocPage) {
			TocModel toc = (TocModel) fEditor.getAggregateModel();
			if (toc != null && toc.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = toc.getToc();
				return list;
			}
		} else if (parent instanceof TocObject) {
			List<TocObject> list = ((TocObject) parent).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}

		return super.getChildren(parent);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		return new TocLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	@Override
	protected String getParentPageId(Object item) {
		return TocPage.PAGE_ID;
	}
}
