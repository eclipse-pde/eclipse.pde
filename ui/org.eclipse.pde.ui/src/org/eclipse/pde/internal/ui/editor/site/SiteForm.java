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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class SiteForm extends ScrollableSectionForm {
	private SitePage page;
	private DescriptionSection descriptionSection;
	private FeatureSection featureSection;
	private ArchiveSection archiveSection;

	public SiteForm(SitePage page) {
		this.page = page;
		setVerticalFit(true);
	}
	
	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth=true;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 15;

		descriptionSection = new DescriptionSection(page);
		Control control = descriptionSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		featureSection = new FeatureSection(page);
		control = featureSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		archiveSection = new ArchiveSection(page);
		control = archiveSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		registerSection(descriptionSection);
		registerSection(featureSection);
		registerSection(archiveSection);
	
		WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_SITE_OVERVIEW);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.AbstractSectionForm#dispose()
	 */
	public void dispose() {
		unregisterSection(descriptionSection);
		unregisterSection(featureSection);
		super.dispose();
	}
	
	public void expandTo(Object object) {
	}
	
	public void initialize(Object modelObject) {
		ISiteModel model = (ISiteModel) modelObject;
		super.initialize(model);
		IEditorInput input = page.getEditor().getEditorInput();
		String name = input.getName();
		setHeadingText(model.getResourceString(name));
		((Composite) getControl()).layout(true);
	}
	
	public void setFocus() {
		descriptionSection.setFocus();
	}
}
