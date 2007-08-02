/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ExternalizeStringsOperation extends WorkspaceModifyOperation {

	private Object[] fChangeFiles;
	private CompositeChange fParentChange;
	private HashMap fCompositeChanges;
	private HashMap fFileEdits;
	
	public ExternalizeStringsOperation(Object[] changeFiles, CompositeChange parentChange) {
		fChangeFiles = changeFiles;
		fParentChange = parentChange;
		fCompositeChanges = new HashMap();
		fFileEdits = new HashMap();
	}
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		for (int i = 0; i < fChangeFiles.length; i++) {
			if (fChangeFiles[i] instanceof ModelChangeFile) {
				ModelChangeFile changeFile = (ModelChangeFile)fChangeFiles[i];
				CompositeChange pluginChange = getChangeForPlugin(changeFile.getModel().getParentModel().getPluginBase().getId());
				ModelChange change = changeFile.getModel();
				IFile pFile = change.getPropertiesFile();
				// if the properties file does not exist and we have not already made a TextFileChange
				// for it create the Change and insert a comment
				if (!pFile.exists() && !fFileEdits.containsKey(pFile))
					getEditForFile(pFile, pluginChange).addChild(new InsertEdit(0, getPropertiesFileComment(pFile)));
				if (!change.localizationSet())
					addBundleLocalization(change, monitor, pluginChange);
				
				ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
				try {
					pManager.connect(pFile.getFullPath(), LocationKind.IFILE, monitor);
					ITextFileBuffer pBuffer = pManager.getTextFileBuffer(pFile.getFullPath(), LocationKind.IFILE);
					IDocument pDoc = pBuffer.getDocument();
					MultiTextEdit pEdit = getEditForFile(pFile, pluginChange);
					
					doReplace(changeFile, pDoc, pEdit, monitor, pluginChange);
					
				} catch (MalformedTreeException e) {
				} finally {
					pManager.disconnect(pFile.getFullPath(), LocationKind.IFILE, monitor);
				}
			}
		}
	}
	private CompositeChange getChangeForPlugin(String pluginName) {
		if (fCompositeChanges.containsKey(pluginName))
			return (CompositeChange) fCompositeChanges.get(pluginName);
		CompositeChange result = new CompositeChange(NLS.bind(PDEUIMessages.ExternalizeStringsOperation_pluginChangeName, pluginName));
		fCompositeChanges.put(pluginName, result);
		fParentChange.add(result);
		return result;
	}
	private MultiTextEdit getEditForFile(IFile file, CompositeChange parentChange) {
		if (fFileEdits.containsKey(file))
			return (MultiTextEdit) fFileEdits.get(file);
		MultiTextEdit edit = new MultiTextEdit();
		TextFileChange change = new TextFileChange(file.getName(), file);
		change.setEdit(edit);
		// mark a plugin.xml or a fragment.xml as PLUGIN2 type so they will be compared
		// with the PluginContentMergeViewer
		String textType = file.getName().equals("plugin.xml") || //$NON-NLS-1$
				file.getName().equals("fragment.xml") ? //$NON-NLS-1$
				"PLUGIN2" : file.getFileExtension(); //$NON-NLS-1$
		change.setTextType(textType);
		parentChange.add(change);
		fFileEdits.put(file, edit);
		return edit;
	}
	private void doReplace(ModelChangeFile changeFile, IDocument pDoc, MultiTextEdit pEdit, IProgressMonitor monitor, CompositeChange parentChange) throws CoreException {
		IFile uFile = changeFile.getFile();
		try {
			MultiTextEdit uEdit = getEditForFile(uFile, parentChange);
			
			Iterator iter = changeFile.getChanges().iterator();
			
			while (iter.hasNext()) {
				ModelChangeElement changeElement = (ModelChangeElement)iter.next();
				if (changeElement.isExternalized()) {
					uEdit.addChild(new ReplaceEdit(changeElement.getOffset(),
							changeElement.getLength(), 
							changeElement.getExternKey()));
					pEdit.addChild(getPropertiesInsertEdit(pDoc, changeElement));
				}
			}
		} catch (MalformedTreeException e) {
		}
 	}
	
	private void addBundleLocalization(ModelChange change, IProgressMonitor mon, CompositeChange parent) {
		IPluginModelBase base = change.getParentModel();
		IFile manifest = base.getUnderlyingResource().getProject().getFile(PDEModelUtility.F_MANIFEST_FP);
		// if the edit for this manifest file is in the HashMap, then we must have added
		// the localization already since it is checked first (this must be the second or subsequent
		// change to the manifest for this plug-in)
		if (fFileEdits.containsKey(manifest))
			return;
		final String localiz = change.getBundleLocalization();
		TextFileChange[] result = PDEModelUtility.changesForModelModication(new ModelModification(manifest) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModel) {
					IBundlePluginModel bundleModel = (IBundlePluginModel) model;
					IBundle bundle = bundleModel.getBundleModel().getBundle();
					bundle.setLocalization(localiz);
				}
			}
		}, mon);
		// this model change just adds localization to the manifest, so we will only have one change
		// with one edit
		if (result.length > 0 && result[0] != null)
			getEditForFile(manifest, parent).addChild(result[0].getEdit());
	}
	
	public static InsertEdit getPropertiesInsertEdit(IDocument doc, ModelChangeElement element) {
		String nl = TextUtilities.getDefaultLineDelimiter(doc);
		StringBuffer sb = new StringBuffer(nl);
		sb.append(element.getKey());
		sb.append(" = "); //$NON-NLS-1$
		sb.append(StringHelper.preparePropertiesString(element.getValue(), nl.toCharArray()));
		return new InsertEdit(doc.getLength(), sb.toString());
	}
	
	public static String getPropertiesFileComment(IFile file) {
		return NLS.bind("#Properties file for {0}", file.getProject().getName()); //$NON-NLS-1$
	}
 }
