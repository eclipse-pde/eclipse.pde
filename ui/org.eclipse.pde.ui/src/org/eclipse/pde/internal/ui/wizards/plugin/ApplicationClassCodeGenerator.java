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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.ui.*;

public class ApplicationClassCodeGenerator {
    private IPluginFieldData fPluginData;
    private IProject fProject;

    public ApplicationClassCodeGenerator(IProject project, IPluginFieldData data) {
        this.fProject = project;
        fPluginData = data;
    }

    public IFile generate(IProgressMonitor monitor) throws CoreException {
    	String qualifiedName = fPluginData.getApplicationClassname();
        int nameloc = qualifiedName.lastIndexOf('.');
        String packageName = (nameloc == -1) ? "" : qualifiedName.substring(0, nameloc); //$NON-NLS-1$
        String className = qualifiedName.substring(nameloc + 1);
        IPath path = new Path(packageName.replace('.', '/'));
        if (fPluginData.getSourceFolderName().trim().length() > 0)
            path = new Path(fPluginData.getSourceFolderName()).append(path);
        CoreUtility.createFolder(fProject.getFolder(path), true, true, null);
        
        IFile file = fProject.getFile(path.append(className + ".java")); //$NON-NLS-1$
        StringWriter swriter = new StringWriter();
        PrintWriter writer = new PrintWriter(swriter);
        generateApplicationClass(packageName, className, writer);
        
        writer.flush();
        try {
            swriter.close();
            ByteArrayInputStream stream = new ByteArrayInputStream(swriter
                    .toString().getBytes(fProject.getDefaultCharset()));
            if (file.exists())
                file.setContents(stream, false, true, monitor);
            else
                file.create(stream, false, monitor);
            stream.close();
        } catch (IOException e) {
        }
        
        generateWorkbenchAdvisor(path, packageName);
        generatePerspectiveClass(path, packageName);
        return file;
    }
    
