/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.templates.PluginReference;
import org.eclipse.pde.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.part.*;

public class NewProjectCreationOperation extends WorkspaceModifyOperation {
    private IFieldData fData;
    private IProjectProvider fProjectProvider;
    private WorkspacePluginModelBase fModel;
    private PluginClassCodeGenerator fGenerator;
    private IPluginContentWizard fContentWizard;
    private boolean result;
    
    public NewProjectCreationOperation(IFieldData data,
            IProjectProvider provider,
            IPluginContentWizard contentWizard) {
        fData = data;
        fProjectProvider = provider;
        fContentWizard = contentWizard;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
    	
    	// start task
		monitor.beginTask(
				PDEPlugin.getResourceString("NewProjectCreationOperation.creating"), getNumberOfWorkUnits()); //$NON-NLS-1$
		monitor.subTask(PDEPlugin
				.getResourceString("NewProjectCreationOperation.project")); //$NON-NLS-1$

		// create project
		IProject project = createProject();
		monitor.worked(1);
		
		// set classpath if project has a Java nature
		if (project.hasNature(JavaCore.NATURE_ID)) {
			monitor.subTask(PDEPlugin
							.getResourceString("NewProjectCreationOperation.setClasspath")); //$NON-NLS-1$
			setClasspath(project, fData);
			monitor.worked(1);
		}
				
		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData) fData;
			
