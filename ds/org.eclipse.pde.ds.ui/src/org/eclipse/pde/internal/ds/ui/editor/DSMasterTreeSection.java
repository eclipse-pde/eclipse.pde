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

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSMasterTreeSection extends TreeSection implements IDSMaster {

	public DSMasterTreeSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.SimpleCSElementSection_0, PDEUIMessages.SimpleCSElementSection_6, null, null, PDEUIMessages.SimpleCSElementSection_7, PDEUIMessages.SimpleCSElementSection_1, PDEUIMessages.SimpleCSElementSection_2, null, null, PDEUIMessages.SimpleCSElementSection_3});
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		// TODO Auto-generated method stub
		
	}

	public void fireSelection() {
		// TODO Auto-generated method stub
		
	}

	public void updateButtons() {
		// TODO Auto-generated method stub
		
	}

}
