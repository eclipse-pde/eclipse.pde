/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 *
 */
public class GenericSourcePage extends PDESourcePage {
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public GenericSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createOutlineLabelProvider()
	 */
	protected ILabelProvider createOutlineLabelProvider() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createOutlineContentProvider()
	 */
	protected ITreeContentProvider createOutlineContentProvider() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createOutlineContentProvider()
	 */
	protected ViewerSorter createOutlineSorter() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#outlineSelectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	protected void outlineSelectionChanged(SelectionChangedEvent e) {
	}
	protected ISortableContentOutlinePage createOutlinePage() {
		return null;
	}
}
