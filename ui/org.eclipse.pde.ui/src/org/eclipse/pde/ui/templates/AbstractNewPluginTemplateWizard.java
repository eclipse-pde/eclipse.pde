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
package org.eclipse.pde.ui.templates;
import java.io.*;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
/**
 * This class is used as a common base for plug-in content wizards that are
 * implemented using PDE template support. The assumption is that one or more
 * templates will be used to generate plug-in content. Dependencies, new files
 * and wizard pages are all computed based on the templates.
 * 
 * @since 2.0
 */
public abstract class AbstractNewPluginTemplateWizard extends Wizard
		implements
			IPluginContentWizard {
	private static final String KEY_WTITLE = "PluginCodeGeneratorWizard.title"; //$NON-NLS-1$
	private IFieldData data;
	/**
	 * Creates a new template wizard.
	 */
	public AbstractNewPluginTemplateWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setNeedsProgressMonitor(true);
	}
	/**
	 * @see org.eclipse.pde.ui.IPluginContentWizard#init(IFieldData)
	 */
	public void init(IFieldData data) {
		this.data = data;
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	/**
	 * Returns the field data passed to the wizard during the initialization.
	 * 
	 * @return the parent wizard field data
	 */
	public IFieldData getData() {
		return data;
	}
	/**
	 * This wizard adds a mandatory first page. Subclasses implement this method
	 * to add additional pages to the wizard.
	 */
	protected abstract void addAdditionalPages();
	/**
	 * Implements wizard method. Subclasses cannot override it.
	 */
	public final void addPages() {
		addAdditionalPages();
	}
	/**
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// do nothing - all the work is in the other 'performFinish'
		return true;
	}
	/**
	 * Implements the interface method by looping through template sections and
	 * executing them sequentially.
	 * 
	 * @param project
	 *            the project
	 * @param model
	 *            the plug-in model
	 * @param monitor
	 *            the progress monitor to track the execution progress as part
	 *            of the overall new project creation operation
	 * @return <code>true</code> if the wizard completed the operation with
	 *         success, <code>false</code> otherwise.
	 */
	public boolean performFinish(IProject project, IPluginModelBase model,
			IProgressMonitor monitor) {
		try {
			ITemplateSection[] sections = getTemplateSections();
			monitor.beginTask("", sections.length); //$NON-NLS-1$
			for (int i = 0; i < sections.length; i++) {
				sections[i].execute(project, model, new SubProgressMonitor(
						monitor, 1));
			}
			//No reason to do this any more with the new editors
			//saveTemplateFile(project, null);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		} finally {
			monitor.done();
		}
		return true;
	}
	/**
	 * Returns the template sections used in this wizard.
	 * 
	 * @return the array of template sections
	 */
	public abstract ITemplateSection[] getTemplateSections();
	/**
	 * @see org.eclipse.pde.ui.IPluginContentWizard#getDependencies(String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		ITemplateSection[] sections = getTemplateSections();
		for (int i = 0; i < sections.length; i++) {
			IPluginReference[] refs = sections[i]
					.getDependencies(schemaVersion);
			for (int j = 0; j < refs.length; j++) {
				if (!result.contains(refs[j]))
					result.add(refs[j]);
			}
		}
		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}
	/**
	 * @see org.eclipse.pde.ui.IPluginContentWizard#getNewFiles()
	 */
	public String[] getNewFiles() {
		ArrayList result = new ArrayList();
		ITemplateSection[] sections = getTemplateSections();
		for (int i = 0; i < sections.length; i++) {
			String[] newFiles = sections[i].getNewFiles();
			for (int j = 0; j < newFiles.length; j++) {
				if (!result.contains(newFiles[j]))
					result.add(newFiles[j]);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}
	private void saveTemplateFile(IProject project, IProgressMonitor monitor) {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		writeTemplateFile(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
		}
		String contents = swriter.toString();
		IFile file = project.getFile(".template"); //$NON-NLS-1$
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(contents
					.getBytes("UTF8")); //$NON-NLS-1$
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
				file.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} catch (IOException e) {
		}
	}
	private void writeTemplateFile(PrintWriter writer) {
		String indent = "   "; //$NON-NLS-1$
		// open
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<form>"); //$NON-NLS-1$
		ITemplateSection[] templateSections = getTemplateSections();
		if (templateSections.length > 0) {
			// add the standard prolog
			writer
					.println(indent
							+ PDEPlugin
									.getResourceString("ManifestEditor.TemplatePage.intro")); //$NON-NLS-1$
			// add template section descriptions
			for (int i = 0; i < templateSections.length; i++) {
				ITemplateSection section = templateSections[i];
				String list = "<li style=\"text\" value=\"" + (i + 1) + ".\">"; //$NON-NLS-1$ //$NON-NLS-2$
				writer.println(indent + list + "<b>" + section.getLabel() //$NON-NLS-1$
						+ ".</b>" + section.getDescription() + "</li>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		// add the standard epilogue
		writer
				.println(indent
						+ PDEPlugin
								.getResourceString("ManifestEditor.TemplatePage.common")); //$NON-NLS-1$
		// close
		writer.println("</form>"); //$NON-NLS-1$
	}
}