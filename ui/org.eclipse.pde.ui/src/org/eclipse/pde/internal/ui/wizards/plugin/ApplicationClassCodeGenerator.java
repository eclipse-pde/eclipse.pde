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
        return file;
    }

    private void generateApplicationClass(String packageName, String className,
            PrintWriter writer) {
        if (!packageName.equals("")) { //$NON-NLS-1$
            writer.println("package " + packageName + ";"); //$NON-NLS-1$ //$NON-NLS-2$
            writer.println();
        }

        writer.println("import org.eclipse.core.runtime.IPlatformRunnable;"); //$NON-NLS-1$
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
        writer.println("\t\t// TODO Auto-generated method stub"); //$NON-NLS-1$
        writer.println("\t\treturn null;"); //$NON-NLS-1$
        writer.println("\t}"); //$NON-NLS-1$
        writer.println("}"); //$NON-NLS-1$
    }
}