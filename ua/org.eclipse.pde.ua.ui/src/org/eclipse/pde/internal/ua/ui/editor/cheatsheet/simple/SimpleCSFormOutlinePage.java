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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class SimpleCSFormOutlinePage extends FormOutlinePage {

	public SimpleCSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	public class SimpleCSLabelProvider extends BasicLabelProvider {
		public SimpleCSLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		@Override
		public String getText(Object obj) {
			if (obj instanceof ISimpleCSObject) {
				return getObjectText((ISimpleCSObject) obj);
			}
			return super.getText(obj);
		}
	}

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

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof SimpleCSDefinitionPage) {
			ISimpleCSModel cheatsheet = (ISimpleCSModel) fEditor.getAggregateModel();
			if ((cheatsheet != null) && cheatsheet.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = cheatsheet.getSimpleCS();
				return list;
			}
		} else if (parent instanceof ISimpleCSObject) {
			List<IDocumentElementNode> list = ((ISimpleCSObject) parent).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return super.getChildren(parent);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		return new SimpleCSLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	@Override
	protected String getParentPageId(Object item) {
		return SimpleCSDefinitionPage.PAGE_ID;
	}

}
