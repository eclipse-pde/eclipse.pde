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

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	@Override
	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	@Override
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	@Override
	public void updateSelection(SelectionChangedEvent e) {
		// NO-OP
	}

	@Override
	protected ISortableContentOutlinePage createOutlinePage() {
		return null;
	}

	@Override
	public void updateSelection(Object object) {
		// NO-OP
	}
}
