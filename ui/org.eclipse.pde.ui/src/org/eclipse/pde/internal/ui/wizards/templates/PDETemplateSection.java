
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import java.net.*;
import java.util.*;

public abstract class PDETemplateSection extends OptionTemplateSection {

	protected ResourceBundle getPluginResourceBundle() {
		return PDEPlugin.getDefault().getDescriptor().getResourceBundle();
	}
	
	protected URL getInstallURL() {
		return PDEPlugin.getDefault().getDescriptor().getInstallURL();
	}
}