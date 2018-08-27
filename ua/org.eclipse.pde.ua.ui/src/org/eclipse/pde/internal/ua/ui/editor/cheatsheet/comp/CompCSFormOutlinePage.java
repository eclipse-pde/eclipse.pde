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

	public CompCSFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	private class CompCSLabelProvider extends BasicLabelProvider {
		public CompCSLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		@Override
		public String getText(Object obj) {
			if (obj instanceof ICompCSObject) {
				return getObjectText((ICompCSObject) obj);
			}
			return super.getText(obj);
		}
	}

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

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof CompCSPage) {
			ICompCSModel cheatsheet = (ICompCSModel) fEditor.getAggregateModel();
			if ((cheatsheet != null) && cheatsheet.isLoaded()) {
				Object[] list = new Object[1];
				list[0] = cheatsheet.getCompCS();
				return list;
			}
		} else if (parent instanceof ICompCSObject) {
			List<? extends ICompCSObject> list = ((ICompCSObject) parent).getChildren();
			// List is never null
			if (list.size() > 0) {
				return list.toArray();
			}
		}
		return super.getChildren(parent);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		return new CompCSLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
	}

	@Override
	protected String getParentPageId(Object item) {
		return CompCSPage.PAGE_ID;
	}

}
