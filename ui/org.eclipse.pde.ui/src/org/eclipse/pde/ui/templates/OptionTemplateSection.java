/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * This class adds some conventions to the class it is based on. For example, it
 * expects to find the template content in the following location:
 * 
 * <pre>
 * 
 *     [install location]/[templateDirectory]/[sectionId]
 *  
 * </pre>
 * 
 * where <code>templateDirectory</code> is expected to be 'templates_3.0' (to
 * distinguish from template designed for earlier Eclipse versions), and
 * <code>sectionId</code> is the unique identifier as reported by the template
 * section.
 * <p>
 * It also assumes that all wizard pages associated with this template will be
 * based on <code>OptionTemplateWizardPage</code>.
 * 
 * 
 * @since 2.0
 */
public abstract class OptionTemplateSection extends BaseOptionTemplateSection {
	private ArrayList pages = new ArrayList();

	private static class TemplatePage {
		WizardPage page;
		ArrayList options;

		public TemplatePage() {
			options = new ArrayList();
		}
	}

	/**
	 * The default constructor.
	 */
	public OptionTemplateSection() {
	}

	/**
	 * Returns the unique name of this section. This name will be used to
	 * construct name and description lookup keys, as well as the template file
	 * location in the contributing plug-in.
	 * 
	 * @return the unique section Id
	 * @see #getLabel()
	 * @see #getDescription()
	 * @see #getTemplateLocation()
	 */
	public abstract String getSectionId();

	/**
	 * Returns the directory where all the templates are located in the
	 * contributing plug-in.
	 * 
	 * @return "templates_[schemaVersion]" for code since Eclipse 3.0, or
	 *         "templates" for pre-3.0 code.
	 */
	protected String getTemplateDirectory() {
		String schemaVersion = model.getPluginBase().getSchemaVersion();
		if (schemaVersion != null)
			return "templates_" + schemaVersion; //$NON-NLS-1$
		return "templates"; //$NON-NLS-1$
	}

	/**
	 * Returns the install URL of the plug-in that contributes this template.
	 * 
	 * @return the install URL of the contributing plug-in
	 */
	protected abstract URL getInstallURL();

