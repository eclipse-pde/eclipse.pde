package org.eclipse.pde.ui.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.net.*;
import java.util.ArrayList;

import org.eclipse.jface.wizard.*;

public abstract class OptionTemplateSection extends BaseOptionTemplateSection {
	private ArrayList pages = new ArrayList();

	class TemplatePage {
		WizardPage page;
		ArrayList options;
		public TemplatePage() {
			options = new ArrayList();
		}
	}

	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public OptionTemplateSection() {
	}

/**
 * Returns the unique name of this section. This name will
 * be used to construct name and description lookup keys,
 * as well as the template file location in the contributing
 * plug-in.
 * @return the unique section Id
 * @see #getLabel()
 * @see #getDescription()
 * @see #getTemplateLocation()
 */	
	public abstract String getSectionId();

/**
 * Returns the directory where all the templates are located 
 * in the contributing plug-in. Default implementation is
 * "templates".
 * @return "templates"
 */
	protected String getTemplateDirectory() {
		return "templates";
	}
/**
 * Returns the install URL of the plug-in that contributes this
 * template. Implement it by accessing the top-level class of
 * your plug-in and calling 'getDescriptor().getInstallURL()'.
 * @return the install URL of the contributing plug-in
 */	
	protected abstract URL getInstallURL();

/**
 * Implements the abstract method by looking for templates using
 * the following path:
 * <p>
 * [install location]/[templateDirectory]/[sectionId]
 * @return the URL of the location where files to be emitted by
 * this template are located.
 */	
	public URL getTemplateLocation() {
		URL url = getInstallURL();
		try {
			String location = getTemplateDirectory()+File.separator+getSectionId();
			return new URL(url, location);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}
/**
 * Returns the wizard page at the specified index.
 * @return the wizard page at the specified index or <samp>null</samp>
 * if invalid index.
 */	
	public WizardPage getPage(int pageIndex) {
		if (pageIndex<0 || pageIndex>=pages.size()) return null;
		TemplatePage tpage = (TemplatePage)pages.get(pageIndex);
		return tpage.page;
	}
	
	public WizardPage createPage(int pageIndex) {
		if (pageIndex<0 || pageIndex>=pages.size()) return null;
		TemplatePage tpage = (TemplatePage)pages.get(pageIndex);
		tpage.page = new OptionTemplateWizardPage(this, tpage.options);
		return tpage.page;
	}
/**
 * Returns a number of pages that this template contributes
 * to the wizard.
 */
	
	public int getPageCount() {
		return pages.size();
	}

/**
 * Sets the number of pages this template will manage. This 
 * method must be called prior to adding pages and options in order
 * to initialize the template.
 */
	public void setPageCount(int count) {
		pages.clear();
		for (int i=0; i<count; i++) {
			pages.add(new TemplatePage());
		}
	}
	
/**
 * Returns options that belong to the page with the given index.
 * @param pageIndex 0-based index of the template page
 */
	
	public TemplateOption [] getOptions(int pageIndex) {
		if (pageIndex <0 || pageIndex>=pages.size()) return new TemplateOption[0];
		TemplatePage page = (TemplatePage)pages.get(pageIndex);
		return (TemplateOption[])page.options.toArray(new TemplateOption[page.options.size()]);
	}
	
	public TemplateOption [] getOptions(WizardPage page) {
		for (int i=0; i<pages.size(); i++) {
			TemplatePage tpage = (TemplatePage)pages.get(i);
			if (tpage.page.equals(page))
				return getOptions(i);
		}
		return new TemplateOption [0];
	}
	
	public int getPageIndex(TemplateOption option) {
		for (int i=0; i<pages.size(); i++) {
			TemplatePage tpage = (TemplatePage)pages.get(i);
			if (tpage.options.contains(option)) return i;
		}
		return -1;
	}
	
/**
 * Returns the label of this template to be used in the UI.
 * The label is obtained by creating a lookup key using the 
 * following rule: "template.[section-id].name". This key is
 * used to locate the label in the plugin.properties file
 * of the plug-in that contributed this template.
 * @return the translated label of this template
 */
	public String getLabel() {
		String key = "template."+getSectionId()+".name";
		return getPluginResourceString(key);
	}
/**
 * Returns the description of this template to be used in the UI.
 * The description is obtained by creating a lookup key using the 
 * following rule: "template.[section-id].desc". This key is
 * used to locate the label in the plugin.properties file
 * of the plug-in that contributed this template.
 * @return the translated description of this template
 */	
	public String getDescription() {
		String key = "template."+getSectionId()+".desc";
		return getPluginResourceString(key);
	}
/**
 * Locates the page that this option is presented in and
 * flags that the option is required and is currently not set.
 * The flagging is done by setting the page incomplete and
 * setting the error message that uses option's message label.
 * @param option the option that is required and currently not set
 */
	protected void flagMissingRequiredOption(TemplateOption option) {
		WizardPage page = null;
		for (int i=0; i<pages.size(); i++) {
			TemplatePage tpage = (TemplatePage)pages.get(i);
			ArrayList list = tpage.options;
			if (list.contains(option)) {
				page = tpage.page;
				break;
			}
		}
		if (page!=null) {
			page.setPageComplete(false);
			String message = "Template option \""+option.getMessageLabel()+"\" must be set.";
			page.setErrorMessage(message);
		}
	}

/**
 * Resets the current page state by clearing the error message
 * and making the page complete, thereby allowing users to flip
 * to the next page.
 */
	protected void resetPageState() {
		if (pages.size()==0) return;
		WizardPage firstPage = ((TemplatePage)pages.get(0)).page;
		IWizardContainer container = firstPage.getWizard().getContainer();
		WizardPage currentPage = (WizardPage)container.getCurrentPage();
		currentPage.setErrorMessage(null);
		currentPage.setPageComplete(true);
	}

	void registerOption(TemplateOption option, Object value, int pageIndex) {
		super.registerOption(option, value, pageIndex);
		if (pageIndex>=0 && pageIndex < pages.size()) {
			TemplatePage tpage = (TemplatePage)pages.get(pageIndex);
			tpage.options.add(option);
		}
	}
}