package org.eclipse.pde.ui.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * An implementation of the standard wizard page that
 * creates its contents from the list of template options.
 * The options will be created in the order they are
 * added to the list. When the page is made visible,
 * options that require late initialization will be
 * given a chance to initialize.
 */

public class OptionTemplateWizardPage extends WizardPage {
	private BaseOptionTemplateSection section;
	private ArrayList options;

	/**
	 * The constructor. 
	 * @param section the section that is contributing this page
	 * @param option a list of options that should be shown in this page.
	 */
	public OptionTemplateWizardPage(
		BaseOptionTemplateSection section,
		ArrayList options) {
		super("something");
		this.section = section;
		this.options = options;
	}
	/**
	 * Creates the page control by creating individual options in the
	 * order subject to their position in the list.
	 */
	public void createControl(Composite composite) {
		Composite container = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		for (int i = 0; i < options.size(); i++) {
			TemplateOption option = (TemplateOption) options.get(i);
			option.createControl(container, 2);
		}
		setControl(container);
	}
	/**
	 * Initializes the options that require late initialization when
	 * the page is made visible.
	 */
	public void setVisible(boolean visible) {
		if (visible && section.isDependentOnFirstPage()) {
			IWizardPage page1 = getWizard().getStartingPage();
			if (page1 instanceof FirstTemplateWizardPage) {
				FirstTemplateWizardPage firstPage = (FirstTemplateWizardPage) page1;
				FieldData data = firstPage.createFieldData();
				section.initializeFields(firstPage.getStructureData(), data);
			}
		}
		super.setVisible(visible);
	}
}