    private void generateWorkbenchAdvisor(IPath path, String packageName) throws CoreException {
    	IFile file = fProject.getFile(path.append("SampleWorkbenchAdvisor.java")); //$NON-NLS-1$
        StringWriter swriter = new StringWriter();
        PrintWriter writer = new PrintWriter(swriter);
    	
    	if (!packageName.equals("")) { //$NON-NLS-1$
            writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.println();
        }      
        writer.println("import org.eclipse.swt.graphics.Point;"); //$NON-NLS-1$
        writer.println("import org.eclipse.ui.application.IWorkbenchWindowConfigurer;"); //$NON-NLS-1$
        writer.println("import org.eclipse.ui.application.WorkbenchAdvisor;"); //$NON-NLS-1$
        writer.println();
        
        writer.println("public class SampleWorkbenchAdvisor extends WorkbenchAdvisor {"); //$NON-NLS-1$
        writer.println();
        writer.println("\tpublic String getInitialWindowPerspectiveId() {"); //$NON-NLS-1$
        writer.println("\t\treturn \"" + fPluginData.getId() + ".samplePerspective\";"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.println("\t}"); //$NON-NLS-1$
        writer.println();	
        writer.println("\tpublic void preWindowOpen(IWorkbenchWindowConfigurer configurer) {"); //$NON-NLS-1$
        writer.println("\t\tsuper.preWindowOpen(configurer);"); //$NON-NLS-1$
        writer.println("\t\tconfigurer.setInitialSize(new Point(400, 300));"); //$NON-NLS-1$
        writer.println("\t\tconfigurer.setShowCoolBar(false);"); //$NON-NLS-1$
        writer.println("\t\tconfigurer.setShowStatusLine(false);"); //$NON-NLS-1$
        writer.println("\t\tconfigurer.setTitle(\"Hello RCP\");"); //$NON-NLS-1$
        writer.println("\t}"); //$NON-NLS-1$
        writer.println("}"); //$NON-NLS-1$
        writer.flush();
        try {
            swriter.close();
            ByteArrayInputStream stream = new ByteArrayInputStream(swriter
                    .toString().getBytes(fProject.getDefaultCharset()));
            if (file.exists())
                file.setContents(stream, false, true, null);
            else
                file.create(stream, false, null);
            stream.close();
        } catch (IOException e) {
        }
    }
    
    private void generatePerspectiveClass(IPath path, String packageName) throws CoreException {
    	IFile file = fProject.getFile(path.append("SamplePerspective.java")); //$NON-NLS-1$
        StringWriter swriter = new StringWriter();
        PrintWriter writer = new PrintWriter(swriter);
    	
    	if (!packageName.equals("")) { //$NON-NLS-1$
            writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.println();
        }      
    	writer.println("import org.eclipse.ui.IPageLayout;"); //$NON-NLS-1$
    	writer.println("import org.eclipse.ui.IPerspectiveFactory;"); //$NON-NLS-1$
    	writer.println();
    	writer.println("public class SamplePerspective implements IPerspectiveFactory {"); //$NON-NLS-1$
    	writer.println();
    	writer.println("\tpublic void createInitialLayout(IPageLayout layout) {"); //$NON-NLS-1$
    	writer.println("\t}"); //$NON-NLS-1$
    	writer.println("}"); //$NON-NLS-1$
        writer.flush();
        try {
            swriter.close();
            ByteArrayInputStream stream = new ByteArrayInputStream(swriter
                    .toString().getBytes(fProject.getDefaultCharset()));
            if (file.exists())
                file.setContents(stream, false, true, null);
            else
                file.create(stream, false, null);
            stream.close();
        } catch (IOException e) {
        }
    }

    private void generateApplicationClass(String packageName, String className,
            PrintWriter writer) {
        if (!packageName.equals("")) { //$NON-NLS-1$
            writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.println();
        }
        writer.println("import org.eclipse.core.runtime.IPlatformRunnable;"); //$NON-NLS-1$
        writer.println("import org.eclipse.swt.widgets.Display;"); //$NON-NLS-1$
        writer.println("import org.eclipse.ui.PlatformUI;");				 //$NON-NLS-1$
        writer.println();
        
        writer.println("/**"); //$NON-NLS-1$
        writer.println(" * This class controls all aspects of the application's execution"); //$NON-NLS-1$
        writer.println(" */"); //$NON-NLS-1$
        writer.println("public class " + className + " implements IPlatformRunnable {"); //$NON-NLS-1$ //$NON-NLS-2$
        writer.println();
        writer.println("\t/* (non-Javadoc)"); //$NON-NLS-1$
        writer.println("\t * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)"); //$NON-NLS-1$
        writer.println("\t */"); //$NON-NLS-1$
        writer.println("\tpublic Object run(Object args) throws Exception {"); //$NON-NLS-1$
        writer.println("\t\tDisplay display = PlatformUI.createDisplay();"); //$NON-NLS-1$
        writer.println("\t\ttry {"); //$NON-NLS-1$
        writer.println("\t\t\tint returnCode = PlatformUI.createAndRunWorkbench(display, new SampleWorkbenchAdvisor());"); //$NON-NLS-1$
        writer.println("\t\t\tif (returnCode == PlatformUI.RETURN_RESTART) {"); //$NON-NLS-1$
        writer.println("\t\t\t\treturn IPlatformRunnable.EXIT_RESTART;"); //$NON-NLS-1$
        writer.println("\t\t\t}"); //$NON-NLS-1$
        writer.println("\t\t\treturn IPlatformRunnable.EXIT_OK;");           //$NON-NLS-1$
        writer.println("\t\t} finally {"); //$NON-NLS-1$
        writer.println("\t\t\tdisplay.dispose();"); //$NON-NLS-1$
        writer.println("\t\t}"); //$NON-NLS-1$
        writer.println("\t}"); //$NON-NLS-1$
        writer.println("}"); //$NON-NLS-1$
    }
}