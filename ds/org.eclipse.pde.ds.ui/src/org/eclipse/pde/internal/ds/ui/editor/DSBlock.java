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
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;

public class DSBlock extends PDEMasterDetailsBlock implements IModelChangedListener, IDetailsPageProvider {

	public DSBlock(PDEFormPage page) {
		super(page);
	}

	protected PDESection createMasterSection(IManagedForm managedForm,
			Composite parent) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void registerPages(DetailsPart arg0) {
		// TODO Auto-generated method stub
		
	}

	public void modelChanged(IModelChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	public IDetailsPage getPage(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getPageKey(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
