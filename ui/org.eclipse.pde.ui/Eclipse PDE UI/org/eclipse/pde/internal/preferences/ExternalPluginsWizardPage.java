package org.eclipse.pde.internal.preferences;

import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.jface.wizard.*;

public class ExternalPluginsWizardPage extends WizardPage {
	public static final String KEY_TITLE = "ExternalPluginsWizard.title";
	public static final String KEY_DESC = "ExternalPluginsWizard.desc";
	private ExternalPluginsBlock pluginsBlock;

public ExternalPluginsWizardPage() {
	super("externalPluginsPage");
	pluginsBlock = new ExternalPluginsBlock(null);
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NONE);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);
	GridData gd = new GridData(GridData.FILL_BOTH);
	container.setLayoutData(gd);

	Control control = pluginsBlock.createContents(container);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	pluginsBlock.initialize(PDEPlugin.getDefault().getPreferenceStore());
	setControl(container);
}
public boolean finish() {
	pluginsBlock.save(PDEPlugin.getDefault().getPreferenceStore());
	return true;
}
}
