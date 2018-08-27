/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.editor.*;

public class BuildOutlinePage extends FormOutlinePage {
	/**
	 * @param editor
	 */
	public BuildOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof PDEFormPage) {
			PDEFormPage page = (PDEFormPage) parent;
			IBuildModel model = (IBuildModel) page.getModel();
			if (model.isValid()) {
				IBuild build = model.getBuild();
				if (page.getId().equals(BuildPage.PAGE_ID))
					return build.getBuildEntries();
			}
		}
		return new Object[0];
	}

	@Override
	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof IBuildEntry)
			pageId = BuildPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}
}
