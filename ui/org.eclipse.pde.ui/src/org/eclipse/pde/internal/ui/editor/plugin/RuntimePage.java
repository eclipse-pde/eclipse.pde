/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;

public class RuntimePage extends PDEFormPage {
	public static final String PAGE_ID="runtime"; //$NON-NLS-1$
	private LibrarySection librarySection;
	private ExportSection exportSection;
	private PluginActivationSection osgiSection;

	public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString("RuntimePage.tabName"));  //$NON-NLS-1$
	}
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setText(PDEPlugin.getResourceString("ManifestEditor.RuntimeForm.title")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 20;
		layout.makeColumnsEqualWidth = true;
		
		librarySection = new LibrarySection(this, form.getBody());
		librarySection.getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		exportSection = new ExportSection(this, form.getBody());
		exportSection.getSection().setLayoutData( new GridData(GridData.FILL_BOTH));
		
		boolean fragment = ((IPluginModelBase)getPDEEditor().getAggregateModel()).isFragmentModel();
		if (!fragment){
			osgiSection = new PluginActivationSection(this, form.getBody());
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			osgiSection.getSection().setLayoutData(gd);
		}
		
		mform.addPart(librarySection);
		mform.addPart(exportSection);
		if (!fragment)
			mform.addPart(osgiSection);
		
		
		
		if (fragment)
			WorkbenchHelp.setHelp(form, IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			WorkbenchHelp.setHelp(form, IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}
}
