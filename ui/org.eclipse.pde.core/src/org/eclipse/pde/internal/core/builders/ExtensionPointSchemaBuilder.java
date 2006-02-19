/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;

public class ExtensionPointSchemaBuilder extends IncrementalProjectBuilder {
	class DeltaVisitor implements IResourceDeltaVisitor {
		private IProgressMonitor monitor;
		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject)
				return isInterestingProject((IProject)resource);
			
			if (resource instanceof IFolder)
				return true;
			
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isSchemaFile(candidate)) {
					// That's it, but only check it if it has been added or changed
					if (delta.getKind() != IResourceDelta.REMOVED) {
						compileFile(candidate, monitor);
					} else {
						removeOutputFile(candidate, monitor);
					}
				}
			}
			return false;
		}
	}

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException {
		IResourceDelta delta = null;
		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			if (isInterestingProject(getProject()))
				compileSchemasIn(getProject(), monitor);
		} else {
			delta.accept(new DeltaVisitor(monitor));
		}
		return new IProject[0];
	}
	
	private boolean isInterestingProject(IProject project) {
		return PDE.hasPluginNature(project) && !WorkspaceModelManager.isBinaryPluginProject(project);
	}

	private void compileFile(IFile file, IProgressMonitor monitor) {
		
		String message = NLS.bind(PDECoreMessages.Builders_Schema_compiling, file.getFullPath().toString());
		monitor.subTask(message);
		
		SchemaErrorReporter reporter = new SchemaErrorReporter(file);
		ValidatingSAXParser.parse(file, reporter);
		reporter.validateContent(monitor);

		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		try {
			boolean generateDoc = CompilerFlags.getBoolean(file.getProject(), CompilerFlags.S_CREATE_DOCS);
			if (reporter.getDocumentRoot() != null && reporter.getErrorCount() == 0 && generateDoc) {
				ensureFoldersExist(file.getProject(), getDocLocation(file));
				String outputFileName = getOutputFileName(file);
				IWorkspace workspace = file.getWorkspace();
				Path outputPath = new Path(outputFileName);

				SchemaDescriptor desc = new SchemaDescriptor(file, false);
				Schema schema = (Schema)desc.getSchema(false);
				
				SchemaTransformer transformer = new SchemaTransformer();
				transformer.transform(schema, writer);
				
				ByteArrayInputStream target = new ByteArrayInputStream(swriter.toString().getBytes("UTF8")); //$NON-NLS-1$
				IFile outputFile = workspace.getRoot().getFile(outputPath);
				if (!workspace.getRoot().exists(outputPath)) {
					outputFile.create(target, true, monitor);
				} else {
					outputFile.setContents(target, true, false, monitor);
				}
			}
		} catch (UnsupportedEncodingException e) {
			PDECore.logException(e);
		} catch (CoreException e) {
			PDECore.logException(e);
		} finally {
			writer.close();
			try {
				swriter.close();
			} catch (IOException e1) {
			}
		}
		monitor.subTask(PDECoreMessages.Builders_updating);
		monitor.done();
	}
	
	private void ensureFoldersExist(IProject project, String pathName) throws CoreException {
		IPath path = new Path(pathName);
		IContainer parent=project;
		
		for (int i = 0; i < path.segmentCount(); i++) {
			String segment = path.segment(i);
			IFolder folder = parent.getFolder(new Path(segment));
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
			parent = folder;
		}
	}

	private void compileSchemasIn(IContainer container, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(PDECoreMessages.Builders_Schema_compilingSchemas);
		IResource[] members = container.members();
		for (int i = 0; i < members.length; i++) {
			IResource member = members[i];
			if (member instanceof IContainer)
				compileSchemasIn((IContainer) member, monitor);
			else if (member instanceof IFile && isSchemaFile((IFile) member)) {
				compileFile((IFile) member, monitor);
			}
		}
		monitor.done();
	}
	
	private String getDocLocation(IFile file) {
		return CompilerFlags.getString(file.getProject(), CompilerFlags.S_DOC_FOLDER);
	}
	
	private String getOutputFileName(IFile file) {
		String fileName = file.getName();
		int dot = fileName.lastIndexOf('.');
		String pageName = fileName.substring(0, dot) + ".html"; //$NON-NLS-1$
		String mangledPluginId = getMangledPluginId(file);
		if (mangledPluginId != null)
			pageName = mangledPluginId + "_" + pageName; //$NON-NLS-1$
		IPath path = file.getProject().getFullPath().append(
				getDocLocation(file)).append(pageName);
		return path.toString();
	}
	
	private String getMangledPluginId(IFile file) {
		IProject project = file.getProject();
		IModel model = PDECore.getDefault().getModelManager().findModel(project);
		if (model instanceof IPluginModelBase) {
			IPluginBase plugin = ((IPluginModelBase) model).getPluginBase();
			if (plugin != null) {
				return plugin.getId().replace('.', '_');
			}
		}
		return null;
	}
	
	private boolean isSchemaFile(IFile file) {
		return "exsd".equals(file.getFileExtension()); //$NON-NLS-1$
	}
	
	private void removeOutputFile(IFile file, IProgressMonitor monitor) {
		String outputFileName = getOutputFileName(file);
		monitor.subTask(NLS.bind(PDECoreMessages.Builders_Schema_removing, outputFileName));

		IWorkspace workspace = file.getWorkspace();
		IPath path = new Path(outputFileName);
		if (workspace.getRoot().exists(path)) {
			IFile outputFile = workspace.getRoot().getFile(path);
			if (outputFile != null) {
				try {
					outputFile.delete(true, true, monitor);
				} catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		}
		monitor.done();
	}
}
