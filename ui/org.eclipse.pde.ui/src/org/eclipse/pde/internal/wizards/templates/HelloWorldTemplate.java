
package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.IBasePluginWizard;
import org.eclipse.pde.ITemplateSection;
import org.eclipse.pde.model.plugin.IPluginModelBase;
import org.eclipse.pde.model.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.PDEPlugin;
import java.net.*;

public class HelloWorldTemplate extends AbstractTemplateSection {
	public static final String KEY_CLASS_NAME = "className";
	public static final String CLASS_NAME = "SampleAction";
	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public HelloWorldTemplate() {
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits()+1;
	}
	
	public URL getTemplateLocation() {
		URL url = PDEPlugin.getDefault().getDescriptor().getInstallURL();
		try {
			return new URL(url, "templates/helloWorld");
		}
		catch (MalformedURLException e) {
			return null;
		}
	}
	
	public String getReplacementString(String fileName, String key) {
		if (key.equals(KEY_CLASS_NAME))
			return CLASS_NAME;
		return super.getReplacementString(fileName, key);
	}
	
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint("org.eclipse.ui.actionSets");
		IPluginModelFactory factory = model.getFactory();

		IPluginElement setElement = factory.createElement(extension);
		setElement.setName("actionSet");
		setElement.setAttribute("id", plugin.getId()+".actionSet");
		setElement.setAttribute("label", "Sample Action Set");
		setElement.setAttribute("visible", "true");
		
		IPluginElement menuElement = factory.createElement(setElement);
		menuElement.setName("menu");
		menuElement.setAttribute("label", "Sample &Menu");
		menuElement.setAttribute("id", "sampleMenu");

		IPluginElement groupElement = factory.createElement(menuElement);
		groupElement.setName("separator");
		groupElement.setAttribute("name", "sampleGroup");
		menuElement.add(groupElement);
		setElement.add(menuElement);
		
		IPluginElement actionElement = factory.createElement(setElement);
		actionElement.setName("action");
		actionElement.setAttribute("id", plugin.getId()+".sampleAction");
		actionElement.setAttribute("label", "&Sample Action");
		actionElement.setAttribute("menubarPath", "sampleMenu/sampleGroup");
		actionElement.setAttribute("toolbarPath", "sampleGroup");
		actionElement.setAttribute("icon", "icons/sample.gif");
		actionElement.setAttribute("tooltip", "Hello Eclipse World");
		actionElement.setAttribute("class", plugin.getId()+"."+CLASS_NAME);
		setElement.add(actionElement);
		extension.add(setElement);
		plugin.add(extension);
	}
}
