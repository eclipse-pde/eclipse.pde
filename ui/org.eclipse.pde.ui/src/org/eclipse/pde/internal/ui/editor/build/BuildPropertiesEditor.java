/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.build.ExternalBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.IPDEEditorPage;
import org.eclipse.pde.internal.ui.editor.PDEMultiPageEditor;
import org.eclipse.pde.internal.ui.preferences.EditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;

public class BuildPropertiesEditor extends PDEMultiPageEditor {
	public static final String BUILD_PAGE_TITLE = "BuildPropertiesEditor.BuildPage.title";
	public static final String BUILD_PAGE = "BuildPage";
	public static final String SOURCE_PAGE = "SourcePage";
	public static final String OUTPUT_PREFIX = "output.";

	public BuildPropertiesEditor() {
		super();
	}
	protected Object createModel(Object input) throws CoreException {
		if (input instanceof IFile)
			return createResourceModel((IFile) input);
		if (input instanceof IStorage)
			return createStorageModel((IStorage) input);
		return null;
	}

	protected void createPages() {
		addPage(SOURCE_PAGE, new BuildSourcePage(this));
	}
	
	private IBuildModel createResourceModel(IFile file) throws CoreException {
		InputStream stream = file.getContents(false);

		IBuildModel model = new WorkspaceBuildModel(file);
		try {
			model.load(stream, false);
		} catch (CoreException e) {
		}
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return model;
	}

	private IBuildModel createStorageModel(IStorage storage)
		throws CoreException {
		InputStream stream = null;

		stream = storage.getContents();

		ExternalBuildModel model = new ExternalBuildModel("");
		model.load(stream, false);
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return model;
	}

	public void dispose() {
		super.dispose();
		IModel model = (IModel) getModel();
		model.dispose();		
	}
	
	public IPDEEditorPage getHomePage() {
		if (model instanceof ExternalBuildModel)
			return getPage(SOURCE_PAGE);
		return getPage(BUILD_PAGE);
	}
	protected String getSourcePageId() {
		return SOURCE_PAGE;
	}
	
	public void createPartControl(Composite parent) {
		if (model instanceof WorkspaceBuildModel) {
			firstPageId = BUILD_PAGE;
			formWorkbook.setFirstPageSelected(false);
			BuildPage buildPage =
				new BuildPage(this, PDEPlugin.getResourceString(BUILD_PAGE_TITLE));
			addPage(BUILD_PAGE, buildPage, 0);
		}
		super.createPartControl(parent);
		if (model instanceof ExternalBuildModel) {
			firstPageId = SOURCE_PAGE;
			showPage(SOURCE_PAGE);
		}
	}

	public void openTo(Object obj, IMarker marker) {
		if (EditorPreferencePage.getUseSourcePage() || model instanceof ExternalBuildModel) {
			PDESourcePage sourcePage =
				(PDESourcePage) showPage(getSourcePageId());
			if (marker != null)
				sourcePage.openTo(marker);
		} else {
			IPDEEditorPage page = getPageFor(obj);
			if (page != null) {
				showPage(page);
				page.openTo(obj);
			}
		}
	}

	public String getTitle() {
		IEditorInput input = getEditorInput();
		if (input instanceof IStorageEditorInput
			&& !(input instanceof IFileEditorInput)) {
			return ((IStorageEditorInput) input).getName();
		}
		return super.getTitle();
	}
	
