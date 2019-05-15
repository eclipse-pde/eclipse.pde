/*******************************************************************************
 * Copyright (c) 2019 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;

public class ProductSourcePage extends XMLSourcePage {

	public ProductSourcePage(PDEFormEditor editor, String id, String title) {
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
		return new ProductOutlinePage((PDEFormEditor) getEditor());
	}

	@Override
	public boolean isQuickOutlineEnabled() {
		return false;
	}

	@Override
	public void updateSelection(Object object) {
		// NO-OP
	}

	@Override
	protected void setPartName(String partName) {
		super.setPartName(PDEUIMessages.EditorSourcePage_name);
	}
}