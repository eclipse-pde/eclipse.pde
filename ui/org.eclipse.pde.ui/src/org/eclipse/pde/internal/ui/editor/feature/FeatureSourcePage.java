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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.*;

public class FeatureSourcePage extends XMLSourcePage {

	public FeatureSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	protected ILabelProvider createOutlineLabelProvider() {
		return null;
	}
	protected ITreeContentProvider createOutlineContentProvider() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	protected ViewerSorter createOutlineSorter() {
		// TODO Auto-generated method stub
		return null;
	}
	protected void outlineSelectionChanged(SelectionChangedEvent e) {
	}
	protected ISortableContentOutlinePage createOutlinePage() {
		//TODO remove this method when the above three stubs
		// are implemented
		return null;
	}
}
