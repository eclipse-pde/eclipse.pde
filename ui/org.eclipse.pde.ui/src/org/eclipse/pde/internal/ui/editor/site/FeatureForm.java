package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class FeatureForm extends ScrollableSectionForm {
	private static final String KEY_TITLE = "FeatureForm.title";
	private static final String KEY_PROPERTY_TITLE = "FeatureForm.property.title";
	private static final String KEY_PROPERTY_DESC = "FeatureForm.property.desc";
	private FeaturePage page;
	private SiteFeatureSection featureSection;
	private PropertySection propertySection;
	private CategorySection categorySection;

	public FeatureForm(FeaturePage page) {
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

		featureSection = new SiteFeatureSection(page);
		Control control = featureSection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		control.setLayoutData(gd);

		String title = PDEPlugin.getResourceString(KEY_PROPERTY_TITLE);
		String desc = PDEPlugin.getResourceString(KEY_PROPERTY_DESC);
		propertySection = new PropertySection(page, title, desc, ISiteFeature.class);
		control = propertySection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		control.setLayoutData(gd);
		
		categorySection = new CategorySection(page);
		control = categorySection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		control.setLayoutData(gd);
		
		SectionChangeManager manager = new SectionChangeManager();
		manager.linkSections(featureSection, propertySection);
		manager.linkSections(featureSection, categorySection);

		registerSection(featureSection);
		registerSection(propertySection);
		registerSection(categorySection);
		
		WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_SITE_FEATURES);
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
