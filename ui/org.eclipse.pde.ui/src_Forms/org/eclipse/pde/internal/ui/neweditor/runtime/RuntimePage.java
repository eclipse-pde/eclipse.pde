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
package org.eclipse.pde.internal.ui.neweditor.runtime;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormPage;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class RuntimePage extends PDEFormPage {
	public static final String PAGE_ID="runtime";
	private LibrarySection librarySection;
	private ExportSection exportSection;
	private PackagePrefixesSection prefixesSection;
	private LibraryTypeSection typeSection;

	public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, "Runtime");
	}
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setText(PDEPlugin.getResourceString("ManifestEditor.RuntimeForm.title"));
		FormToolkit toolkit = mform.getToolkit();
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 10;
		layout.makeColumnsEqualWidth = true;

		librarySection = new LibrarySection(this, form.getBody());
		librarySection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

		prefixesSection = new PackagePrefixesSection(this, form.getBody());
		prefixesSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

		typeSection = new LibraryTypeSection(this, form.getBody());
		typeSection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

		exportSection = new ExportSection(this, form.getBody());
		exportSection.getSection().setLayoutData( new GridData(GridData.FILL_BOTH));

		mform.addPart(librarySection);
		mform.addPart(typeSection);
		mform.addPart(exportSection);
		mform.addPart(prefixesSection);
		
		boolean fragment = ((IPluginModelBase)getPDEEditor().getAggregateModel()).isFragmentModel();

		if (fragment)
			WorkbenchHelp.setHelp(form, IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			WorkbenchHelp.setHelp(form, IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}
}
