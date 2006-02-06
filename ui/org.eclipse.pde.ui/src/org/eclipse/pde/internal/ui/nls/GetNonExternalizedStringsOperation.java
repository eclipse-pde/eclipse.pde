package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.text.edits.MalformedTreeException;

public class GetNonExternalizedStringsOperation 
		implements IRunnableWithProgress {

	public static final String MANIFEST_LOCATION = 
		"META-INF/MANIFEST.MF"; //$NON-NLS-1$
	private static final String[] PLUGIN_XML_FILES = 
		new String[] {"plugin.xml", "fragment.xml"}; //$NON-NLS-1$ //$NON-NLS-2$
	
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
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < elems.length; i++) {
				IProject project = null;
				if (elems[i] instanceof IFile) {
					IFile file = (IFile) elems[i];
					project = file.getProject();
				} else if (elems[i] instanceof IProject) {
					project = (IProject) elems[i];
				}
				if (project != null
						&& !WorkspaceModelManager.isBinaryPluginProject(project)) {
					IPluginModelBase model = manager.findModel(project);
					if (model != null) {
						fSelectedModels.add(model);
					}
				}
			}
		
			fModelChangeTable = new ModelChangeTable();
			
			IPluginModelBase[] pluginModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
			monitor.beginTask(PDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, pluginModels.length);
			for (int i = 0; i < pluginModels.length; i++) {
				IProject project = pluginModels[i].getUnderlyingResource().getProject();
				if (!WorkspaceModelManager.isBinaryPluginProject(project) && !fCanceled) {
					getUnExternalizedStrings(project, new SubProgressMonitor(monitor, 1) , pluginModels[i]);
				}
			}
			
		}
	}
	
	private void getUnExternalizedStrings(IProject project, IProgressMonitor monitor, IModel model) {
		// check manifest
		if (model instanceof IBundlePluginModelBase) {
			try {
				inspectManifest(project, (IBundlePluginModelBase)model, monitor);
			} catch (CoreException e) {}
		}
		if (model instanceof IPluginModelBase) {
			String[] xmlFiles = PLUGIN_XML_FILES;
			for (int i = 0; i < xmlFiles.length && !fCanceled; i++) {
				IResource member = project.findMember(xmlFiles[i]);
				try {
					inspectXML(project, member, (IPluginModelBase)model, monitor);
				} catch (CoreException e) {}
			}
		}
		monitor.done();
	}
	
	private void inspectManifest(IProject project, IBundlePluginModelBase model, IProgressMonitor monitor) throws CoreException {
		try {
			if (!ModelChange.modelLoaded(model)) return;
			IFile manifestFile = null;
			for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
				if (isNotTranslated(model.getBundleModel().getBundle().getHeader(ICoreConstants.TRANSLATABLE_HEADERS[i]))) {
					if (manifestFile == null)
						manifestFile = getManifestFile(project);
					if (manifestFile != null) {
						IManifestHeader header = getHeader(project, manifestFile, ICoreConstants.TRANSLATABLE_HEADERS[i], monitor);
						if (header != null)
							fModelChangeTable.addToChangeTable(model, manifestFile, header, fSelectedModels.contains(model));
					}
				}
			}
		} catch (MalformedTreeException e) {
		}
	}
	
	private IFile getManifestFile(IProject project) {
		IResource member = project.findMember(MANIFEST_LOCATION);
		if ((member instanceof IFile))
			return (IFile)member;
		return null;
	}
	
	private IManifestHeader getHeader(IProject project, IFile file, String headerName, IProgressMonitor monitor) throws CoreException {
		IManifestHeader header = null;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			IDocument document = manager.getTextFileBuffer(file.getFullPath()).getDocument();		
			BundleModel model = new BundleModel(document, false);
			Bundle bundle = null;
			if (ModelChange.modelLoaded(model)) bundle = (Bundle)model.getBundle();
			if (bundle != null) {
				header = bundle.getManifestHeader(headerName);
			}
		} catch (MalformedTreeException e) {
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}
		return header;
	}
	
	
	
	private void inspectXML(IProject project, IResource resource, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		if (resource == null) return;
		if (!(resource instanceof IFile)) return;
		IFile file = (IFile)resource;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
			IDocument document = buffer.getDocument();
			PluginModelBase loadModel;
			if ("fragment.xml".equals(file.getName())) //$NON-NLS-1$
				loadModel = new FragmentModel(document, false);
			else
				loadModel = new PluginModel(document, false);

			if (!ModelChange.modelLoaded(loadModel)) return;			
			IPluginExtensionPoint[] points = loadModel.getPluginBase().getExtensionPoints();
			for (int i = 0; i < points.length; i++) {
				if (isNotTranslated(points[i].getName()))
					fModelChangeTable.addToChangeTable(model, file, points[i], fSelectedModels.contains(model));
			}
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			IPluginExtension[] extensions = loadModel.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}
				ISchema schema = registry.getSchema(extensions[i].getPoint());
				if (schema != null)
					inspectExtension(schema, extensions[i], loadModel, model, file);
			}
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}
	}
	
	
	private void inspectExtension(ISchema schema, IPluginParent parent, PluginModelBase model, IPluginModelBase memModel, IFile file) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				if (schemaElement.hasTranslatableContent())
					if (isNotTranslated(child.getText()))
						fModelChangeTable.addToChangeTable(memModel, file, child, fSelectedModels.contains(memModel));
				
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable()) 
						if (isNotTranslated(attr.getValue()))
							fModelChangeTable.addToChangeTable(memModel, file, attr, fSelectedModels.contains(memModel));	
					
				}
			}
			inspectExtension(schema, child, model, memModel, file);
		}
	}
	
	private boolean isNotTranslated(String value) {
		if (value != null && value.length() > 0)
			return (value == null || value.length() == 0 || value.charAt(0) != '%' ||
				(value.charAt(0) == '%' && value.length() == 1));
		return false;
	}

	protected ModelChangeTable getChangeTable() {
		return fModelChangeTable;
	}
	protected boolean wasCanceled() {
		return fCanceled;
	}
}

