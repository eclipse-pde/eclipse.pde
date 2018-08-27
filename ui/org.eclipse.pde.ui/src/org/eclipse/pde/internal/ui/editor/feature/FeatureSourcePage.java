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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.*;

public class FeatureSourcePage extends XMLSourcePage {

	public FeatureSourcePage(PDEFormEditor editor, String id, String title) {
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
		// TODO remove this method when the above three stubs
		// are implemented
		return new FeatureOutlinePage((PDEFormEditor) getEditor());
	}

	@Override
	public boolean isQuickOutlineEnabled() {
		return false;
	}

	@Override
	public void updateSelection(Object object) {
		// NO-OP
	}
}
