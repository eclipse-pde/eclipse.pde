/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 252329
 *     David Green <dgreen99@gmail.com> - bug 275240
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionNode;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionPointNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class GetNonExternalizedStringsOperation implements IRunnableWithProgress {

	private final ISelection fSelection;
	private ArrayList<Object> fSelectedModels;
	private ModelChangeTable fModelChangeTable;
	private boolean fCanceled;

	//Azure: To indicate that only selected plug-ins under <code>fSelection</code> are to be externalized.
	private final boolean fExternalizeSelectedPluginsOnly;

	public GetNonExternalizedStringsOperation(ISelection selection, boolean externalizeSelectedPluginsOnly) {
		fSelection = selection;
		fExternalizeSelectedPluginsOnly = externalizeSelectedPluginsOnly;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			fSelectedModels = new ArrayList<>(elems.length);
			for (int i = 0; i < elems.length; i++) {
				if (elems[i] instanceof IFile)
					elems[i] = ((IFile) elems[i]).getProject();

				if (elems[i] instanceof IProject && WorkspaceModelManager.isPluginProject((IProject) elems[i]) && !WorkspaceModelManager.isBinaryProject((IProject) elems[i]))
					fSelectedModels.add(elems[i]);
			}

			fModelChangeTable = new ModelChangeTable();

			/*
			 * Azure: This will add only the preselected plug-ins to the ModelChangeTable
			 * instead of adding the list of all plug-ins in the workspace. This is useful
			 * when the Internationalize action is run on a set of non-externalized plug-ins
			 * where there is no need to display all non-externalized plug-ins in the
			 * workspace, but only those selected.
			 */
			if (fExternalizeSelectedPluginsOnly) {
				SubMonitor subMonitor = SubMonitor.convert(monitor,
						PDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, fSelectedModels.size());
				Iterator<Object> iterator = fSelectedModels.iterator();
				while (iterator.hasNext() && !fCanceled) {
					IProject project = (IProject) iterator.next();
					if (!WorkspaceModelManager.isBinaryProject(project))
						getUnExternalizedStrings(project, subMonitor.split(1));
				}
			} else {
				IPluginModelBase[] pluginModels = PluginRegistry.getWorkspaceModels();
				SubMonitor subMonitor = SubMonitor.convert(monitor,
						PDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, pluginModels.length);
				for (int i = 0; i < pluginModels.length && !fCanceled; i++) {
					IProject project = pluginModels[i].getUnderlyingResource().getProject();
					if (!WorkspaceModelManager.isBinaryProject(project))
						getUnExternalizedStrings(project, subMonitor.split(1));
				}
			}
		}
	}

	private void getUnExternalizedStrings(IProject project, IProgressMonitor monitor) {
		PDEModelUtility.modifyModel(new ModelModification(project) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase)
					inspectManifest((IBundlePluginModelBase) model, monitor);

				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}

				if (model instanceof IPluginModelBase)
					inspectXML((IPluginModelBase) model, monitor);

				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}
			}
		}, monitor);
	}

	/**
	 * @param model
	 * @param monitor
	 * @throws CoreException
	 */
	private void inspectManifest(IBundlePluginModelBase model, IProgressMonitor monitor) throws CoreException {
		IFile manifestFile = (IFile) model.getBundleModel().getUnderlyingResource();
		IBundle bundle = model.getBundleModel().getBundle();
		for (String translatableHeader : ICoreConstants.TRANSLATABLE_HEADERS) {
			IManifestHeader header = bundle.getManifestHeader(translatableHeader);
			if (header != null && isNotTranslated(header.getValue()))
				fModelChangeTable.addToChangeTable(model, manifestFile, header, selected(manifestFile));
		}
	}

	/**
	 * @param model
	 * @param monitor
	 * @throws CoreException
	 */
	private void inspectXML(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		IFile file;
		if (model instanceof IBundlePluginModelBase) {
			ISharedExtensionsModel extModel = ((IBundlePluginModelBase) model).getExtensionsModel();
			if (extModel == null)
				return;
			file = (IFile) extModel.getUnderlyingResource();
		} else
			file = (IFile) model.getUnderlyingResource();

		IPluginBase base = model.getPluginBase();
		if (base instanceof IDocumentElementNode) {
			// old style xml plugin
			// check xml name declaration
			IDocumentAttributeNode attr = ((IDocumentElementNode) base).getDocumentAttribute(IPluginObject.P_NAME);
			if (attr != null && isNotTranslated(attr.getAttributeValue()))
				fModelChangeTable.addToChangeTable(model, file, attr, selected(file));

			// check xml provider declaration
			attr = ((IDocumentElementNode) base).getDocumentAttribute(IPluginBase.P_PROVIDER);
			if (attr != null && isNotTranslated(attr.getAttributeValue()))
				fModelChangeTable.addToChangeTable(model, file, attr, selected(file));
		}

		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (IPluginExtension extension : extensions) {
			ISchema schema = registry.getSchema(extension.getPoint());
			if (schema != null)
				inspectExtension(schema, extension, model, file);
		}

		IPluginExtensionPoint[] extensionPoints = model.getPluginBase().getExtensionPoints();
		for (IPluginExtensionPoint extensionPoint : extensionPoints) {
			inspectExtensionPoint(extensionPoint, model, file);
		}
	}

	private void inspectExtension(ISchema schema, IPluginParent parent, IPluginModelBase memModel, IFile file) {
		if (parent instanceof PluginExtensionNode parentNode) {
			IDocumentAttributeNode[] attributes = parentNode.getNodeAttributes();
			ISchemaElement schemaElement = schema.findElement(parentNode.getXMLTagName());
			if (schemaElement != null) {
				for (IDocumentAttributeNode attribute : attributes) {
					IPluginAttribute attr = (IPluginAttribute) attribute;
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable())
						if (isNotTranslated(attr.getValue()))
							fModelChangeTable.addToChangeTable(memModel, file, attr, selected(file));
				}
			}
		}

		IPluginObject[] children = parent.getChildren();
		for (IPluginObject element : children) {
			IPluginElement child = (IPluginElement) element;
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				if (schemaElement.hasTranslatableContent())
					if (isNotTranslated(child.getText()))
						fModelChangeTable.addToChangeTable(memModel, file, child, selected(file));

				IPluginAttribute[] attributes = child.getAttributes();
				for (IPluginAttribute attr : attributes) {
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable())
						if (isNotTranslated(attr.getValue()))
							fModelChangeTable.addToChangeTable(memModel, file, attr, selected(file));
				}
			}
			inspectExtension(schema, child, memModel, file);
		}
	}

	private void inspectExtensionPoint(IPluginExtensionPoint extensionPoint, IPluginModelBase memModel, IFile file) {
		if (extensionPoint instanceof PluginExtensionPointNode)
			if (isNotTranslated(extensionPoint.getName()))
				fModelChangeTable.addToChangeTable(memModel, file, ((PluginExtensionPointNode) extensionPoint).getNodeAttributesMap().get(IPluginObject.P_NAME), selected(file));
	}

	private boolean isNotTranslated(String value) {
		if (value == null)
			return false;
		if (value.length() > 0 && value.charAt(0) == '%')
			return false;
		return true;
	}

	protected ModelChangeTable getChangeTable() {
		return fModelChangeTable;
	}

	public boolean wasCanceled() {
		return fCanceled;
	}

	private boolean selected(IFile file) {
		return fSelectedModels.contains(file.getProject());
	}
}
