/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.TransferData;

public class ElementSectionDropAdapter extends ViewerDropAdapter {
	private TransferData currentTransfer;
	private ElementSection section;

	public ElementSectionDropAdapter(ElementSection section) {
		super(section.getTreeViewer());
		this.section = section;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	public boolean performDrop(Object data) {
		if (data instanceof Object[]) {
			section.doPaste(getCurrentTarget(), (Object[])data);
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	public boolean validateDrop(
		Object target,
		int operation,
		TransferData transferType) {
		currentTransfer = transferType;
		if (currentTransfer != null
			&& ModelDataTransfer.getInstance().isSupportedType(
				currentTransfer)) {
			return validateTarget();
		}
		return false;
	}

	private boolean validateTarget() {
		Object target = getCurrentTarget();
		if (target == null || target instanceof ISchemaObject)
			return true;
		else
			return false;
	}

}