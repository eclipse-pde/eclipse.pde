/*******************************************************************************
 *  Copyright (c) 2000, 2007 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.internal.ui.templates.PDETemplateSection;
import org.eclipse.pde.ui.templates.BooleanOption;
import org.eclipse.pde.ui.templates.TemplateOption;

public class HelpTemplate extends PDETemplateSection {

	public static final String KEY_TOC_LABEL = "tocLabel"; //$NON-NLS-1$
	public static final String KEY_IS_PRIMARY = "isPrimary"; //$NON-NLS-1$
	public static final String KEY_GEN_TEST = "generateTest"; //$NON-NLS-1$
	public static final String KEY_GET_STARTED = "gettingStarted"; //$NON-NLS-1$
	public static final String KEY_CONCEPTS = "concepts"; //$NON-NLS-1$
	public static final String KEY_TASKS = "tasks"; //$NON-NLS-1$
	public static final String KEY_REFERENCE = "reference"; //$NON-NLS-1$
	public static final String KEY_SAMPLES = "samples"; //$NON-NLS-1$

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

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_HELP);
		page.setTitle(PDETemplateMessages.HelpTemplate_title);
		page.setDescription(PDETemplateMessages.HelpTemplate_desc);
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
		tocLabelOption = addOption(KEY_TOC_LABEL, PDETemplateMessages.HelpTemplate_tocLabel, PDETemplateMessages.HelpTemplate_sampleText, 0);

		primaryOption = (BooleanOption) addOption(KEY_IS_PRIMARY, PDETemplateMessages.HelpTemplate_isPrimary, false, 0);

		genTestOption = (BooleanOption) addOption(KEY_GEN_TEST, PDETemplateMessages.HelpTemplate_generateTest, true, 0);

		gettingStartedOption = (BooleanOption) addOption(KEY_GET_STARTED, PDETemplateMessages.HelpTemplate_gettingStarted, true, 0);

		conceptsOption = (BooleanOption) addOption(KEY_CONCEPTS, PDETemplateMessages.HelpTemplate_concepts, true, 0);

		tasksOption = (BooleanOption) addOption(KEY_TASKS, PDETemplateMessages.HelpTemplate_tasks, true, 0);

		referenceOption = (BooleanOption) addOption(KEY_REFERENCE, PDETemplateMessages.HelpTemplate_reference, true, 0);

		samplesOption = (BooleanOption) addOption(KEY_SAMPLES, PDETemplateMessages.HelpTemplate_samples, true, 0);

	}

	@Override
	public String getSectionId() {
		return "help"; //$NON-NLS-1$
	}

	@Override
	protected boolean isOkToCreateFolder(File sourceFolder) {
		boolean isOk = true;
		String folderName = sourceFolder.getName();
		switch (folderName) {
		case "concepts": //$NON-NLS-1$
			isOk = conceptsOption.isEnabled() && conceptsOption.isSelected();
			break;
		case "gettingstarted": //$NON-NLS-1$
			isOk = gettingStartedOption.isEnabled() && gettingStartedOption.isSelected();
			break;
		case "reference": //$NON-NLS-1$
			isOk = referenceOption.isEnabled() && referenceOption.isSelected();
			break;
		case "samples": //$NON-NLS-1$
			isOk = samplesOption.isEnabled() && samplesOption.isSelected();
			break;
		case "tasks": //$NON-NLS-1$
			isOk = tasksOption.isEnabled() && tasksOption.isSelected();
			break;
		default:
			break;
		}
		return isOk;
	}

	@Override
	protected boolean isOkToCreateFile(File sourceFile) {
		boolean isOk = true;
		String fileName = sourceFile.getName();
		if (fileName.equals("testToc.xml")) { //$NON-NLS-1$
			isOk = genTestOption.isEnabled() && genTestOption.isSelected();
		} else if (fileName.equals("tocconcepts.xml")) { //$NON-NLS-1$
			isOk = conceptsOption.isEnabled() && conceptsOption.isSelected();
		} else if (fileName.equals("tocgettingstarted.xml")) { //$NON-NLS-1$
			isOk = gettingStartedOption.isEnabled() && gettingStartedOption.isSelected();
		} else if (fileName.equals("tocreference.xml")) { //$NON-NLS-1$
			isOk = referenceOption.isEnabled() && referenceOption.isSelected();
		} else if (fileName.equals("tocsamples.xml")) { //$NON-NLS-1$
			isOk = samplesOption.isEnabled() && samplesOption.isSelected();
		} else if (fileName.equals("toctasks.xml")) { //$NON-NLS-1$
			isOk = tasksOption.isEnabled() && tasksOption.isSelected();
		} else if ((fileName.equals("maintopic.html") || fileName.equals("subtopic.html")) //$NON-NLS-1$ //$NON-NLS-2$
				&& sourceFile.getParentFile().getName().equals("html")) { //$NON-NLS-1$
			isOk = !primaryOption.isSelected() || (primaryOption.isSelected() && !gettingStartedOption.isSelected() && !conceptsOption.isSelected() && !tasksOption.isSelected() && !referenceOption.isSelected() && !samplesOption.isSelected());
		}
		return isOk;
	}

	@Override
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

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement tocElement = factory.createElement(extension);
		tocElement.setName("toc"); //$NON-NLS-1$
		tocElement.setAttribute("file", "toc.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		if (primaryOption.isSelected())
			tocElement.setAttribute("primary", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(tocElement);

		if (genTestOption.isSelected() && genTestOption.isEnabled()) {
			IPluginElement testTocElement = factory.createElement(extension);
			testTocElement.setName("toc"); //$NON-NLS-1$
			testTocElement.setAttribute("file", "testToc.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			testTocElement.setAttribute("primary", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			extension.add(testTocElement);
		}
		addNonPrimaryTopic(conceptsOption, "tocconcepts.xml", extension); //$NON-NLS-1$
		addNonPrimaryTopic(gettingStartedOption, "tocgettingstarted.xml", extension); //$NON-NLS-1$
		addNonPrimaryTopic(referenceOption, "tocreference.xml", extension); //$NON-NLS-1$
		addNonPrimaryTopic(samplesOption, "tocsamples.xml", extension); //$NON-NLS-1$
		addNonPrimaryTopic(tasksOption, "toctasks.xml", extension); //$NON-NLS-1$

		if (!extension.isInTheModel())
			plugin.add(extension);
	}

	private void addNonPrimaryTopic(BooleanOption option, String file, IPluginExtension extension) throws CoreException {
		if (option.isEnabled() && option.isSelected()) {
			IPluginElement tocElement = extension.getPluginModel().getPluginFactory().createElement(extension);
			tocElement.setName("toc"); //$NON-NLS-1$
			tocElement.setAttribute("file", file); //$NON-NLS-1$
			extension.add(tocElement);
		}
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.help.toc"; //$NON-NLS-1$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[0];
	}

	@Override
	public String[] getNewFiles() {
		return new String[] {"html/", "*.xml"}; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
