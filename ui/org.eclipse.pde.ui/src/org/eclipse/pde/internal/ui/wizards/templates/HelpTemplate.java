/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.templates;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.templates.BooleanOption;
import org.eclipse.pde.ui.templates.TemplateOption;

public class HelpTemplate extends PDETemplateSection {
	
	public static final String KEY_TOC_LABEL   = "tocLabel";
	public static final String KEY_IS_PRIMARY  = "isPrimary";
	public static final String KEY_GEN_TEST    = "generateTest";
	public static final String KEY_GET_STARTED = "gettingStarted";
	public static final String KEY_CONCEPTS    = "concepts";
	public static final String KEY_TASKS       = "tasks";
	public static final String KEY_REFERENCE   = "reference";
	public static final String KEY_SAMPLES     = "samples";
	
	private static final String NL_TOC_LABEL   = "HelpTemplate.tocLabel";
	private static final String NL_IS_PRIMARY  = "HelpTemplate.isPrimary";
	private static final String NL_GEN_TEST    = "HelpTemplate.generateTest";
	private static final String NL_GET_STARTED = "HelpTemplate.gettingStarted";
	private static final String NL_CONCEPTS    = "HelpTemplate.concepts";
	private static final String NL_TASKS       = "HelpTemplate.tasks";
	private static final String NL_REFERENCE   = "HelpTemplate.reference";
	private static final String NL_SAMPLES     = "HelpTemplate.samples";
	
	private static final String NL_DESC        = "HelpTemplate.desc";
	private static final String NL_TITLE       = "HelpTemplate.title";
	
	private TemplateOption tocLabelOption;
	private BooleanOption primaryOption;
	private BooleanOption genTestOption;
	private BooleanOption gettingStartedOption;
	private BooleanOption conceptsOption;
	private BooleanOption tasksOption;
	private BooleanOption referenceOption;
	private BooleanOption samplesOption;
	
