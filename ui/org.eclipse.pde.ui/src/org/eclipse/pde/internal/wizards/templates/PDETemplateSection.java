
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.IBasePluginWizard;
import org.eclipse.pde.model.plugin.IPluginModelBase;
import org.eclipse.pde.model.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.PDEPlugin;
import java.net.*;
import java.util.*;
import org.eclipse.jface.wizard.*;
import java.io.File;

public abstract class PDETemplateSection extends GenericTemplateSection {
	public static final String TEMPLATE_DIRECTORY = "templates";
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public PDETemplateSection() {
	}
	
	public abstract String getSectionId();
	
	protected ResourceBundle getPluginResourceBundle() {
		return PDEPlugin.getDefault().getDescriptor().getResourceBundle();
	}
	
	public URL getTemplateLocation() {
		URL url = PDEPlugin.getDefault().getDescriptor().getInstallURL();
		try {
			String location = TEMPLATE_DIRECTORY+File.separator+getSectionId();
			return new URL(url, location);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}
	
	public String getReplacementString(String fileName, String key) {
		String value = getStringOption(key);
		if (value!=null) return value;
		return super.getReplacementString(fileName, key);
	}
	
	public String getLabel() {
		String key = "template."+getSectionId()+".name";
		return getPluginResourceString(key);
	}
	
	public String getDescription() {
		String key = "template."+getSectionId()+".desc";
		return getPluginResourceString(key);
	}
}
