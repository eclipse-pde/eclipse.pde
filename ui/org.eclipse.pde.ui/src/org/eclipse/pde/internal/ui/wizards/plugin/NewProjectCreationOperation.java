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
import org.eclipse.core.boot.*;
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

public class NewProjectCreationOperation extends WorkspaceModifyOperation {
    private IFieldData fData;
    private IProjectProvider fProjectProvider;
    private WorkspacePluginModelBase fModel;
    private PluginClassCodeGenerator fGenerator;
    private IPluginContentWizard fContentWizard;
    private BrandingData fBrandingData;
    private boolean result;
    
    public NewProjectCreationOperation(IFieldData data,
            IProjectProvider provider, BrandingData bData,
            IPluginContentWizard contentWizard) {
        fData = data;
        fProjectProvider = provider;
        fContentWizard = contentWizard;
        fBrandingData = bData;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(IProgressMonitor monitor) throws CoreException,
    InvocationTargetException, InterruptedException {
        monitor
        .beginTask(
                PDEPlugin
                .getResourceString("NewProjectCreationOperation.creating"), getNumberOfWorkUnits()); //$NON-NLS-1$
        monitor.subTask(PDEPlugin
                .getResourceString("NewProjectCreationOperation.project")); //$NON-NLS-1$
        IProject project = createProject();
        monitor.worked(1);
        if (project.hasNature(JavaCore.NATURE_ID)) {
            monitor
            .subTask(PDEPlugin
                    .getResourceString("NewProjectCreationOperation.setClasspath")); //$NON-NLS-1$
            setClasspath(project, fData);
            monitor.worked(1);
        }
        if (fData instanceof IPluginFieldData
                && ((IPluginFieldData) fData).doGenerateClass()) {
            generateTopLevelPluginClass(project, new SubProgressMonitor(
                    monitor, 1));
        }
        monitor.subTask(PDEPlugin
                .getResourceString("NewProjectCreationOperation.manifestFile")); //$NON-NLS-1$
        createManifest(project);
        monitor.worked(1);
        monitor
        .subTask(PDEPlugin
                .getResourceString("NewProjectCreationOperation.buildPropertiesFile")); //$NON-NLS-1$
        createBuildPropertiesFile(project);
        monitor.worked(1);
        boolean contentWizardResult = true;
        if (fContentWizard != null) {
            contentWizardResult = fContentWizard.performFinish(project, fModel,
                    new SubProgressMonitor(monitor, 1));
        }
        if (fData instanceof PluginFieldData
                && ((PluginFieldData) fData).isBrandingPlugin()) {
            createBrandingExtension(fBrandingData, new SubProgressMonitor(monitor, 1));
            copyBrandingImages(fBrandingData);
            createAboutPropertiesFile(project);
            createPreferencesFile(project);
            createAboutIniFile(project);
            createPluginProperties(project);
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
    private void createPreferencesFile(IProject project) {
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
        pWriter
        .println("# Property \"org.eclipse.ui/defaultPerspectiveId\" controls the"); //$NON-NLS-1$
        pWriter.println("# perspective that the workbench opens initially"); //$NON-NLS-1$
        pWriter
        .println("org.eclipse.ui/defaultPerspectiveId=org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# new-style tabs by default"); //$NON-NLS-1$
        pWriter.println("org.eclipse.ui/SHOW_TRADITIONAL_STYLE_TABS=false"); //$NON-NLS-1$
        pWriter.println();
        pWriter.println("# put the perspective switcher on the top right"); //$NON-NLS-1$
        pWriter.println("org.eclipse.ui/DOCK_PERSPECTIVE_BAR=topRight"); //$NON-NLS-1$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("UTF8")); //$NON-NLS-1$
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
        pWriter
        .println("# java.io.Properties file (ISO 8859-1 with \"\\\" escapes)"); //$NON-NLS-1$
        pWriter.println("# fill-ins are supplied by about.mappings"); //$NON-NLS-1$
        pWriter.println("# This file should be translated."); //$NON-NLS-1$
        pWriter.println("#"); //$NON-NLS-1$
        pWriter.println("# Do not translate any values surrounded by {}"); //$NON-NLS-1$
        pWriter.println(""); //$NON-NLS-1$
        pWriter.println("blurb=" + fBrandingData.getProductName() + "\\n\\"); //$NON-NLS-1$ //$NON-NLS-2$
        pWriter.println("\\n\\"); //$NON-NLS-1$
        pWriter.println("Version: {featureVersion}\\n\\"); //$NON-NLS-1$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("UTF8")); //$NON-NLS-1$
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
        pWriter.println("pluginName=" + fBrandingData.getProductName()); //$NON-NLS-1$
        if (fData.getProvider() != null && fData.getProvider().length() != 0)
            pWriter.println("providerName=" + fData.getProvider()); //$NON-NLS-1$
        else {
            pWriter.println("# Insert provider name for value below"); //$NON-NLS-1$
            pWriter.println("#providerName="); //$NON-NLS-1$
        }
        pWriter.println();
        pWriter.println("productName=" + fBrandingData.getProductName()); //$NON-NLS-1$
        pWriter
        .println("productBlurb= " + fBrandingData.getProductName() + "\\n\\"); //$NON-NLS-1$ //$NON-NLS-2$
        pWriter.println("\\n\\"); //$NON-NLS-1$
        pWriter.println("Version: " + fData.getVersion() + "\\n\\"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("UTF8")); //$NON-NLS-1$
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
        pWriter
        .println("# java.io.Properties file (ISO 8859-1 with \"\\\" escapes)"); //$NON-NLS-1$
        pWriter
        .println("# \"%key\" are externalized strings defined in about.properties"); //$NON-NLS-1$
        pWriter.println(" # This file does not need to be translated."); //$NON-NLS-1$
        pWriter.println();
        pWriter
        .println("# Property \"aboutText\" contains blurb for feature details in the \"About\""); //$NON-NLS-1$
        pWriter
        .println("# dialog (translated).  Maximum 15 lines and 75 characters per line."); //$NON-NLS-1$
        pWriter.println("aboutText=%blurb"); //$NON-NLS-1$
        pWriter.println();
        pWriter
        .println("# Property \"featureImage\" contains path to feature image (32x32)"); //$NON-NLS-1$
        pWriter.println();
        pWriter
        .println("# Property \"windowImage\" contains path to window icon (16x16)"); //$NON-NLS-1$
        pWriter.println("# needed for primary features only"); //$NON-NLS-1$
        pWriter.println();
        pWriter
        .println("# Property \"aboutImage\" contains path to product image (500x330 or 115x164)"); //$NON-NLS-1$
        pWriter.println("# needed for primary features only"); //$NON-NLS-1$
        pWriter.println();
        pWriter
        .println("# Property \"appName\" contains name of the application (translated)"); //$NON-NLS-1$
        pWriter.println("# needed for primary features only"); //$NON-NLS-1$
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(sWriter
                    .toString().getBytes("UTF8")); //$NON-NLS-1$
            newFile.create(target, true, null);
        } catch (UnsupportedEncodingException e) {
            PDEPlugin.logException(e);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
    /**
     * @param brandingData
     */
    private void copyBrandingImages(BrandingData brandingData) {
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
            if (brandingData.useDefaultImages()) {
                IPluginDescriptor descriptor = (IPluginDescriptor) Platform
                .getPluginRegistry().getPluginDescriptor(
                "org.eclipse.platform"); //$NON-NLS-1$
                if (descriptor == null)
                    return;
                String[] imageNames = new String[BrandingData.defaultWindowImages.length + 2]; // add
                // 2
                // for
                // about
                // and
                // splash
                // image
                System.arraycopy(BrandingData.defaultWindowImages, 0,
                        imageNames, 0, BrandingData.defaultWindowImages.length);
                imageNames[imageNames.length - 2] = BrandingData.defaultAboutImage;
                imageNames[imageNames.length - 1] = BrandingData.defaultSplashImage;
                for (int i = 0; i < imageNames.length; i++) {
                    image = new File(BootLoader.getInstallURL().getFile()
                            + "/plugins/" //$NON-NLS-1$
                            + descriptor.toString() + File.separator
                            + imageNames[i]);
                    if (image.exists()) {
                        fiStream = new FileInputStream(image);
                        if (i == imageNames.length - 1)
                            copy = fProjectProvider.getProject().getFile(
                                    image.getName());
                        else
                            copy = iconFolder.getFile(image.getName());
                        if (!copy.exists())
                            copy.create(fiStream, true,
                                    new NullProgressMonitor());
                        fiStream.close();
                    }
                }
                return;
            }
            // copy aboutImage
            String aboutImage = fBrandingData.getAboutImage();
            if (aboutImage.length() != 0) {
                image = new Path(aboutImage).toFile();
                if (image.exists()) {
                    fiStream = new FileInputStream(image);
                    copy = iconFolder.getFile(image.getName());
                    if (!copy.exists())
                        copy.create(fiStream, true, new NullProgressMonitor());
                    fiStream.close();
                }
            }
            // copy splashImage
            String splashImage = fBrandingData.getSplashImage();
            if (splashImage.length() != 0) {
                image = new Path(splashImage).toFile();
                if (image.exists()) {
                    fiStream = new FileInputStream(image);
                    copy = fProjectProvider.getProject().getFile(
                            image.getName());
                    if (!copy.exists())
                        copy.create(fiStream, true, new NullProgressMonitor());
                    fiStream.close();
                }
            }
            // copy windowImages
            String[] images = fBrandingData.getWindowImages();
            for (int i = 0; i < images.length; i++) {
                image = new Path(images[i]).toFile();
                if (image.exists()) {
                    fiStream = new FileInputStream(image);
                    copy = iconFolder.getFile(image.getName());
                    if (!copy.exists())
                        copy.create(fiStream, true, new NullProgressMonitor());
                    fiStream.close();
                }
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
    private void createApplicationExtension(String applicationId) {
        IPluginBase plugin = fModel.getPluginBase();
        IPluginModelFactory factory = fModel.getPluginFactory();
        try {
            IPluginExtension extension = createExtension(
                    "org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
            extension.setId(fBrandingData.getApplicationId());
            IPluginElement appElement = factory.createElement(extension);
            appElement.setName("application"); //$NON-NLS-1$
            IPluginElement runElement = factory.createElement(appElement);
            runElement.setName("run"); //$NON-NLS-1$
            runElement.setAttribute(
                    "class", getApplicationClassId(fData.getId())); //$NON-NLS-1$ //$NON-NLS-2$
            appElement.add(runElement);
            extension.add(appElement);
            if (!extension.isInTheModel())
                plugin.add(extension);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
    private String getApplicationClassId(String id) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < id.length(); i++) {
            char ch = id.charAt(i);
            if (buffer.length() == 0) {
                if (Character.isJavaIdentifierStart(ch))
                    buffer.append(Character.toLowerCase(ch));
            } else {
                if (Character.isJavaIdentifierPart(ch) || ch == '.')
                    buffer.append(ch);
            }
        }
        StringTokenizer tok = new StringTokenizer(buffer.toString(), "."); //$NON-NLS-1$
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (!tok.hasMoreTokens())
                buffer.append("." + Character.toUpperCase(token.charAt(0)) //$NON-NLS-1$
                        + token.substring(1) + "Application"); //$NON-NLS-1$
        }
        return buffer.toString();
    }
    
    /**
     * @param brandingData
     */
    private void createBrandingExtension(BrandingData brandingData,
            IProgressMonitor monitor) {
        try {
            IPluginBase plugin = fModel.getPluginBase();
            IPluginModelFactory factory = fModel.getPluginFactory();
            String appId = fBrandingData.getApplicationId();
            if (!doesApplicationExist(appId) && appId.indexOf(".") == -1) { //$NON-NLS-1$
                createApplicationExtension(appId);
                fBrandingData.setApplicationId(plugin.getId() + "." + appId); //$NON-NLS-1$
                createApplicationClass(getApplicationClassId(fData.getId()),
                        monitor);
            }
            IPluginExtension extension = createExtension(
                    "org.eclipse.core.runtime.products", true); //$NON-NLS-1$
            extension.setId("product"); //$NON-NLS-1$
            IPluginElement extElement = factory.createElement(extension);
            extElement.setName("product"); //$NON-NLS-1$
            extElement.setAttribute("name", fBrandingData.getProductName()); //$NON-NLS-1$
            extElement.setAttribute(
                    "application", fBrandingData.getApplicationId()); //$NON-NLS-1$
            // windowImages
            IPluginElement windowImagesProperty = factory
            .createElement(extElement);
            windowImagesProperty.setName("property"); //$NON-NLS-1$
            windowImagesProperty.setAttribute("name", "windowImages"); //$NON-NLS-1$ //$NON-NLS-2$
            windowImagesProperty.setAttribute(
                    "value", fBrandingData.getExtWindowImages()); //$NON-NLS-1$
            extElement.add(windowImagesProperty);
            // aboutImage
            IPluginElement aboutImageProperty = factory
            .createElement(extElement);
            aboutImageProperty.setName("property"); //$NON-NLS-1$
            aboutImageProperty.setAttribute("name", "aboutImage"); //$NON-NLS-1$ //$NON-NLS-2$
            aboutImageProperty.setAttribute(
                    "value", fBrandingData.getExtAboutImage()); //$NON-NLS-1$
            extElement.add(aboutImageProperty);
            // aboutText
            IPluginElement aboutTextProperty = factory
            .createElement(extElement);
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
            /**
             * TODO: remember to adjust config for splash.bmp (i.e. have some
             * kind of handle to easily retrieve splashLocation within this
             * plug-in for config file) Also, copy to splash.bmp in with all the
             * other images
             */
            extension.add(extElement);
            if (!extension.isInTheModel())
                plugin.add(extension);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
    
    /**
     * @param applicationClassName
     */
    private void createApplicationClass(String applicationClassId, IProgressMonitor monitor) {
        try {
            ApplicationClassCodeGenerator generator = new ApplicationClassCodeGenerator(fProjectProvider.getProject(), 
                    getApplicationClassId(fData.getId()), (IPluginFieldData) fData);
            generator.generate(monitor);
            monitor.done();
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
        
    }
    /**
     * @param applicationId
     * @return
     */
    private boolean doesApplicationExist(String applicationId) {
        IPluginModelBase[] plugins = PDECore.getDefault().getModelManager()
        .getPlugins();
        for (int i = 0; i < plugins.length; i++) {
            IPluginExtension[] extensions = plugins[i].getPluginBase()
            .getExtensions();
            for (int j = 0; j < extensions.length; j++) {
                String point = extensions[j].getPoint();
                if (point != null
                        && point
                        .equals("org.eclipse.core.runtime.applications")) { //$NON-NLS-1$
                    String id = extensions[j].getPluginBase().getId();
                    if (id == null || id.trim().length() == 0
                            || id.startsWith("org.eclipse.pde.junit.runtime")) //$NON-NLS-1$
                        continue;
                    if (extensions[j].getId() != null)
                        if (applicationId.equals(id
                                + "." + extensions[j].getId())) //$NON-NLS-1$
                            return true;
                }
            }
        }
        return false;
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
            binEntry
            .addToken(fData instanceof IFragmentFieldData ? "fragment.xml" //$NON-NLS-1$
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