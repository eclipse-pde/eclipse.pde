/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.text.TranslationHyperlink;
import org.eclipse.swt.widgets.Display;

/**
 * TranslatableAttributeRow
 *
 */
public class TranslatableAttributeRow extends ReferenceAttributeRow {

	/**
	 * @param part
	 * @param att
	 */
	public TranslatableAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	@Override
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}

	@Override
	protected void openReference() {
		TranslationHyperlink link = new TranslationHyperlink(null, text.getText(), (IPluginModelBase) part.getPage().getModel());
		link.open();
		if (!link.getOpened()) {
			Display.getCurrent().beep();
		}
	}

}
