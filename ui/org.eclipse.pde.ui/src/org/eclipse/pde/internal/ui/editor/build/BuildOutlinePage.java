/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.editor.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class BuildOutlinePage extends FormOutlinePage {
	/**
	 * @param editor
	 */
	public BuildOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
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
	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof IBuildEntry)
			pageId = BuildPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}
}