	public HelpTemplate() {
		setPageCount(1);
		createOptions();
		alterOptionStates();
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_HELP);
		page.setTitle(PDEPlugin.getResourceString(NL_TITLE));
		page.setDescription(PDEPlugin.getResourceString(NL_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}
	
	private void alterOptionStates() {
		genTestOption.setEnabled(!primaryOption.isSelected());
		gettingStartedOption.setEnabled(primaryOption.isSelected());
		conceptsOption.setEnabled(primaryOption.isSelected());
		tasksOption.setEnabled(primaryOption.isSelected());
		referenceOption.setEnabled(primaryOption.isSelected());
		samplesOption.setEnabled(primaryOption.isSelected());
	}	

	private void createOptions() {
		tocLabelOption = addOption(
			KEY_TOC_LABEL,
			PDEPlugin.getResourceString(NL_TOC_LABEL),
			"Sample Table of Contents",
			0);
			
		primaryOption = (BooleanOption)addOption(
			KEY_IS_PRIMARY,
			PDEPlugin.getResourceString(NL_IS_PRIMARY),
			false,
			0);
			
		genTestOption = (BooleanOption)addOption(
			KEY_GEN_TEST,
			PDEPlugin.getResourceString(NL_GEN_TEST),
			true,
			0);

		gettingStartedOption = (BooleanOption)addOption(
			KEY_GET_STARTED,
			PDEPlugin.getResourceString(NL_GET_STARTED),
			true,
			0);
			
		conceptsOption = (BooleanOption)addOption(
			KEY_CONCEPTS,
			PDEPlugin.getResourceString(NL_CONCEPTS),
			true,
			0);
			
		tasksOption = (BooleanOption)addOption(
			KEY_TASKS,
			PDEPlugin.getResourceString(NL_TASKS),
			true,
			0);
			
		referenceOption = (BooleanOption)addOption(
			KEY_REFERENCE,
			PDEPlugin.getResourceString(NL_REFERENCE),
			true,
			0);
			
		samplesOption = (BooleanOption)addOption(
			KEY_SAMPLES,
			PDEPlugin.getResourceString(NL_SAMPLES),
			true,
			0);

	}
	/**
	 * @see OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "help";
	}

	protected boolean isOkToCreateFolder(File sourceFolder) {
		boolean isOk = true;
		String folderName = sourceFolder.getName();
		if (folderName.equals("concepts")) {
			isOk = conceptsOption.isEnabled() && conceptsOption.isSelected();
		} else if (folderName.equals("gettingstarted")) {
			isOk = gettingStartedOption.isEnabled() && gettingStartedOption.isSelected();
		} else if (folderName.equals("reference")) {
			isOk = referenceOption.isEnabled() && referenceOption.isSelected();
		} else if (folderName.equals("samples")) {
			isOk = samplesOption.isEnabled() && samplesOption.isSelected();
		} else if (folderName.equals("tasks")) {
			isOk = tasksOption.isEnabled() && tasksOption.isSelected();
		}
		return isOk;
	}
	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	protected boolean isOkToCreateFile(File sourceFile) {
		boolean isOk = true;
		String fileName = sourceFile.getName();
		if (fileName.equals("testToc.xml")) {
			isOk = genTestOption.isEnabled() && genTestOption.isSelected();
		} else if (fileName.equals("tocconcepts.xml")) {
			isOk = conceptsOption.isEnabled() && conceptsOption.isSelected();
		} else if (fileName.equals("tocgettingstarted.xml")) {
			isOk = gettingStartedOption.isEnabled() && gettingStartedOption.isSelected();
		} else if (fileName.equals("tocreference.xml")) {
			isOk = referenceOption.isEnabled() && referenceOption.isSelected();
		} else if (fileName.equals("tocsamples.xml")) {
			isOk = samplesOption.isEnabled() && samplesOption.isSelected();
		} else if (fileName.equals("toctasks.xml")) {
			isOk = tasksOption.isEnabled() && tasksOption.isSelected();
		} else if (
			(fileName.equals("maintopic.html") || fileName.equals("subtopic.html"))
				&& sourceFile.getParentFile().getName().equals("html")) {
			isOk =
				!primaryOption.isSelected()
					|| (primaryOption.isSelected()
						&& !gettingStartedOption.isSelected()
						&& !conceptsOption.isSelected()
						&& !tasksOption.isSelected()
						&& !referenceOption.isSelected()
						&& !samplesOption.isSelected());
		}
		return isOk;	
	}
	/**
	 * @see BaseOptionTemplateSection#validateOptions(TemplateOption)
	 */
	public void validateOptions(TemplateOption changed) {
		if (changed == tocLabelOption) {
			if (changed.isEmpty()) {
				flagMissingRequiredOption(changed);
			} else {
				resetPageState();
			}
		} else if (changed == primaryOption) {
			alterOptionStates();
		}
	}

	/**
	 * @see AbstractTemplateSection#updateModel(IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement tocElement = factory.createElement(extension);
		tocElement.setName("toc");
		tocElement.setAttribute("file","toc.xml");
		if (primaryOption.isSelected()) tocElement.setAttribute("primary","true");
		extension.add(tocElement);
		
		if (genTestOption.isSelected() && genTestOption.isEnabled()) {
			IPluginElement testTocElement = factory.createElement(extension);
			testTocElement.setName("toc");
			testTocElement.setAttribute("file","testToc.xml");
			testTocElement.setAttribute("primary","true");
			extension.add(testTocElement);
		}
		addNonPrimaryTopic(conceptsOption, "tocconcepts.xml", extension);
		addNonPrimaryTopic(gettingStartedOption, "tocgettingstarted.xml", extension);
		addNonPrimaryTopic(referenceOption, "tocreference.xml", extension);
		addNonPrimaryTopic(samplesOption, "tocsamples.xml", extension);
		addNonPrimaryTopic(tasksOption, "toctasks.xml", extension);
		
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
	
	private void addNonPrimaryTopic(BooleanOption option, String file, IPluginExtension extension) throws CoreException {
		if (option.isEnabled() && option.isSelected()) {
			IPluginElement tocElement = extension.getPluginModel().getPluginFactory().createElement(extension);
			tocElement.setName("toc");
			tocElement.setAttribute("file", file);
			extension.add(tocElement);
		}
	}

	/**
	 * @see ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.help.toc";
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[] {new PluginReference("org.eclipse.help", null, 0)};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFoldersToInclude()
	 */
	public String[] getNewFiles() {
		return new String[] {"html/", "*.xml"};
	}

}
