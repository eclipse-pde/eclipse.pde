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

import java.io.*;
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
import org.eclipse.pde.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.part.*;
import org.osgi.framework.*;

public class NewProjectCreationOperation extends WorkspaceModifyOperation {
    private IFieldData fData;
    private IProjectProvider fProjectProvider;
    private WorkspacePluginModelBase fModel;
    private PluginClassCodeGenerator fGenerator;
    private IPluginContentWizard fContentWizard;
    private RCPData fRCPData;
    private boolean result;
    
    public static final String[] defaultWindowImages = new String[]{
            "eclipse.gif", "eclipse32.gif"}; //$NON-NLS-1$ //$NON-NLS-2$
    public static final String defaultAboutImage = "eclipse_lg.gif"; //$NON-NLS-1$
    public static final String defaultSplashImage = "splash.bmp"; //$NON-NLS-1$
   
    public NewProjectCreationOperation(IFieldData data,
            IProjectProvider provider, RCPData bData,
            IPluginContentWizard contentWizard) {
        fData = data;
        fProjectProvider = provider;
        fContentWizard = contentWizard;
        fRCPData = bData;
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
		
		// generate top-level Java class if that option is selected
		if (fData instanceof IPluginFieldData && ((IPluginFieldData) fData).doGenerateClass()) {
			generateTopLevelPluginClass(project, new SubProgressMonitor(monitor, 1));
		}
		
		// generate the manifest file
		monitor.subTask(PDEPlugin
				.getResourceString("NewProjectCreationOperation.manifestFile")); //$NON-NLS-1$
		createManifest(project);
		monitor.worked(1);
		
		// generate the build.properties file
		monitor.subTask(PDEPlugin
						.getResourceString("NewProjectCreationOperation.buildPropertiesFile")); //$NON-NLS-1$
		createBuildPropertiesFile(project);
		monitor.worked(1);
		
		
		// if it's an RCP plugin, generate application and product branding if necessary
		if (fData instanceof PluginFieldData
				&& ((PluginFieldData) fData).isRCPAppPlugin()) {
			createApplicationExtension();
			createApplicationClass(new SubProgressMonitor(monitor, 1));
			if (fRCPData.getAddBranding()) {
				createProductExtension();
				if (fRCPData.useDefaultImages())
					copyBrandingImages();
				if (fRCPData.getGenerateTemplateFiles()) {
					createAboutPropertiesFile(project);
					createPluginCustomizationFile(project);
					createAboutIniFile(project);
					createPluginProperties(project);
				}
				monitor.worked(1);
			}
		}

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
    
    /**
	 * @param project
	 */
    private void createPluginCustomizationFile(IProject project) {
        IFile newFile = project.getFile("plugin_customization.ini"); //$NON-NLS-1$
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        if (newFile.exists())
            return;
        pWriter.println("# plugin_customization.ini"); //$NON-NLS-1$
        pWriter
        .println("# sets default values for plug-in-specific preferences"); //$NON-NLS-1$
        pWriter.println("# keys are qualified by plug-in id"); //$NON-NLS-1$
        pWriter.println("# e.g., com.example.acmeplugin/myproperty=myvalue"); //$NON-NLS-1$
        pWriter.println();
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("ISO-8859-1")); //$NON-NLS-1$
            newFile.create(target, true, null);
        } catch (UnsupportedEncodingException e) {
            PDEPlugin.logException(e);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
    /**
     * @param project
     */
    private void createAboutPropertiesFile(IProject project) {
        IFile newFile = project.getFile("about.properties"); //$NON-NLS-1$
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        if (newFile.exists())
            return;
        pWriter.println("# about.properties"); //$NON-NLS-1$
        pWriter.println("# contains externalized strings for about.ini"); //$NON-NLS-1$
        pWriter.println("# java.io.Properties file (ISO 8859-1 with \"\\\" escapes)"); //$NON-NLS-1$
        pWriter.println("# fill-ins are supplied by about.mappings"); //$NON-NLS-1$
        pWriter.println("# This file should be translated."); //$NON-NLS-1$
        pWriter.println("#"); //$NON-NLS-1$
        pWriter.println("# Do not translate any values surrounded by {}"); //$NON-NLS-1$
        pWriter.println(""); //$NON-NLS-1$
        pWriter.println("blurb=" + fRCPData.getProductName() + "\\n\\"); //$NON-NLS-1$ //$NON-NLS-2$
        pWriter.println("\\n\\"); //$NON-NLS-1$
        pWriter.println("Version: {featureVersion}\\n\\"); //$NON-NLS-1$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter.toString().getBytes("ISO-8859-1")); //$NON-NLS-1$
            newFile.create(target, true, null);
        } catch (UnsupportedEncodingException e) {
            PDEPlugin.logException(e);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        } 
    }
    
