/*******************************************************************************
 * Copyright (c) 2015 OPCoach
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #441331,  466269, 466680)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.e4;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.internal.ui.wizards.plugin.PluginFieldData;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;

public class E4ApplicationNewWizard extends NewPluginTemplateWizard {

	private static final String ORG_ECLIPSE_CORE_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_UI = "org.eclipse.ui"; //$NON-NLS-1$
	private static final String MODEL_EDITOR_ID = "org.eclipse.e4.tools.emf.editor3x.e4wbm"; //$NON-NLS-1$
	private E4ApplicationTemplate appTemplate;

	@Override
	public void init(IFieldData data) {
		super.init(data);
		setWindowTitle(PDETemplateMessages.E4ApplicationNewWizard_title);
		setE4Plugin(true); // Fix 466680
	}

	@Override
	public ITemplateSection[] createTemplateSections() {
		appTemplate = new E4ApplicationTemplate();
		return new ITemplateSection[] {appTemplate};
	}

	@Override
	public boolean performFinish(IProject project, IPluginModelBase model, IProgressMonitor monitor) {
		// Must do like ancestor 
		boolean result = super.performFinish(project, model, monitor);

		// but must then remove the "org.eclipse.ui" dependency which has been generated !
		// and core.runtime without any version (must remove it and readd it with good version) !
		IPluginBase pb = model.getPluginBase();
		IPluginImport ui = null, runtime = null;
		String runtimeVersion = null;

		for (IPluginImport ii : pb.getImports()) {
			if (ii.getId().equals(ORG_ECLIPSE_UI))
				ui = ii;
			if (ii.getId().equals(ORG_ECLIPSE_CORE_RUNTIME)) {
				// This plugin appears twice : with and without version (due to ancestor)
				if (ii.getVersion() == null)
					runtime = ii;
				else
					runtimeVersion = ii.getVersion();
			}

		}

		// Remove these two bad imports...
		try {
			if (ui != null)
				pb.remove(ui);
			if (runtime != null) {
				// Remove the org.eclipse.core.runtime without any version
				pb.remove(runtime);

				// And must re-add it with correct version 
				PluginReference pr = new PluginReference(ORG_ECLIPSE_CORE_RUNTIME, runtimeVersion, IMatchRules.GREATER_OR_EQUAL);
				IPluginImport iimport = model.getPluginFactory().createImport();
				iimport.setId(pr.getId());
				iimport.setVersion(pr.getVersion());
				iimport.setMatch(pr.getMatch());
				pb.add(iimport);

			}

		} catch (CoreException e) {
			// Not a so big problem if remove failed...
		}

		try {
			openEditorForApplicationModel(project);
		} catch (PartInitException e) {
			// Not a so big problem if editor does not open (may be e4 tools not present)...
		}

		return result;
	}


	/**
	 * Opens the model editor after the project was created.
	 *
	 * @throws PartInitException
	 */
	private void openEditorForApplicationModel(IProject project) throws PartInitException {
		final IFile file = project.getFile(E4ApplicationTemplate.E4_MODEL_FILE);
		if (file != null) {
			final FileEditorInput input = new FileEditorInput(file);
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			final IWorkbenchPage page = window.getActivePage();
			if (page != null)
				page.openEditor(input, MODEL_EDITOR_ID);
		}
	}

	/** The template must generate an E4 compliant plugin */ 
	private void setE4Plugin(boolean e4Mode) {
		IFieldData data = getData();
		if (data instanceof PluginFieldData) {
			PluginFieldData pfd = (PluginFieldData) data;
			pfd.setE4Plugin(e4Mode);
		}
	}
}
