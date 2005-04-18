/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;

/**
 * 
 * Features page.
 */
public class ArchivePage extends PDEFormPage {
	public static final String PAGE_ID = "archives"; //$NON-NLS-1$
	private DescriptionSection fDescSection;
	private MirrorsSection fMirrorsSection;
	private ArchiveSection fArchiveSection;
	
	public ArchivePage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.ArchivePage_name);  //$NON-NLS-1$
	}
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		form.getBody().setLayout(layout);
		
		fDescSection = new DescriptionSection(this, form.getBody());
		fDescSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fArchiveSection = new ArchiveSection(this, form.getBody());
		fArchiveSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fMirrorsSection = new MirrorsSection(this, form.getBody());
		fMirrorsSection.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		mform.addPart(fDescSection);
		mform.addPart(fMirrorsSection);
		mform.addPart(fArchiveSection);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_SITE_ARCHIVES);
		form.setText(PDEUIMessages.ArchivePage_title); //$NON-NLS-1$
	}
}
