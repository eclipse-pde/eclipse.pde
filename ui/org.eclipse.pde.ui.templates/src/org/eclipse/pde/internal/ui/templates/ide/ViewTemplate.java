/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473694, 486261
 *******************************************************************************/

package org.eclipse.pde.internal.ui.templates.ide;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.templates.IHelpContextIds;
import org.eclipse.pde.internal.ui.templates.PDETemplateMessages;
import org.eclipse.pde.internal.ui.templates.PDETemplateSection;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.AbstractTemplateSection;
import org.eclipse.pde.ui.templates.BooleanOption;
import org.eclipse.pde.ui.templates.PluginReference;
import org.osgi.framework.Constants;

public class ViewTemplate extends PDETemplateSection {
	private BooleanOption addToPerspective;
	private BooleanOption contextHelp;

	/**
	 * Constructor for HelloWorldTemplate.
	 */
	public ViewTemplate() {
		setPageCount(1);
		createOptions();
	}

	@Override
	public String getSectionId() {
		return "view"; //$NON-NLS-1$
	}

	@Override
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.ViewTemplate_packageName, (String) null, 0);
		addOption("className", PDETemplateMessages.ViewTemplate_className, "SampleView", 0); //$NON-NLS-1$ //$NON-NLS-2$
		addOption("viewName", PDETemplateMessages.ViewTemplate_name, PDETemplateMessages.ViewTemplate_defaultName, 0); //$NON-NLS-1$
		addOption("viewCategoryId", PDETemplateMessages.ViewTemplate_categoryId, (String) null, 0); //$NON-NLS-1$
		addOption("viewCategoryName", PDETemplateMessages.ViewTemplate_categoryName, PDETemplateMessages.ViewTemplate_defaultCategoryName, 0); //$NON-NLS-1$
		addOption("viewType", PDETemplateMessages.ViewTemplate_select, //$NON-NLS-1$
				new String[][] { {"tableViewer", PDETemplateMessages.ViewTemplate_table}, //$NON-NLS-1$
						{"treeViewer", PDETemplateMessages.ViewTemplate_tree}}, //$NON-NLS-1$
				"tableViewer", 0); //$NON-NLS-1$
		addOption("addViewID", PDETemplateMessages.ViewTemplate_addViewID, true, 0); //$NON-NLS-1$
		addToPerspective = (BooleanOption) addOption("addToPerspective", PDETemplateMessages.ViewTemplate_addToPerspective, true, 0); //$NON-NLS-1$
		contextHelp = (BooleanOption) addOption("contextHelp", PDETemplateMessages.ViewTemplate_contextHelp, true, 0); //$NON-NLS-1$
	}

	@Override
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		initializeFields(data.getId());

	}

	@Override
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so
		// we can initialize directly from it
		initializeFields(model.getPluginBase().getId());
	}

	public void initializeFields(String id) {
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
		initializeOption("viewCategoryId", id); //$NON-NLS-1$
	}

	@Override
	public boolean isDependentOnParentWizard() {
		return true;
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page0 = createPage(0, IHelpContextIds.TEMPLATE_VIEW);
		page0.setTitle(PDETemplateMessages.ViewTemplate_title0);
		page0.setDescription(PDETemplateMessages.ViewTemplate_desc0);
		wizard.addPage(page0);

		markPagesAdded();
	}

	/**
	 * @see AbstractTemplateSection#isOkToCreateFile(File)
	 */
	@Override
	protected boolean isOkToCreateFile(File sourceFile) {
		boolean isOk = true;
		String fileName = sourceFile.getName();
		if (fileName.equals("contexts.xml")) { //$NON-NLS-1$
			isOk = contextHelp.isSelected();
		}
		return isOk;
	}

	@Override
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.views"; //$NON-NLS-1$
	}

	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {

		IBundle bundle = ((IBundlePluginModelBase) model).getBundleModel().getBundle();
		bundle.setHeader(Constants.IMPORT_PACKAGE, "javax.inject;version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$

		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.views", true); //$NON-NLS-1$
		IPluginModelFactory factory = model.getPluginFactory();

		String cid = getStringOption("viewCategoryId"); //$NON-NLS-1$

		createCategory(extension, cid);
		String fullClassName = getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption("className"); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginElement viewElement = factory.createElement(extension);
		viewElement.setName("view"); //$NON-NLS-1$
		viewElement.setAttribute("id", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("name", getStringOption("viewName")); //$NON-NLS-1$ //$NON-NLS-2$
		viewElement.setAttribute("icon", "icons/sample.png"); //$NON-NLS-1$ //$NON-NLS-2$

		viewElement.setAttribute("class", fullClassName); //$NON-NLS-1$
		viewElement.setAttribute("category", cid); //$NON-NLS-1$
		viewElement.setAttribute("inject", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		extension.add(viewElement);
		if (!extension.isInTheModel())
			plugin.add(extension);

		if (addToPerspective.isSelected()) {
			IPluginExtension perspectiveExtension = createExtension("org.eclipse.ui.perspectiveExtensions", true); //$NON-NLS-1$

			IPluginElement perspectiveElement = factory.createElement(perspectiveExtension);
			perspectiveElement.setName("perspectiveExtension"); //$NON-NLS-1$
			perspectiveElement.setAttribute("targetID", //$NON-NLS-1$
					"org.eclipse.jdt.ui.JavaPerspective"); //$NON-NLS-1$

			IPluginElement view = factory.createElement(perspectiveElement);
			view.setName("view"); //$NON-NLS-1$
			view.setAttribute("id", fullClassName); //$NON-NLS-1$
			view.setAttribute("relative", "org.eclipse.ui.views.ProblemView"); //$NON-NLS-1$ //$NON-NLS-2$
			view.setAttribute("relationship", "right"); //$NON-NLS-1$ //$NON-NLS-2$
			view.setAttribute("ratio", "0.5"); //$NON-NLS-1$ //$NON-NLS-2$
			perspectiveElement.add(view);

			perspectiveExtension.add(perspectiveElement);
			if (!perspectiveExtension.isInTheModel())
				plugin.add(perspectiveExtension);
		}

		if (contextHelp.isSelected()) {
			IPluginExtension contextExtension = createExtension("org.eclipse.help.contexts", true); //$NON-NLS-1$

			IPluginElement contextsElement = factory.createElement(contextExtension);
			contextsElement.setName("contexts"); //$NON-NLS-1$
			contextsElement.setAttribute("file", "contexts.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			contextExtension.add(contextsElement);
			if (!contextExtension.isInTheModel())
				plugin.add(contextExtension);
		}
	}

	private void createCategory(IPluginExtension extension, String id) throws CoreException {
		IPluginObject[] children = extension.getChildren();
		for (IPluginObject child : children) {
			IPluginElement element = (IPluginElement) child;
			if (element.getName().equalsIgnoreCase("category")) { //$NON-NLS-1$
				IPluginAttribute att = element.getAttribute("id"); //$NON-NLS-1$
				if (att != null) {
					String cid = att.getValue();
					if (cid != null && cid.equals(id))
						return;
				}
			}
		}
		IPluginElement categoryElement = model.getFactory().createElement(extension);
		categoryElement.setName("category"); //$NON-NLS-1$
		categoryElement.setAttribute("name", getStringOption("viewCategoryName")); //$NON-NLS-1$ //$NON-NLS-2$
		categoryElement.setAttribute("id", id); //$NON-NLS-1$
		extension.add(categoryElement);
	}

	@Override
	public String[] getNewFiles() {
		if (contextHelp.isSelected())
			return new String[] {"icons/", "contexts.xml"}; //$NON-NLS-1$ //$NON-NLS-2$
		return new String[] {"icons/"}; //$NON-NLS-1$
	}

	@Override
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList<PluginReference> result = new ArrayList<>();
		if (schemaVersion != null)
			result.add(new PluginReference("org.eclipse.core.runtime")); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui")); //$NON-NLS-1$
		return result.toArray(new IPluginReference[result.size()]);
	}

	@Override
	protected String getFormattedPackageName(String id) {
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".views"; //$NON-NLS-1$
		return "views"; //$NON-NLS-1$
	}

	@Override
	public Object getValue(String name) {
		if (name.equals("useEnablement")) //$NON-NLS-1$
			return Boolean.valueOf(getTargetVersion() >= 3.3);
		return super.getValue(name);
	}
}
