package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.update.ui.forms.internal.*;

public class SiteForm extends ScrollableSectionForm {
	private SitePage page;
	private SiteDescriptionSection descriptionSection;
	private CategoryDefinitionSection categorySection;

	public SiteForm(SitePage page) {
		this.page = page;
		setVerticalFit(true);
	}
	
	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		parent.setLayout(layout);
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth=true;
		layout.marginWidth = 10;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 15;
		GridData gd;

		descriptionSection = new SiteDescriptionSection(page);
		Control control = descriptionSection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);

		categorySection = new CategoryDefinitionSection(page);
		control = categorySection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);

		registerSection(descriptionSection);
		registerSection(categorySection);
	
		//WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_FEATURE_OVERVIEW);
	}
	
	public void expandTo(Object object) {
		categorySection.expandTo(object);
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
