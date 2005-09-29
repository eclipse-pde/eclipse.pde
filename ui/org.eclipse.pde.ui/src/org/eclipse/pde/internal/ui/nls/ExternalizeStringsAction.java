package org.eclipse.pde.internal.ui.nls;

import java.util.ArrayList;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.osgi.framework.Constants;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class ExternalizeStringsAction 
		implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	private ArrayList fSelectedModels;
	private ModelChangeTable fToBeExternalized;
	
	/**
	 * The constructor.
	 */
	public ExternalizeStringsAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		
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
						&& !WorkspaceModelManager.isBinaryPluginProject(project)
						&& !WorkspaceModelManager.isBinaryFeatureProject(project)) {
					IPluginModelBase model = manager.findModel(project);
					if (model != null) {
						fSelectedModels.add(model);
					}
				}
			}
		
			fToBeExternalized = new ModelChangeTable();
			
			NullProgressMonitor monitor = new NullProgressMonitor();
			IPluginModelBase[] pluginModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
			for (int i = 0; i < pluginModels.length; i++) {
				IProject project = pluginModels[i].getUnderlyingResource().getProject();
				if (!WorkspaceModelManager.isBinaryPluginProject(project)) {
					getUnExternalizedStrings(project, monitor, pluginModels[i]);
				}
			}
			/*
			IFeatureModel[] featureModels = PDECore.getDefault().getFeatureModelManager().getModels();
			for (int i = 0; i < featureModels.length; i++) {
				IResource resource = featureModels[i].getUnderlyingResource();
				if (resource == null) continue;
				IProject project = resource.getProject();
				if (!WorkspaceModelManager.isBinaryFeatureProject(project)) {
					if (modelLoaded(featureModels[i]))
						getUnExternalizedStrings(project, monitor);
				}
			}
			*/
			
			ExternalizeStringsWizard wizard = new ExternalizeStringsWizard(fToBeExternalized);
			final WizardDialog dialog = new WizardDialog(PDEPlugin
					.getActiveWorkbenchShell(), wizard);
			BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell()
					.getDisplay(), new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
	}
	
	private void getUnExternalizedStrings(IProject project, IProgressMonitor monitor, IPluginModelBase model) {
		// check manifest
		if (model instanceof IBundlePluginModelBase) {
			try {
				inspectManifest(project, (IBundlePluginModelBase)model, monitor);
			} catch (CoreException e) {}
		}
		// check remaining xml files
		String[] xmlFiles = new String[] {"plugin.xml","fragment.xml"};
		for (int i = 0; i < xmlFiles.length; i++) {
			IResource member = project.findMember(xmlFiles[i]);
			try {
				inspectXML(project, member, model, monitor);
			} catch (CoreException e) {}
		}
	}
	
	private void inspectManifest(IProject project, IBundlePluginModelBase model, IProgressMonitor monitor) throws CoreException {
		try {
			if (!ModelChange.modelLoaded(model)) return;
			IFile manifestFile = null;
			if (isNotTranslated(model.getBundleModel().getBundle().getHeader(Constants.BUNDLE_NAME))) {
				manifestFile = getManifestFile(project, monitor);
				if (manifestFile != null) {
					ManifestHeader header = inspectHeader(project, manifestFile, Constants.BUNDLE_NAME, monitor);
					if (header != null)
						fToBeExternalized.addToChangeTable(model, manifestFile, header, fSelectedModels.contains(model));
				}
			}
			if (isNotTranslated(model.getBundleModel().getBundle().getHeader(Constants.BUNDLE_VENDOR))) {
				if (manifestFile == null) manifestFile = getManifestFile(project, monitor);
				if (manifestFile != null) {
					ManifestHeader header = inspectHeader(project, manifestFile, Constants.BUNDLE_VENDOR, monitor);
					if (header != null)
						fToBeExternalized.addToChangeTable(model, manifestFile, header, fSelectedModels.contains(model));
				}
			}
		} catch (MalformedTreeException e) {
		}
	}
	
	private IFile getManifestFile(IProject project, IProgressMonitor monitor) {
		IResource member = project.findMember("META-INF/MANIFEST.MF");
		if ((member instanceof IFile))
			return (IFile)member;
		return null;
	}
	
	private ManifestHeader inspectHeader(IProject project, IFile file, String headerName, IProgressMonitor monitor) throws CoreException {
		ManifestHeader header = null;
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
					fToBeExternalized.addToChangeTable(model, file, points[i], fSelectedModels.contains(model));
			}
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			IPluginExtension[] extensions = loadModel.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
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
						fToBeExternalized.addToChangeTable(memModel, file, child, fSelectedModels.contains(memModel));
				
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable()) 
						if (isNotTranslated(attr.getValue()))
							fToBeExternalized.addToChangeTable(memModel, file, attr, fSelectedModels.contains(memModel));	
					
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
}