	/**
	 * Implements the abstract method by looking for templates using the
	 * following path:
	 * <p>
	 * [install location]/[templateDirectory]/[sectionId]
	 * 
	 * @return the URL of the location where files to be emitted by this
	 *         template are located.
	 */
	public URL getTemplateLocation() {
		URL url = getInstallURL();
		try {
			String location = getTemplateDirectory() + "/" //$NON-NLS-1$
					+ getSectionId() + "/"; //$NON-NLS-1$
			return new URL(url, location);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Returns the wizard page at the specified index. Pages must be created
	 * prior to calling this method.
	 * 
	 * @param pageIndex the index to get the page for 
	 * @return the wizard page at the specified index or <samp>null </samp> if
	 *         invalid index.
	 * @see #createPage(int)
	 */
	public WizardPage getPage(int pageIndex) {
		if (pageIndex < 0 || pageIndex >= pages.size())
			return null;
		TemplatePage tpage = (TemplatePage) pages.get(pageIndex);
		return tpage.page;
	}

	/**
	 * Creates the wizard page for the specified page index. This method cannot
	 * be called before setPageCount(int). The page will be created with all the
	 * options registered for that page index. Therefore, make all the calls to
	 * addOption() before calling this method.
	 * 
	 * @param pageIndex
	 *            a zero-based index of the page relative to this template. For
	 *            example, if a template needs to have two pages, you have to
	 *            call this method twice (once with index 0 and again with index
	 *            1).
	 * @return the created wizard page
	 * @see #setPageCount(int)
	 * @see BaseOptionTemplateSection#addOption
	 */
	public WizardPage createPage(int pageIndex) {
		if (pageIndex < 0 || pageIndex >= pages.size())
			return null;
		TemplatePage tpage = (TemplatePage) pages.get(pageIndex);
		tpage.page = new OptionTemplateWizardPage(this, tpage.options, null);
		return tpage.page;
	}

	/**
	 * Creates the wizard page for the specified page index. This method cannot
	 * be called before setPageCount(int). The page will be created with all the
	 * options registered for that page index. Therefore, make all the calls to
	 * addOption() before calling this method.
	 * 
	 * @param pageIndex
	 *            a zero-based index of the page relative to this template. For
	 *            example, if a template need to have two pages, you have to
	 *            call this method twice (once with index 0 and again with index
	 *            1).
	 * @param helpContextId
	 *            the Id of the help context defined in the contributing plug-in
	 *            that will be used to locate content of the info-pop displayed
	 *            when F1 is pressed.
	 * @return the created wizard page
	 * @see #setPageCount(int)
	 * @see BaseOptionTemplateSection#addOption
	 */
	public WizardPage createPage(int pageIndex, String helpContextId) {
		if (pageIndex < 0 || pageIndex >= pages.size())
			return null;
		TemplatePage tpage = (TemplatePage) pages.get(pageIndex);
		tpage.page = new OptionTemplateWizardPage(this, tpage.options, helpContextId);
		return tpage.page;
	}

	/**
	 * Returns a number of pages that this template contributes to the wizard.
	 * 
	 * @return the number of pages
	 * @see #setPageCount(int)
	 */
	public int getPageCount() {
		return pages.size();
	}

	/**
	 * Sets the number of pages this template will manage. This method must be
	 * called prior to adding pages and options in order to initialize the
	 * template. Once the method has been called, you can call methods that
	 * accept page index in the range [0..count-1].
	 * 
	 * @param count
	 *            number of pages that this template will contribute to the
	 *            template wizard
	 */
	public void setPageCount(int count) {
		pages.clear();
		for (int i = 0; i < count; i++) {
			pages.add(new TemplatePage());
		}
	}

	/**
	 * Returns options that belong to the page with the given index.
	 * 
	 * @param pageIndex
	 *            0-based index of the template page
	 * @return @see #setPageCount(int)
	 */

	public TemplateOption[] getOptions(int pageIndex) {
		if (pageIndex < 0 || pageIndex >= pages.size())
			return new TemplateOption[0];
		TemplatePage page = (TemplatePage) pages.get(pageIndex);
		return (TemplateOption[]) page.options.toArray(new TemplateOption[page.options.size()]);
	}

	/**
	 * Returns options that are added to the provided wizard page.
	 * 
	 * @param page
	 *            wizard page that hosts required options
	 * @return array of options added to the provided wizard page
	 */

	public TemplateOption[] getOptions(WizardPage page) {
		for (int i = 0; i < pages.size(); i++) {
			TemplatePage tpage = (TemplatePage) pages.get(i);
			if (tpage.page.equals(page))
				return getOptions(i);
		}
		return new TemplateOption[0];
	}

	/**
	 * Returns the zero-based index of a page that hosts the the given option.
	 * 
	 * @param option
	 *            template option for which a page index is being requested
	 * @return zero-based index of a page that hosts the option or -1 if none of
	 *         the pages contain the option.
	 */
	public int getPageIndex(TemplateOption option) {
		for (int i = 0; i < pages.size(); i++) {
			TemplatePage tpage = (TemplatePage) pages.get(i);
			if (tpage.options.contains(option))
				return i;
		}
		return -1;
	}

	/**
	 * Returns the label of this template to be used in the UI. The label is
	 * obtained by creating a lookup key using the following rule:
	 * "template.[section-id].name". This key is used to locate the label in the
	 * plugin.properties file of the plug-in that contributed this template.
	 * 
	 * @return the translated label of this template
	 */
	public String getLabel() {
		String key = "template." + getSectionId() + ".name"; //$NON-NLS-1$ //$NON-NLS-2$
		return getPluginResourceString(key);
	}

	/**
	 * Returns the description of this template to be used in the UI. The
	 * description is obtained by creating a lookup key using the following
	 * rule: "template.[section-id].desc". This key is used to locate the label
	 * in the plugin.properties file of the plug-in that contributed this
	 * template.
	 * 
	 * @return the translated description of this template
	 */
	public String getDescription() {
		String key = "template." + getSectionId() + ".desc"; //$NON-NLS-1$ //$NON-NLS-2$
		return getPluginResourceString(key);
	}

	/**
	 * Locates the page that this option is presented in and flags that the
	 * option is required and is currently not set. The flagging is done by
	 * setting the page incomplete and setting the error message that uses
	 * option's message label.
	 * 
	 * @param option
	 *            the option that is required and currently not set
	 */
	protected void flagMissingRequiredOption(TemplateOption option) {
		WizardPage page = null;
		for (int i = 0; i < pages.size(); i++) {
			TemplatePage tpage = (TemplatePage) pages.get(i);
			ArrayList list = tpage.options;
			if (list.contains(option)) {
				page = tpage.page;
				break;
			}
		}
		if (page != null) {
			page.setPageComplete(false);
			String message = NLS.bind(PDEUIMessages.OptionTemplateSection_mustBeSet, option.getMessageLabel());
			page.setErrorMessage(message);
		}
	}

	/**
	 * Resets the current page state by clearing the error message and making
	 * the page complete, thereby allowing users to flip to the next page.
	 */
	protected void resetPageState() {
		if (pages.size() == 0)
			return;
		WizardPage firstPage = ((TemplatePage) pages.get(0)).page;
		IWizardContainer container = firstPage.getWizard().getContainer();
		WizardPage currentPage = (WizardPage) container.getCurrentPage();
		currentPage.setErrorMessage(null);
		currentPage.setPageComplete(true);
	}

	protected void registerOption(TemplateOption option, Object value, int pageIndex) {
		super.registerOption(option, value, pageIndex);
		if (pageIndex >= 0 && pageIndex < pages.size()) {
			TemplatePage tpage = (TemplatePage) pages.get(pageIndex);
			tpage.options.add(option);
		}
	}

	/**
	 * Validate options given a template option
	 * 
	 * @param source the template option to validate
	 */
	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		}
		validateContainerPage(source);
	}

	private void validateContainerPage(TemplateOption source) {
		TemplateOption[] allPageOptions = getOptions(0);
		for (int i = 0; i < allPageOptions.length; i++) {
			TemplateOption nextOption = allPageOptions[i];
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}

}