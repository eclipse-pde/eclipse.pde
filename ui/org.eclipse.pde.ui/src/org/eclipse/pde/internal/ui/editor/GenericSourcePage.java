/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.*;

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
	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createOutlineContentProvider()
	 */
	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createOutlineContentProvider()
	 */
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#outlineSelectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void updateSelection(SelectionChangedEvent e) {
		// NO-OP
	}

	protected ISortableContentOutlinePage createOutlinePage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		// NO-OP
	}
}
