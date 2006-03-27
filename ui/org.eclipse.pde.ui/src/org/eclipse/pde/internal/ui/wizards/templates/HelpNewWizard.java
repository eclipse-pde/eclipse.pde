package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class HelpNewWizard extends NewPluginTemplateWizard {

	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDEUIMessages.HelpNewWizard_wiz);
	}
	
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new HelpTemplate() };
	}

}
