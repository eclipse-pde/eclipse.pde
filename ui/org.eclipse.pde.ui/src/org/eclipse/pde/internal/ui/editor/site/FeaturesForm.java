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

import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.ui.forms.internal.*;

public class FeaturesForm extends ScrollableSectionForm {
	private FeaturesPage page;
	private FeatureSection featureSection;
	private CategorySection categorySection;

	public FeaturesForm(FeaturesPage page) {
		this.page = page;
		setVerticalFit(true);
	}
	
	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.horizontalSpacing = 12;
		layout.marginWidth = 10;

		featureSection = new FeatureSection(page);
		Control control = featureSection.createControl(parent, factory);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 250;
		control.setLayoutData(gd);
		
		categorySection = new CategorySection(page);
		control = categorySection.createControl(parent, factory); 
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 250;
		control.setLayoutData(gd);
		
		registerSection(featureSection);
	
		WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_SITE_OVERVIEW);
	}
	
	public void dispose() {
		unregisterSection(featureSection);
		unregisterSection(categorySection);
		super.dispose();
	}
	
	public void initialize(Object modelObject) {
		ISiteModel model = (ISiteModel) modelObject;
		super.initialize(model);
		IEditorInput input = page.getEditor().getEditorInput();
		String name = input.getName();
		setHeadingText(model.getResourceString(name));
		((Composite) getControl()).layout(true);
	}
}
