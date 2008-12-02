/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	/**
	 * @param editor
	 */
	public TocFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	/**
	 * TocLabelProvider
	 *
	 */
	private class TocLabelProvider extends BasicLabelProvider {
		public TocLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		public String getText(Object obj) {
			if (obj instanceof TocObject) {
				return getObjectText((TocObject) obj);
			}
			return super.getText(obj);
		}
	}

	/**
	 * @param obj
	 * @return
	 */
	protected String getObjectText(TocObject obj) {
		return PDETextHelper.translateReadText(obj.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#getChildren(java.lang.Object)
	 */
	protected Object[] getChildren(Object parent) {
		if (parent instanceof TocPage) {
			TocModel toc = (TocModel) fEditor.getAggregateModel();
			if (toc != null && toc.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = toc.getToc();
				return list;
			}
		} else if (parent instanceof TocObject) {
			List list = ((TocObject) parent).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}

		return super.getChildren(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#createLabelProvider()
	 */
	public ILabelProvider createLabelProvider() {
		return new TocLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#getParentPageId(java.lang.Object)
	 */
	protected String getParentPageId(Object item) {
		return TocPage.PAGE_ID;
	}
}
