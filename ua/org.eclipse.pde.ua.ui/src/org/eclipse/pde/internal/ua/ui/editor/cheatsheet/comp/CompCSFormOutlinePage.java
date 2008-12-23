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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class CompCSFormOutlinePage extends FormOutlinePage {

	/**
	 * @param editor
	 */
	public CompCSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	/**
	 * CompCSLabelProvider
	 *
	 */
	private class CompCSLabelProvider extends BasicLabelProvider {
		public CompCSLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		public String getText(Object obj) {
			if (obj instanceof ICompCSObject) {
				return getObjectText((ICompCSObject) obj);
			}
			return super.getText(obj);
		}
	}

	/**
	 * @param obj
	 * @return
	 */
	protected String getObjectText(ICompCSObject obj) {
		int limit = 50;

		if (obj.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			limit = 30;
		} else if (obj.getType() == ICompCSConstants.TYPE_TASK) {
			limit = 26;
		} else if (obj.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			limit = 22;
		}

		return PDETextHelper.truncateAndTrailOffText(PDETextHelper.translateReadText(obj.getName()), limit);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#getChildren(java.lang.Object)
	 */
	protected Object[] getChildren(Object parent) {
		if (parent instanceof CompCSPage) {
			ICompCSModel cheatsheet = (ICompCSModel) fEditor.getAggregateModel();
			if ((cheatsheet != null) && cheatsheet.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = cheatsheet.getCompCS();
				return list;
			}
		} else if (parent instanceof ICompCSObject) {
			List list = ((ICompCSObject) parent).getChildren();
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
		return new CompCSLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#getParentPageId(java.lang.Object)
	 */
	protected String getParentPageId(Object item) {
		return CompCSPage.PAGE_ID;
	}

}
