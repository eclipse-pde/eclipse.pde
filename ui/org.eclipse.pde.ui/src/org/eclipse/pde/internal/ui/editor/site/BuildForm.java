package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class BuildForm extends ScrollableSectionForm {
	private static final String KEY_TITLE = "BuildForm.title";
	private BuildPage page;
	private BuildControlSection controlSection;
	private FeatureProjectSection featureSection;
	//	private URLSection urlSection;
	//	private FeatureSpecSection specSection;
	//	private PortabilitySection portabilitySection;

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
		/*
		
		urlSection = new URLSection(page);
		control = urlSection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		control.setLayoutData(gd);
		
		portabilitySection = new PortabilitySection(page);
		control = portabilitySection.createControl(parent, factory);
		gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		*/

		registerSection(featureSection);
		registerSection(controlSection);
		//registerSection(urlSection);
		//registerSection(portabilitySection);

		//WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_FEATURE_OVERVIEW);
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
		controlSection.setFocus();
	}

}
