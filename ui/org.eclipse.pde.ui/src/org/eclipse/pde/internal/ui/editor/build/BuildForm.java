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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class BuildForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "BuildEditor.Form.title";
	public static final String FORM_RTITLE = "BuildEditor.Form.rtitle";
	private BuildPage page;
	private VariableSection variableSection;
	private TokenSection tokenSection;

	public BuildForm(BuildPage page) {
		this.page = page;
		setScrollable(false);
		//setVerticalFit(true);
	}
	
	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.makeColumnsEqualWidth = true;

		variableSection = new VariableSection(page);
		Control control = variableSection.createControl(parent, factory);
		GridData gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);

		tokenSection = new TokenSection(page);
		control = tokenSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);

		// Link
		SectionChangeManager manager = new SectionChangeManager();
		manager.linkSections(variableSection, tokenSection);

		registerSection(variableSection);
		registerSection(tokenSection);
		
		WorkbenchHelp.setHelp(parent, IHelpContextIds.BUILD_PAGE);
	}
	
	public void expandTo(Object object) {
		variableSection.expandTo(object);
	}
	
	public void initialize(Object modelObject) {
		IBuildModel model = (IBuildModel) modelObject;

		super.initialize(model);
		String title = "";
		if (model instanceof IEditable && model.isEditable() == false) {
			title = PDEPlugin.getResourceString(FORM_RTITLE);
		} else
			title = PDEPlugin.getResourceString(FORM_TITLE);
		setHeadingText(title);
		((Composite) getControl()).layout(true);
	}
	
	public void setFocus() {
	}

}
