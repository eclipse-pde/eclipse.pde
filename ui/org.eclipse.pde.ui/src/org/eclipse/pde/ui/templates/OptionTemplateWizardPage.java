
package org.eclipse.pde.ui.templates;
import java.util.ArrayList;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class OptionTemplateWizardPage extends WizardPage {
	private OptionTemplateSection section;
	private ArrayList options;
	public OptionTemplateWizardPage(OptionTemplateSection section, ArrayList options) {
		super("something");
		this.section = section;
		this.options = options;
	}
	
	public void createControl(Composite composite) {
		Composite container = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		
		for (int i=0; i<options.size(); i++) {
			TemplateOption option = (TemplateOption)options.get(i);
			option.createControl(container, 2, null);
		}
		setControl(container);
	}

	public void setVisible(boolean visible) {
		if (visible && section.isDependentOnFirstPage()) {
			IWizardPage page1 = getWizard().getStartingPage();
			if (page1 instanceof FirstTemplateWizardPage) {
				FirstTemplateWizardPage firstPage = (FirstTemplateWizardPage)page1;
				FieldData data = firstPage.createFieldData();
				section.initializeFields(firstPage.getStructureData(), data);
			}
		}
		super.setVisible(visible);
	}
}