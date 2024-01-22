/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.bnd.ui.wizards.ProjectTemplateParam;
import org.eclipse.pde.bnd.ui.wizards.TemplateParamsWizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;

import aQute.bnd.build.model.EE;

public class TemplatePluginContentWizard extends Wizard implements IPluginContentWizard {

	private static final String DEFAULT_BND_INSTRUCTION = "bnd.bnd"; //$NON-NLS-1$
	private Template template;
	private IFieldData data;
	private TemplateParamsWizardPage paramsWizardPage;

	public TemplatePluginContentWizard(Template template) {
		this.template = template;
	}

	@Override
	public void init(IFieldData data) {
		this.data = data;
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		return new IPluginReference[0];
	}

	@Override
	public String[] getNewFiles() {
		return new String[0];
	}

	@Override
	public boolean performFinish(IProject project, IPluginModelBase model, IProgressMonitor monitor) {

		SubMonitor progress = SubMonitor.convert(monitor, 100);
		Map<String, List<Object>> params = new HashMap<>();
		params.put(ProjectTemplateParam.PROJECT_NAME.getString(), List.of(data.getName()));
		params.put(ProjectTemplateParam.BASE_PACKAGE_NAME.getString(), List.of(data.getId()));
		String packageDir = data.getId().replace('.', '/');
		params.put(ProjectTemplateParam.BASE_PACKAGE_DIR.getString(), List.of(packageDir));
		params.put(ProjectTemplateParam.VERSION.getString(), List.of(data.getVersion()));
		params.put(ProjectTemplateParam.SRC_DIR.getString(), List.of(data.getSourceFolderName()));
		params.put(ProjectTemplateParam.BIN_DIR.getString(), List.of(data.getOutputFolderName()));
		if (data instanceof AbstractFieldData) {
			String ee = ((AbstractFieldData) data).getExecutionEnvironment();
			if (ee != null) {
				EE parsedEE = EE.parse(ee);
				params.put(ProjectTemplateParam.JAVA_LEVEL.getString(), List.of(parsedEE.getRelease()));
			}
		}
		getTemplatePage().getValues().forEach((key, override) -> params.put(key, List.of(override)));
		try {
			ResourceMap resourceMap = template.generateOutputs(params, progress.split(50));
			progress.setWorkRemaining(resourceMap.size());
			for (Entry<String, Resource> outputEntry : resourceMap.entries()) {
				String path = outputEntry.getKey();
				Resource resource = outputEntry.getValue();
				if (path.startsWith("/")) { //$NON-NLS-1$
					path = path.substring(1);
				}
				switch (resource.getType()) {
					case Folder:
						if (!path.isEmpty()) {
							IFolder folder = project.getFolder(path);
							mkdirs(folder, progress.split(1));
						}
						break;
					case File:
						if (DEFAULT_BND_INSTRUCTION.equals(path)) {
							// the template bnd.bnd file will replace the
							// content of pde.bnd
							makeFile(project.getFile(BndProject.INSTRUCTIONS_FILE), resource, progress.split(1));
						} else {
							makeFile(project.getFile(path), resource, progress.split(1));
						}
						break;
					default:
						// ignore
				}
			}
		} catch (Exception e) {
			ILog.get().error("Generating template failed!", e); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	private static void makeFile(IFile file, Resource resource, IProgressMonitor monitor)
			throws CoreException, IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
		mkdirs(file.getParent(), subMonitor.split(1));
		try (InputStream in = resource.getContent()) {
			if (file.exists()) {
				file.setContents(in, true, true, subMonitor.split(1));
			} else {
				file.create(in, true, subMonitor.split(1));
			}
			file.setCharset(resource.getTextEncoding(), null);
		}
	}

	private static void mkdirs(IContainer container, IProgressMonitor monitor) throws CoreException {
		if (container instanceof IFolder folder) {
			mkdirs(folder.getParent(), monitor);
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
		}
	}

	@Override
	public boolean performFinish() {
		return true; // As per IPluginContentWizard API...
	}

	@Override
	public void addPages() {
		addPage(getTemplatePage());
	}

	private TemplateParamsWizardPage getTemplatePage() {
		if (paramsWizardPage == null) {
			paramsWizardPage = new TemplateParamsWizardPage(ProjectTemplateParam.valueStrings());
			paramsWizardPage.setTemplate(template);
		}
		return paramsWizardPage;
	}

}
