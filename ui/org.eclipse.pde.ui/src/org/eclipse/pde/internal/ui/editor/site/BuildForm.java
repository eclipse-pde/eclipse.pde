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

import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class BuildForm extends ScrollableSectionForm {
	private static final String KEY_TITLE = "BuildForm.title";
	private BuildPage page;
	private BuildControlSection controlSection;
	private FeatureProjectSection featureSection;

	public BuildForm(BuildPage page) {
		this.page = page;
		setVerticalFit(true);
	}

	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		layout.makeColumnsEqualWidth=true;
		layout.numColumns = 2;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 15;
		GridData gd;

		featureSection = new FeatureProjectSection(page);
		Control control = featureSection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);

		controlSection = new BuildControlSection(page);
		control = controlSection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);

		registerSection(featureSection);
		registerSection(controlSection);

		WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_SITE_BUILD);
	}

	public void expandTo(Object object) {
		featureSection.expandTo(object);
	}

	public void initialize(Object modelObject) {
		super.initialize(modelObject);
		setHeadingText(PDEPlugin.getResourceString(KEY_TITLE));
		((Composite) getControl()).layout(true);
	}

	public void setFocus() {
		featureSection.setFocus();
	}

}
