package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class ArchiveForm extends ScrollableSectionForm {
	private static final String KEY_TITLE = "ArchiveForm.title";
	private static final String KEY_PROPERTY_TITLE = "ArchiveForm.property.title";
	private static final String KEY_PROPERTY_DESC = "ArchiveForm.property.desc";
	private ArchivePage page;
	private SiteArchiveSection archiveSection;
	private PropertySection propertySection;

	public ArchiveForm(ArchivePage page) {
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

		archiveSection = new SiteArchiveSection(page);
		Control control = archiveSection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_BOTH);
		control.setLayoutData(gd);

		String title = PDEPlugin.getResourceString(KEY_PROPERTY_TITLE);
		String desc = PDEPlugin.getResourceString(KEY_PROPERTY_DESC);
		propertySection = new PropertySection(page, title, desc, ISiteArchive.class);
		control = propertySection.createControl(parent, factory);
		gd =
			new GridData(
				GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		control.setLayoutData(gd);
		
		SectionChangeManager manager = new SectionChangeManager();
		manager.linkSections(archiveSection, propertySection);

		registerSection(archiveSection);
		registerSection(propertySection);
		
		WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_SITE_ARCHIVES);
	}
	
	public void expandTo(Object object) {
		archiveSection.expandTo(object);
	}
	
	public void initialize(Object modelObject) {
		super.initialize(modelObject);
		setHeadingText(PDEPlugin.getResourceString(KEY_TITLE));
		((Composite) getControl()).layout(true);
	}
	
	public void setFocus() {
		archiveSection.setFocus();
	}

}
