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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.*;

public class RuntimeForm extends ScrollableSectionForm {
	private ManifestRuntimePage page;
	private LibrarySection librarySection;
	private ExportSection exportSection;
	private PackagePrefixesSection prefixesSection;
	private LibraryTypeSection typeSection;

	public RuntimeForm(ManifestRuntimePage page) {
		this.page = page;
		setVerticalFit(true);
	}
	protected void createFormClient(Composite parent) {
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 10;
		layout.makeColumnsEqualWidth = true;

		librarySection = new LibrarySection(page);
		Control control = librarySection.createControl(parent, getFactory());
		control.setLayoutData(new GridData(GridData.FILL_BOTH));

		prefixesSection = new PackagePrefixesSection(page);
		control = prefixesSection.createControl(parent, getFactory());
		control.setLayoutData(new GridData(GridData.FILL_BOTH));

		typeSection = new LibraryTypeSection(page);
		control = typeSection.createControl(parent, getFactory());
		control.setLayoutData(new GridData(GridData.FILL_BOTH));

		exportSection = new ExportSection(page);
		control = exportSection.createControl(parent, getFactory());
		control.setLayoutData( new GridData(GridData.FILL_BOTH));

		// Link
		SectionChangeManager manager = new SectionChangeManager();
		manager.linkSections(librarySection, typeSection);
		manager.linkSections(librarySection, exportSection);
		manager.linkSections(librarySection, prefixesSection);

		registerSection(librarySection);
		registerSection(typeSection);
		registerSection(exportSection);
		registerSection(prefixesSection);

		if (((ManifestEditor) page.getEditor()).isFragmentEditor())
			WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}
	
	public void expandTo(Object object) {
		librarySection.expandTo(object);
	}
	
	public void initialize(Object model) {
		setHeadingText(PDEPlugin.getResourceString("ManifestEditor.RuntimeForm.title"));
		super.initialize(model);
		((Composite) getControl()).layout(true);
	}
}
