/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
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
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class GetNonExternalizedStringsOperation implements IRunnableWithProgress {

	private ISelection fSelection;
	private ArrayList fSelectedModels;
	private ModelChangeTable fModelChangeTable;
	private boolean fCanceled;
	
	public GetNonExternalizedStringsOperation(ISelection selection) {
		fSelection = selection;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		
		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			fSelectedModels = new ArrayList(elems.length);
			for (int i = 0; i < elems.length; i++) {
				if (elems[i] instanceof IFile)
					elems[i] = ((IFile) elems[i]).getProject();
				
				if (elems[i] instanceof IProject 
						&& WorkspaceModelManager.isPluginProject((IProject)elems[i])
						&& !WorkspaceModelManager.isBinaryProject((IProject)elems[i]))
					fSelectedModels.add(elems[i]);
			}
		
			fModelChangeTable = new ModelChangeTable();
			
			IPluginModelBase[] pluginModels = PluginRegistry.getWorkspaceModels();
			monitor.beginTask(PDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, pluginModels.length);
			for (int i = 0; i < pluginModels.length && !fCanceled; i++) {
				IProject project = pluginModels[i].getUnderlyingResource().getProject();
				if (!WorkspaceModelManager.isBinaryProject(project))
					getUnExternalizedStrings(project, new SubProgressMonitor(monitor, 1));
			}
		}
	}
	
	private void getUnExternalizedStrings(IProject project, IProgressMonitor monitor) {
		PDEModelUtility.modifyModel(new ModelModification(project) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase)
					inspectManifest((IBundlePluginModelBase)model, monitor);
				
				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}
				
				if (model instanceof IPluginModelBase)
					inspectXML((IPluginModelBase)model, monitor);
				
				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}
			}
		}, monitor);
		monitor.done();
	}
	
	private void inspectManifest(IBundlePluginModelBase model, IProgressMonitor monitor) throws CoreException {
		IFile manifestFile = (IFile)model.getBundleModel().getUnderlyingResource();
		IBundle bundle = model.getBundleModel().getBundle();
		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			IManifestHeader header = bundle.getManifestHeader(ICoreConstants.TRANSLATABLE_HEADERS[i]);
			if (header != null && isNotTranslated(header.getValue()))
				fModelChangeTable.addToChangeTable(model, manifestFile, header, selected(manifestFile));
		}
	}
	
	private void inspectXML(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		IFile file;
		if (model instanceof IBundlePluginModelBase) {
			ISharedExtensionsModel extModel = ((IBundlePluginModelBase)model).getExtensionsModel();
			if (extModel == null)
				return;
			file = (IFile)extModel.getUnderlyingResource();
		} else
			file = (IFile)model.getUnderlyingResource();
		
		IPluginBase base = model.getPluginBase();
		if (base instanceof IDocumentElementNode) {
			// old style xml plugin
			// check xml name declaration
			IDocumentAttributeNode attr = ((IDocumentElementNode)base).getDocumentAttribute(IPluginObject.P_NAME);
			if (attr != null && isNotTranslated(attr.getAttributeValue()))
				fModelChangeTable.addToChangeTable(model, file, attr, selected(file));
			
			// check xml provider declaration
			attr = ((IDocumentElementNode)base).getDocumentAttribute(IPluginBase.P_PROVIDER);
			if (attr != null && isNotTranslated(attr.getAttributeValue()))
				fModelChangeTable.addToChangeTable(model, file, attr, selected(file));
		}
		
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			ISchema schema = registry.getSchema(extensions[i].getPoint());
			if (schema != null)
				inspectExtension(schema, extensions[i], model, file);
		}
	}
	
	
	private void inspectExtension(ISchema schema, IPluginParent parent, IPluginModelBase memModel, IFile file) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				if (schemaElement.hasTranslatableContent())
					if (isNotTranslated(child.getText()))
						fModelChangeTable.addToChangeTable(memModel, file, child, selected(file));
				
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable()) 
						if (isNotTranslated(attr.getValue()))
							fModelChangeTable.addToChangeTable(memModel, file, attr, selected(file));	
				}
			}
			inspectExtension(schema, child, memModel, file);
		}
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

