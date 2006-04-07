package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.Assert;
import org.eclipse.pde.core.IBaseModel;

/**
 * ModelModification class used my the PDEModelUtility
 * Subclass me to create changes to your models.
 *
 */
public abstract class ModelModification {

	private IFile fModelFile;
	private IFile fManifestFile;
	private IFile fXMLFile;
	private boolean fIsBundleModel;
	
	/**
	 * Create a single model modification - used for modifying single AbstractEditingModels
	 * @param modelFile the basic underlying file for the model you wish to modify.
	 */
	public ModelModification(IFile modelFile) {
		singleFileModification(modelFile);
	}
	
	
	/**
	 * Create a full IBundlePluginModelBase modification
	 * @param bundleFile the MANIFEST.MF file
	 * @param xmlFile the plugin.xml/fragment.xml file for this modification (optional - can be null)
	 * @pre bundleFile must not be <code>null</code>
	 */
	public ModelModification(IFile bundleFile, IFile xmlFile) {
		createFullBundleModification(bundleFile, xmlFile);
	}
	
	/**
	 * Create a ModelModification based on the contents of the project
	 * ie. if the project contains a MANIFEST.MF this will be tagged as a 
	 * fullBundleModification, otherwise (this project is an old-style plugin)
	 * this will be a PluginModel/FragmentModel modification.
	 * @param project
	 */
	public ModelModification(IProject project) {
		IFile xml = project.getFile(PDEModelUtility.F_PLUGIN);
		if (!xml.exists())
			xml = project.getFile(PDEModelUtility.F_FRAGMENT);
		if (!xml.exists())
			xml = null;
		IFile manifest = project.getFile(PDEModelUtility.F_MANIFEST_FP);
		if (!manifest.exists() && xml != null)
			singleFileModification(xml);
		else if (manifest.exists())
			createFullBundleModification(manifest, xml);
	}
	
	private void singleFileModification(IFile file) {
		assignFile(file);
		fModelFile = getManifestFile();
		if (fModelFile == null)
			fModelFile = getXMLFile();
		fIsBundleModel = file.getName().equals(PDEModelUtility.F_MANIFEST);
	}
	
	private void createFullBundleModification(IFile bundleFile, IFile xmlFile) {
		assignFile(bundleFile);
		assignFile(xmlFile);
		
		Assert.isNotNull(fManifestFile, "Full bundle modifications require a MANIFEST.MF.");
		fModelFile = fManifestFile;
		fIsBundleModel = true;
	}
	
	private void assignFile(IFile file) {
		if (file == null)
			return;
		String name = file.getName();
		if (name.equals(PDEModelUtility.F_MANIFEST))
			fManifestFile = file;
		else if (name.equals(PDEModelUtility.F_PLUGIN) || name.equals(PDEModelUtility.F_FRAGMENT))
			fXMLFile = file;
	}
	
	/**
	 * Invoke this using PDEModelUtility.modifyModel(ModelModification modification)
	 * @param model
	 * @param monitor
	 * @throws CoreException
	 */
	protected abstract void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException;
	
	protected final IFile getFile() {
		return fModelFile;
	}
	
	protected final IFile getManifestFile() {
		return fManifestFile;
	}
	
	protected final IFile getXMLFile() {
		return fXMLFile;
	}
	
	protected final boolean isFullBundleModification() {
		return fIsBundleModel;
	}
}
