
package org.eclipse.pde.internal.wizards.templates;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import java.util.ArrayList;

public class GenericTemplateWizardPage extends WizardPage {
	private GenericTemplateSection section;
	private ArrayList options;
	public GenericTemplateWizardPage(GenericTemplateSection section, ArrayList options) {
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
}