    private void createPluginProperties(IProject project) {
        IFile newFile = project.getFile("plugin.properties"); //$NON-NLS-1$
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        if (newFile.exists())
            return;
        pWriter.println("productBlurb= " + fRCPData.getProductName() + "\\n\\"); //$NON-NLS-1$ //$NON-NLS-2$
        pWriter.println("\\n\\"); //$NON-NLS-1$
        pWriter.println("Version: " + fData.getVersion() + "\\n\\"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("ISO-8859-1")); //$NON-NLS-1$
            newFile.create(target, true, null);
        } catch (UnsupportedEncodingException e) {
            PDEPlugin.logException(e);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
    private void createAboutIniFile(IProject project) {
        IFile newFile = project.getFile("about.ini"); //$NON-NLS-1$
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        if (newFile.exists())
            return;
        pWriter.println("# about.ini"); //$NON-NLS-1$
        pWriter.println("# contains information about a feature"); //$NON-NLS-1$
        pWriter.println("# java.io.Properties file (ISO 8859-1 with \"\\\" escapes)"); //$NON-NLS-1$
        pWriter.println("# \"%key\" are externalized strings defined in about.properties"); //$NON-NLS-1$
        pWriter.println(" # This file does not need to be translated."); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# Property \"aboutText\" contains blurb for feature details in the \"About\""); //$NON-NLS-1$
        pWriter.println("# dialog (translated).  Maximum 15 lines and 75 characters per line."); //$NON-NLS-1$
        pWriter.println("aboutText=%blurb"); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# Property \"featureImage\" contains path to feature image (32x32)"); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# Property \"windowImage\" contains path to window icon (16x16)"); //$NON-NLS-1$
        pWriter.println("# needed for primary features only"); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# Property \"aboutImage\" contains path to product image (500x330 or 115x164)"); //$NON-NLS-1$
        pWriter.println("# needed for primary features only"); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# Property \"appName\" contains name of the application (translated)"); //$NON-NLS-1$
        pWriter.println("# needed for primary features only"); //$NON-NLS-1$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("ISO-8859-1")); //$NON-NLS-1$
            newFile.create(target, true, null);
        } catch (UnsupportedEncodingException e) {
            PDEPlugin.logException(e);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
    private void copyBrandingImages() {
        //create icons/ folder if d.n.e
        IFolder iconFolder = fProjectProvider.getProject().getFolder("icons"); //$NON-NLS-1$
        try {
            if (!iconFolder.exists())
                iconFolder.create(true, true, null);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
        
        try {
            File image = null;
            FileInputStream fiStream = null;
            IFile copy = null;
            if (fRCPData.useDefaultImages()) {
                Bundle bundle = Platform.getBundle("org.eclipse.platform"); //$NON-NLS-1$
                if (bundle == null)
                    return;
                
                String[] imageNames = new String[defaultWindowImages.length + 2]; // add
                System.arraycopy(defaultWindowImages, 0,imageNames, 0, defaultWindowImages.length);
                imageNames[imageNames.length - 2] = defaultAboutImage;
                imageNames[imageNames.length - 1] = defaultSplashImage;
                
                for (int i = 0; i < imageNames.length; i++) {
                     image = new File(Platform.resolve(Platform.find(bundle, new Path(imageNames[i]))).getFile());
                    if (image.exists()) {
                        fiStream = new FileInputStream(image);
                        if (i == imageNames.length - 1)
                            copy = fProjectProvider.getProject().getFile(
                                    image.getName());
                        else
                            copy = iconFolder.getFile(image.getName());
                        if (!copy.exists())
                            copy.create(fiStream, true, new NullProgressMonitor());
                        fiStream.close();
                    }
                }
                return;
            }
        } catch (FileNotFoundException e) {
            PDEPlugin.logException(e);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        } catch (IOException e) {
            PDEPlugin.logException(e);
        }
    }
    
    /**
     * A utility method to create an extension object for the plug-in model from
     * the provided extension point id.
     * 
     * @param pointId
     *            the identifier of the target extension point
     * @param reuse
     *            if true, new extension object will be created only if an
     *            extension with the same Id does not exist.
     * @return an existing extension (if exists and <samp>reuse </samp> is
     *         <samp>true </samp>), or a new extension object otherwise.
     */
    protected IPluginExtension createExtension(String pointId, boolean reuse)
    throws CoreException {
        if (reuse) {
            IPluginExtension[] extensions = fModel.getPluginBase()
            .getExtensions();
            for (int i = 0; i < extensions.length; i++) {
                IPluginExtension extension = extensions[i];
                if (extension.getPoint().equalsIgnoreCase(pointId)) {
                    return extension;
                }
            }
        }
        IPluginExtension extension = fModel.getFactory().createExtension();
        extension.setPoint(pointId);
        return extension;
    }
    
    /**
     * @param applicationId
     */
    private void createApplicationExtension() {
        IPluginBase plugin = fModel.getPluginBase();
        IPluginModelFactory factory = fModel.getPluginFactory();
        try {
            IPluginExtension extension = createExtension(
                    "org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
            extension.setId(fRCPData.getApplicationId());
            IPluginElement appElement = factory.createElement(extension);
            appElement.setName("application"); //$NON-NLS-1$
            IPluginElement runElement = factory.createElement(appElement);
            runElement.setName("run"); //$NON-NLS-1$
            runElement.setAttribute(
                    "class", fRCPData.getApplicationClass()); //$NON-NLS-1$ //$NON-NLS-2$
            appElement.add(runElement);
            extension.add(appElement);
            if (!extension.isInTheModel())
                plugin.add(extension);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
        
    private void createProductExtension() {
        try {
            IPluginBase plugin = fModel.getPluginBase();
            IPluginModelFactory factory = fModel.getPluginFactory();
            IPluginExtension extension = createExtension(
                    "org.eclipse.core.runtime.products", true); //$NON-NLS-1$
            extension.setId("product"); //$NON-NLS-1$
            IPluginElement extElement = factory.createElement(extension);
            extElement.setName("product"); //$NON-NLS-1$
            extElement.setAttribute("name", fRCPData.getProductName()); //$NON-NLS-1$
            extElement.setAttribute(
                    "application", fData.getId() + "." + fRCPData.getApplicationId()); //$NON-NLS-1$ //$NON-NLS-2$

            // add markup for images if the images option is selected
            if (fRCPData.useDefaultImages()) {
	            // windowImages
	            IPluginElement windowImagesProperty = factory
	            .createElement(extElement);
	            windowImagesProperty.setName("property"); //$NON-NLS-1$
	            windowImagesProperty.setAttribute("name", "windowImages"); //$NON-NLS-1$ //$NON-NLS-2$
	            windowImagesProperty.setAttribute(
	                    "value", "icons/eclipse.gif,icons/eclipse32.gif"); //$NON-NLS-1$ //$NON-NLS-2$
	            extElement.add(windowImagesProperty);
	            
	            // aboutImage
	            IPluginElement aboutImageProperty = factory
	            .createElement(extElement);
	            aboutImageProperty.setName("property"); //$NON-NLS-1$
	            aboutImageProperty.setAttribute("name", "aboutImage"); //$NON-NLS-1$ //$NON-NLS-2$
	            aboutImageProperty.setAttribute(
	                    "value", "icons/" + defaultAboutImage); //$NON-NLS-1$ //$NON-NLS-2$
	            extElement.add(aboutImageProperty);
            }
            
            // generate markup for about.ini and plugin_customization.ini
            // if that option is selected
            if (fRCPData.getGenerateTemplateFiles()) {
	            // aboutText
	            IPluginElement aboutTextProperty = factory.createElement(extElement);
	            aboutTextProperty.setName("property"); //$NON-NLS-1$
	            aboutTextProperty.setAttribute("name", "aboutText"); //$NON-NLS-1$ //$NON-NLS-2$
	            aboutTextProperty.setAttribute("value", "%productBlurb"); //$NON-NLS-1$ //$NON-NLS-2$
	            extElement.add(aboutTextProperty);
	            // preferenceCustomization
	            IPluginElement prefProperty = factory.createElement(extElement);
	            prefProperty.setName("property"); //$NON-NLS-1$
	            prefProperty.setAttribute("name", "preferenceCustomization"); //$NON-NLS-1$ //$NON-NLS-2$
	            prefProperty.setAttribute("value", "plugin_customization.ini"); //$NON-NLS-1$ //$NON-NLS-2$
	            extElement.add(prefProperty);
            }

            extension.add(extElement);
            if (!extension.isInTheModel())
                plugin.add(extension);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
     private void createApplicationClass(IProgressMonitor monitor) {
        try {
            ApplicationClassCodeGenerator generator = new ApplicationClassCodeGenerator(fProjectProvider.getProject(), 
                    fRCPData.getApplicationClass(), (IPluginFieldData) fData);
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
            if (fContentWizard != null)
                numUnits++;
        }
        if (fData instanceof PluginFieldData) {
        	if (((PluginFieldData)fData).isRCPAppPlugin()) {
        		numUnits += 1;
        		if (fRCPData.getAddBranding())
        		numUnits += 1;
        	}
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
        if (fContentWizard != null) {
            IPluginReference[] refs = fContentWizard.getDependencies(fData
                    .isLegacy() ? null : "3.0"); //$NON-NLS-1$
            for (int j = 0; j < refs.length; j++) {
                if (!result.contains(refs[j]))
                    result.add(refs[j]);
            }
        }
        return (IPluginReference[]) result.toArray(new IPluginReference[result
                                                                        .size()]);
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