			// generate top-level Java class if that option is selected
			if (data.doGenerateClass()) {
				generateTopLevelPluginClass(project, new SubProgressMonitor(monitor, 1));
			}
			// generate an application class if the RCP option is selected
			if (data.isRCPApplicationPlugin()) {
				generateApplicationClass(new SubProgressMonitor(monitor, 1));				
			}
		}
		// generate the manifest file
		monitor.subTask(PDEPlugin
				.getResourceString("NewProjectCreationOperation.manifestFile")); //$NON-NLS-1$
		createManifest(project);
		monitor.worked(1);
		
		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData) fData;
			
			// generate top-level Java class if that option is selected
			if (data.doGenerateClass()) {
				generateTopLevelPluginClass(project, new SubProgressMonitor(monitor, 1));
			}
			// generate an application class if the RCP option is selected
			if (data.isRCPApplicationPlugin()) {
				generateApplicationClass(new SubProgressMonitor(monitor, 1));				
			}
		}

		// generate the build.properties file
		monitor.subTask(PDEPlugin
						.getResourceString("NewProjectCreationOperation.buildPropertiesFile")); //$NON-NLS-1$
		createBuildPropertiesFile(project);
		monitor.worked(1);
		
		// generate content contributed by template wizards
		boolean contentWizardResult = true;
		if (fContentWizard != null) {
			contentWizardResult = fContentWizard.performFinish(project, fModel,
					new SubProgressMonitor(monitor, 1));
		}
				
		fModel.save();

		if (fData.hasBundleStructure()) {
			String filename = (fData instanceof IFragmentFieldData) ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
			PDEPluginConverter.convertToOSGIFormat(project, filename,
					new SubProgressMonitor(monitor, 1));
			trimModel(fModel.getPluginBase());
			fModel.save();
			openFile(project.getFile("META-INF/MANIFEST.MF")); //$NON-NLS-1$
		} else {
			openFile((IFile) fModel.getUnderlyingResource());
		}
		monitor.worked(1);
		result = contentWizardResult;
	}
    
    private void createApplicationExtension(String id, String classname) {
        IPluginBase plugin = fModel.getPluginBase();
        IPluginModelFactory factory = fModel.getPluginFactory();
        try {
            IPluginExtension extension = fModel.getFactory().createExtension();
            extension.setPoint("org.eclipse.core.runtime.applications"); //$NON-NLS-1$
            extension.setId(id);
            
            IPluginElement appElement = factory.createElement(extension);
            appElement.setName("application"); //$NON-NLS-1$
            
            IPluginElement runElement = factory.createElement(appElement);
            runElement.setName("run"); //$NON-NLS-1$
            runElement.setAttribute("class", classname); //$NON-NLS-1$
            
            appElement.add(runElement);
            extension.add(appElement);
            if (!extension.isInTheModel())
                plugin.add(extension);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
 
    private void createPerspectiveExtension() {
        IPluginBase plugin = fModel.getPluginBase();
        IPluginModelFactory factory = fModel.getPluginFactory();
        try {
            IPluginExtension extension = fModel.getFactory().createExtension();
            extension.setPoint("org.eclipse.ui.perspectives"); //$NON-NLS-1$
            
            IPluginElement appElement = factory.createElement(extension);
            appElement.setName("perspective"); //$NON-NLS-1$          
            appElement.setAttribute("name", "Sample Perspective"); //$NON-NLS-1$ //$NON-NLS-2$
            
        	String qualifiedName = ((IPluginFieldData)fData).getApplicationClassname();
            int nameloc = qualifiedName.lastIndexOf('.');
            String packageName = (nameloc == -1) ? "" : qualifiedName.substring(0, nameloc); //$NON-NLS-1$
            appElement.setAttribute("class", packageName + ".SamplePerspective"); //$NON-NLS-1$ //$NON-NLS-2$
            
            appElement.setAttribute("id", fData.getId() + ".samplePerspective"); //$NON-NLS-1$ //$NON-NLS-2$
            
            extension.add(appElement);
            if (!extension.isInTheModel())
                plugin.add(extension);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }

     private void generateApplicationClass(IProgressMonitor monitor) {
        try {
            ApplicationClassCodeGenerator generator = new ApplicationClassCodeGenerator(fProjectProvider.getProject(),(IPluginFieldData) fData);
            generator.generate(monitor);
            monitor.done();
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }     
    }
    private void trimModel(IPluginBase base) throws CoreException {
        base.setId(null);
        base.setVersion(null);
        base.setName(null);
        base.setProviderName(null);
        if (base instanceof IFragment) {
            ((IFragment) base).setPluginId(null);
            ((IFragment) base).setPluginVersion(null);
            ((IFragment) base).setRule(0);
        } else {
            ((IPlugin) base).setClassName(null);
        }
        IPluginImport[] imports = base.getImports();
        for (int i = 0; i < imports.length; i++) {
            base.remove(imports[i]);
        }
        IPluginLibrary[] libraries = base.getLibraries();
        for (int i = 0; i < libraries.length; i++) {
            base.remove(libraries[i]);
        }
    }
    
    public boolean getResult() {
        return result;
    }
    
    private void generateTopLevelPluginClass(IProject project,
            IProgressMonitor monitor) throws CoreException {
        IPluginFieldData data = (IPluginFieldData) fData;
        fGenerator = new PluginClassCodeGenerator(project, data.getClassname(),
                data);
        fGenerator.generate(monitor);
        monitor.done();
    }
    
    private int getNumberOfWorkUnits() {
        int numUnits = 4;
        if (fData.hasBundleStructure())
            numUnits++;
        if (fData instanceof IPluginFieldData) {
            IPluginFieldData data = (IPluginFieldData) fData;
            if (data.doGenerateClass())
                numUnits++;
            if (data.isRCPApplicationPlugin())
            	numUnits++;
            if (fContentWizard != null)
                numUnits++;
        }
        return numUnits;
    }
    
    private IProject createProject() throws CoreException {
        IProject project = fProjectProvider.getProject();
        if (!project.exists()) {
            CoreUtility.createProject(project, fProjectProvider
                    .getLocationPath(), null);
            project.open(null);
        }
        if (!project.hasNature(PDE.PLUGIN_NATURE))
            CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, null);
        if (!fData.isSimple() && !project.hasNature(JavaCore.NATURE_ID))
            CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
        if (!fData.isSimple()
                && fData.getSourceFolderName().trim().length() > 0) {
            IFolder folder = project.getFolder(fData.getSourceFolderName());
            if (!folder.exists())
                CoreUtility.createFolder(folder, true, true, null);
        }
        return project;
    }
    
    private void createManifest(IProject project) throws CoreException {
        if (fData instanceof IFragmentFieldData) {
            fModel = new WorkspaceFragmentModel(project.getFile("fragment.xml")); //$NON-NLS-1$
        } else {
            fModel = new WorkspacePluginModel(project.getFile("plugin.xml")); //$NON-NLS-1$
        }
        IPluginBase pluginBase = fModel.getPluginBase();
        if (!fData.isLegacy())
            pluginBase.setSchemaVersion("3.0"); //$NON-NLS-1$
        pluginBase.setId(fData.getId());
        pluginBase.setVersion(fData.getVersion());
        pluginBase.setName(fData.getName());
        pluginBase.setProviderName(fData.getProvider());
        if (pluginBase instanceof IFragment) {
            IFragment fragment = (IFragment) pluginBase;
            FragmentFieldData data = (FragmentFieldData) fData;
            fragment.setPluginId(data.getPluginId());
            fragment.setPluginVersion(data.getPluginVersion());
            fragment.setRule(data.getMatch());
        } else {
            if (((IPluginFieldData) fData).doGenerateClass())
                ((IPlugin) pluginBase).setClassName(((IPluginFieldData) fData)
                        .getClassname());
        }
        if (!fData.isSimple()) {
            IPluginLibrary library = fModel.getPluginFactory().createLibrary();
            library.setName(fData.getLibraryName());
            library.setExported(true);
            pluginBase.add(library);
        }
        IPluginReference[] dependencies = getDependencies();
        for (int i = 0; i < dependencies.length; i++) {
            IPluginReference ref = dependencies[i];
            IPluginImport iimport = fModel.getPluginFactory().createImport();
            iimport.setId(ref.getId());
            iimport.setVersion(ref.getVersion());
            iimport.setMatch(ref.getMatch());
            pluginBase.add(iimport);
        }
		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData) fData;
			
			// generate an applications section and Java class if the RCP option is selected
			if (data.isRCPApplicationPlugin()) {
				createApplicationExtension(data.getApplicationID(), data.getApplicationClassname());
				createPerspectiveExtension();
			}
		}


    }
    
    private void createBuildPropertiesFile(IProject project)
    throws CoreException {
        IFile file = project.getFile("build.properties"); //$NON-NLS-1$
        if (!file.exists()) {
            WorkspaceBuildModel model = new WorkspaceBuildModel(file);
            IBuildModelFactory factory = model.getFactory();
            IBuildEntry binEntry = factory
            .createEntry(IBuildEntry.BIN_INCLUDES);
            binEntry.addToken(fData instanceof IFragmentFieldData ? "fragment.xml" //$NON-NLS-1$
                    : "plugin.xml"); //$NON-NLS-1$
            if (fData.hasBundleStructure())
                binEntry.addToken("META-INF/"); //$NON-NLS-1$
            if (!fData.isSimple()) {
                binEntry.addToken(fData.getLibraryName());
                if (fContentWizard != null) {
                    String[] files = fContentWizard.getNewFiles();
                    for (int j = 0; j < files.length; j++) {
                        if (!binEntry.contains(files[j]))
                            binEntry.addToken(files[j]);
                    }
                }
                IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX
                        + fData.getLibraryName());
                String srcFolder = fData.getSourceFolderName().trim();
                if (srcFolder.length() > 0)
                    entry.addToken(new Path(srcFolder).addTrailingSeparator()
                            .toString());
                else
                    entry.addToken("."); //$NON-NLS-1$
                model.getBuild().add(entry);
                entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX
                        + fData.getLibraryName());
                String outputFolder = fData.getOutputFolderName().trim();
                if (outputFolder.length() > 0)
                    entry.addToken(new Path(outputFolder)
                            .addTrailingSeparator().toString());
                else
                    entry.addToken("."); //$NON-NLS-1$
                model.getBuild().add(entry);
            }
            model.getBuild().add(binEntry);
            model.save();
        }
    }
    
    private void setClasspath(IProject project, IFieldData data)
    throws JavaModelException, CoreException {
        // Set output folder
        IJavaProject javaProject = JavaCore.create(project);
        IPath path = project.getFullPath().append(data.getOutputFolderName());
        javaProject.setOutputLocation(path, null);
        // Set classpath
        IClasspathEntry[] entries = new IClasspathEntry[3];
        path = project.getFullPath().append(data.getSourceFolderName());
        entries[0] = JavaCore.newSourceEntry(path);
        entries[1] = ClasspathUtilCore.createContainerEntry();
        entries[2] = ClasspathUtilCore.createJREEntry();
        javaProject.setRawClasspath(entries, null);
    }
    
    private IPluginReference[] getDependencies() {
        ArrayList result = new ArrayList();
        if (fGenerator != null) {
            IPluginReference[] refs = fGenerator.getDependencies();
            for (int i = 0; i < refs.length; i++) {
                result.add(refs[i]);
            }
        }
        
        if (fData instanceof IPluginFieldData && ((IPluginFieldData)fData).isRCPApplicationPlugin()) {
        	IPluginReference ref = new PluginReference("org.eclipse.core.runtime", null, 0);
        	if (!result.contains(ref))
        		result.add(ref);
        	ref = new PluginReference("org.eclipse.ui", null, 0);
        	if (!result.contains(ref))
        		result.add(ref);
        }
        
        if (fContentWizard != null) {
            IPluginReference[] refs = fContentWizard.getDependencies(fData
                    .isLegacy() ? null : "3.0"); //$NON-NLS-1$
            for (int j = 0; j < refs.length; j++) {
                if (!result.contains(refs[j]))
                    result.add(refs[j]);
            }
        }
        return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
    }
    
    private void openFile(final IFile file) {
        final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
        final IWorkbenchPage page = ww.getActivePage();
        if (page == null)
            return;
        final IWorkbenchPart focusPart = page.getActivePart();
        ww.getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (focusPart instanceof ISetSelectionTarget) {
                    ISelection selection = new StructuredSelection(file);
                    ((ISetSelectionTarget) focusPart).selectReveal(selection);
                }
                try {
                    page.openEditor(new FileEditorInput(file),
                            PDEPlugin.MANIFEST_EDITOR_ID);
                } catch (PartInitException e) {
                }
            }
        });
    }
}