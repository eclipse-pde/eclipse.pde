/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.ui.forms.editor.FormEditor;

public class DSPage extends PDEFormPage implements IModelChangedListener {
	
	public static final String PAGE_ID = "dsPage";
	
	private DSBlock fBlock;
	
	public DSPage(FormEditor editor) {
		super(editor, PAGE_ID, Messages.DSPage_title);

		fBlock = new DSBlock(this);
	}
	
	public PDEMasterDetailsBlock getBlock() {
		return fBlock;
	}

	public void modelChanged(IModelChangedEvent event) {
		//TODO ds.modelChanged content
	}

}
