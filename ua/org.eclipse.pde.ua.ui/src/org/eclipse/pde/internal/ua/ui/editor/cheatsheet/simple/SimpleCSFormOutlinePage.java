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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class SimpleCSFormOutlinePage extends FormOutlinePage {

	/**
	 * @param editor
	 */
	public SimpleCSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	/**
	 * SimpleCSLabelProvider
	 *
	 */
	public class SimpleCSLabelProvider extends BasicLabelProvider {
		public SimpleCSLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		public String getText(Object obj) {
			if (obj instanceof ISimpleCSObject) {
				return getObjectText((ISimpleCSObject) obj);
			}
			return super.getText(obj);
		}
	}

	/**
	 * @param obj
	 * @return
	 */
	protected String getObjectText(ISimpleCSObject obj) {
		int limit = 50;

		if (obj.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
			limit = 30;
		} else if (obj.getType() == ISimpleCSConstants.TYPE_ITEM) {
			limit = 26;
		} else if (obj.getType() == ISimpleCSConstants.TYPE_INTRO) {
			limit = 26;
		} else if (obj.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			limit = 22;
		}

		return PDETextHelper.truncateAndTrailOffText(PDETextHelper.translateReadText(obj.getName()), limit);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#getChildren(java.lang.Object)
	 */
	protected Object[] getChildren(Object parent) {
		if (parent instanceof SimpleCSDefinitionPage) {
			ISimpleCSModel cheatsheet = (ISimpleCSModel) fEditor.getAggregateModel();
			if ((cheatsheet != null) && cheatsheet.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = cheatsheet.getSimpleCS();
				return list;
			}
		} else if (parent instanceof ISimpleCSObject) {
			List list = ((ISimpleCSObject) parent).getChildren();
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
		return new SimpleCSLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#getParentPageId(java.lang.Object)
	 */
	protected String getParentPageId(Object item) {
		return SimpleCSDefinitionPage.PAGE_ID;
	}

}