	protected boolean isModelDirty(Object model) {
		return model != null
			&& model instanceof IEditable
			&& model instanceof IModel
			&& ((IEditable) model).isDirty();
	}
	protected boolean isValidContentType(IEditorInput input) {
		String name = input.getName().toLowerCase();
		if (input instanceof IStorageEditorInput
			&& !(input instanceof IFileEditorInput)) {
			if (name.startsWith("build.properties"))
				return true;
			else
				return false;
		}
		if (name.equals("build.properties"))
			return true;
		return false;
	}
	protected boolean updateModel() {
		IBuildModel model = (IBuildModel) getModel();
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		String text = document.get();
		boolean cleanModel = true;
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			try {
				model.reload(stream, false);
				if (model instanceof IEditable) {
					((IEditable)model).setDirty(false);
					fireSaveNeeded();
				}
			} catch (CoreException e) {
				cleanModel = false;
			}
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
		}
		return cleanModel;
	}
	
	public void doSave(IProgressMonitor monitor) {
		validateSourceFolders(monitor);
		super.doSave(monitor);
	}
	
	private void validateSourceFolders(IProgressMonitor monitor) {
		String[] folders = getFolderNames();
		if (folders.length > 0) {
			IPackageFragmentRoot[] sourceFolders = computeSourceFolders();
			if (sourceFolders.length == 0)
				return;
			ArrayList list = new ArrayList();
			for (int i = 0; i < folders.length; i++) {
				if (getSourceFolder(folders[i], sourceFolders) == null)
					list.add(folders[i]);
			}
			if (list.size() > 0)
				convertToSourceFolders(list, monitor);
			refreshOutputKeys(sourceFolders);		
		}
	}
	
	private String[] getFolderNames() {
		ArrayList folderNames = new ArrayList();
		IBuildModel buildModel = (IBuildModel)getModel();
		IBuildEntry[] entries =
			BuildUtil.getBuildLibraries(buildModel.getBuild().getBuildEntries());
		for (int i = 0; i < entries.length; i++) {
			String[] tokens = entries[i].getTokens();
			for (int j = 0; j < tokens.length; j++) {
				if (!folderNames.contains(tokens[j]))
					folderNames.add(tokens[j]);
			}
		}
		return (String[]) folderNames.toArray(new String[folderNames.size()]);
	}
	
	private void refreshOutputKeys(IPackageFragmentRoot[] sourceFolders) {
		IBuildModel buildModel = (IBuildModel) getModel();
		IBuild build = buildModel.getBuild();
		IBuildEntry[] libraries = BuildUtil.getBuildLibraries(build.getBuildEntries());

		String[] jarFolders;
		IPackageFragmentRoot sourceFolder;
		IClasspathEntry entry;
		IPath outputPath;
		Set outputFolders;
		try {
			for (int i = 0; i < libraries.length; i++) {
				jarFolders = libraries[i].getTokens();
				outputFolders = new HashSet();
				for (int j = 0; j < jarFolders.length; j++) {
					sourceFolder = getSourceFolder(jarFolders[j], sourceFolders);
					if (sourceFolder != null) {
						entry = sourceFolder.getRawClasspathEntry();
						outputPath = entry.getOutputLocation();
						if (outputPath == null) {
							outputFolders.add("bin");
						} else {
							outputPath = outputPath.removeFirstSegments(1);
							outputFolders.add(outputPath.toString());
						}
					}
				}

				createOutputKey(
					libraries[i].getName().replaceFirst(IBuildEntry.JAR_PREFIX, ""),
					outputFolders);
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}

	}
	
	private void createOutputKey(String libName, Set outputFolders){
		if (outputFolders.size()==0)
			return;
		IBuildModel buildModel = (IBuildModel)getModel();
		IBuild build = buildModel.getBuild();
		String outputName = OUTPUT_PREFIX + libName;
		IBuildEntry outputEntry = build.getEntry(outputName);
		Iterator iter = outputFolders.iterator();
		try {
			if (outputEntry == null){		
				outputEntry = buildModel.getFactory().createEntry(outputName);
				build.add(outputEntry);
			} else {
				String[] tokens = outputEntry.getTokens();
				for (int i = 0 ; i<tokens.length; i++ ){
					outputEntry.removeToken(tokens[i]);
				}
			}
			
			
			while(iter.hasNext()){
				String outputFolder = iter.next().toString();
				if (!outputFolder.endsWith(""+ Path.SEPARATOR))
					outputFolder = outputFolder.concat(""+Path.SEPARATOR);
				outputEntry.addToken(outputFolder.toString());
			}
			
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		
	}	
	
	private void convertToSourceFolders(
		ArrayList folders,
		IProgressMonitor monitor) {
		IBuildModel buildModel = (IBuildModel) getModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);

		Vector newSrcEntries = new Vector();
		for (int i = 0; i < folders.size(); i++) {
			String folderName = folders.get(i).toString();
			IPath path = project.getFullPath().append(folderName);
			IFolder folder = project.getWorkspace().getRoot().getFolder(path);
			newSrcEntries.add(JavaCore.newSourceEntry(folder.getFullPath()));
		}

		try {
			IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
			IClasspathEntry[] newEntries =
				new IClasspathEntry[oldEntries.length + newSrcEntries.size()];
			System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
			for (int i = 0; i < newSrcEntries.size(); i++)
				newEntries[oldEntries.length + i] =
					(IClasspathEntry) newSrcEntries.elementAt(i);

			javaProject.setRawClasspath(newEntries, new SubProgressMonitor(monitor, 1));
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}

	}
	
	private IPackageFragmentRoot[] computeSourceFolders() {
		ArrayList folders = new ArrayList();
		IBuildModel buildModel = (IBuildModel)getModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots =
					jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						folders.add(roots[i]);
					}
				}
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return (IPackageFragmentRoot[]) folders.toArray(
			new IPackageFragmentRoot[folders.size()]);
	}

	private IPackageFragmentRoot getSourceFolder(
		String folderName,
		IPackageFragmentRoot[] sourceFolders) {
		for (int i = 0; i < sourceFolders.length; i++) {
			if (sourceFolders[i]
				.getPath()
				.removeFirstSegments(1)
				.equals(new Path(folderName))) {
				return sourceFolders[i];
			}
		}
		return null;
	}
}
