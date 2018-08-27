/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 201994
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 274454
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

public class ExternalizeStringsOperation extends WorkspaceModifyOperation {

	private Object[] fChangeFiles;
	private CompositeChange fParentChange;
	private HashMap<String, CompositeChange> fCompositeChanges;
	private HashMap<IFile, TextFileChange> fFileChanges;

	public ExternalizeStringsOperation(Object[] changeFiles, CompositeChange parentChange) {
		fChangeFiles = changeFiles;
		fParentChange = parentChange;
		fCompositeChanges = new HashMap<>();
		fFileChanges = new HashMap<>();
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		for (Object changeFileObject : fChangeFiles) {
			if (changeFileObject instanceof ModelChangeFile) {
				ModelChangeFile changeFile = (ModelChangeFile) changeFileObject;
				CompositeChange pluginChange = getChangeForPlugin(changeFile.getModel().getParentModel().getPluginBase().getId());
				ModelChange change = changeFile.getModel();
				IFile pFile = change.getPropertiesFile();
				// if the properties file does not exist and we have not already made a TextFileChange
				// for it create the Change and insert a comment
				if (!pFile.exists() && !fFileChanges.containsKey(pFile)) {
					TextFileChange fileChange = getChangeForFile(pFile, pluginChange);
					InsertEdit edit = new InsertEdit(0, getPropertiesFileComment(pFile));
					fileChange.getEdit().addChild(edit);
					fileChange.addTextEditGroup(new TextEditGroup(PDEUIMessages.ExternalizeStringsOperation_editNames_addComment, edit));
				}
				if (!change.localizationSet())
					addBundleLocalization(change, monitor, pluginChange);

				// Update build.properties file (if exists & not already done)
				IFile buildProps = PDEProject.getBuildProperties(changeFile.getFile().getProject());
				if (buildProps != null && buildProps.exists() && !fFileChanges.containsKey(buildProps)) {
					getChangeForBuild(buildProps, monitor, pluginChange, change.getBundleLocalization());
				}

				ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
				try {
					pManager.connect(pFile.getFullPath(), LocationKind.IFILE, monitor);
					ITextFileBuffer pBuffer = pManager.getTextFileBuffer(pFile.getFullPath(), LocationKind.IFILE);
					IDocument pDoc = pBuffer.getDocument();
					TextFileChange pChange = getChangeForFile(pFile, pluginChange);

					doReplace(changeFile, pDoc, pChange, monitor, pluginChange);

				} catch (MalformedTreeException e) {
				} finally {
					pManager.disconnect(pFile.getFullPath(), LocationKind.IFILE, monitor);
				}
			}
		}
	}

	private void getChangeForBuild(IFile buildPropsFile, IProgressMonitor monitor, CompositeChange parent, final String localization) {
		// Create change
		TextFileChange[] changes = PDEModelUtility.changesForModelModication(new ModelModification(buildPropsFile) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {

				// Get model & set includes entry...
				if (model instanceof IBuildModel) {
					IBuildModel buildModel = (IBuildModel) model;
					IBuildEntry binIncludes = buildModel.getBuild().getEntry(IBuildEntry.BIN_INCLUDES);
					if (binIncludes == null) {
						binIncludes = buildModel.getFactory().createEntry(IBuildEntry.BIN_INCLUDES);
					}
					// Add new entry to bin.includes key
					binIncludes.addToken(localization + ".properties"); //$NON-NLS-1$
				}
			}
		}, monitor);
		// Add to changes tree
		if (changes.length > 0 && changes[0] != null) {
			fFileChanges.put(buildPropsFile, changes[0]);
			parent.add(changes[0]);
		}
	}

	private CompositeChange getChangeForPlugin(String pluginName) {
		if (fCompositeChanges.containsKey(pluginName))
			return fCompositeChanges.get(pluginName);
		CompositeChange result = new CompositeChange(NLS.bind(PDEUIMessages.ExternalizeStringsOperation_pluginChangeName, pluginName));
		fCompositeChanges.put(pluginName, result);
		fParentChange.add(result);
		return result;
	}

	private TextFileChange getChangeForFile(IFile file, CompositeChange parentChange) {
		if (fFileChanges.containsKey(file))
			return fFileChanges.get(file);
		MultiTextEdit edit = new MultiTextEdit();
		TextFileChange change = new TextFileChange(file.getName(), file);
		change.setEdit(edit);
		// mark a plugin.xml or a fragment.xml as PLUGIN2 type so they will be compared
		// with the PluginContentMergeViewer
		String textType = file.getName().equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || file.getName().equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) ? "PLUGIN2" //$NON-NLS-1$
				: file.getFileExtension();
		change.setTextType(textType);
		parentChange.add(change);
		fFileChanges.put(file, change);
		return change;
	}

	/**
	 * @param changeFile
	 * @param pDoc
	 * @param pChange
	 * @param monitor
	 * @param parentChange
	 * @throws CoreException
	 */
	private void doReplace(ModelChangeFile changeFile, IDocument pDoc, TextFileChange pChange, IProgressMonitor monitor, CompositeChange parentChange) throws CoreException {
		IFile uFile = changeFile.getFile();
		try {
			TextFileChange uChange = getChangeForFile(uFile, parentChange);

			Iterator<?> iter = changeFile.getChanges().iterator();

			while (iter.hasNext()) {
				ModelChangeElement changeElement = (ModelChangeElement) iter.next();
				if (changeElement.isExternalized()) {
					ReplaceEdit uEdit = new ReplaceEdit(changeElement.getOffset(), changeElement.getLength(), changeElement.getExternKey());
					uChange.getEdit().addChild(uEdit);
					uChange.addTextEditGroup(new TextEditGroup(NLS.bind(PDEUIMessages.ExternalizeStringsOperation_editNames_replaceText, changeElement.getKey()), uEdit));
					InsertEdit pEdit = getPropertiesInsertEdit(pDoc, changeElement);
					pChange.getEdit().addChild(pEdit);
					pChange.addTextEditGroup(new TextEditGroup(NLS.bind(PDEUIMessages.ExternalizeStringsOperation_editNames_insertProperty, changeElement.getKey()), pEdit));
				}
			}
		} catch (MalformedTreeException e) {
		}
	}

	private void addBundleLocalization(ModelChange change, IProgressMonitor mon, CompositeChange parent) {
		IPluginModelBase base = change.getParentModel();
		IFile manifest = PDEProject.getManifest(base.getUnderlyingResource().getProject());
		// if the edit for this manifest file is in the HashMap, then we must have added
		// the localization already since it is checked first (this must be the second or subsequent
		// change to the manifest for this plug-in)
		if (fFileChanges.containsKey(manifest))
			return;
		final String localiz = change.getBundleLocalization();
		TextFileChange[] result = PDEModelUtility.changesForModelModication(new ModelModification(manifest) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					IBundlePluginModelBase bundleModel = (IBundlePluginModelBase) model;
					IBundle bundle = bundleModel.getBundleModel().getBundle();
					bundle.setHeader(Constants.BUNDLE_LOCALIZATION, localiz);
				}
			}
		}, mon);
		// this model change just adds localization to the manifest, so we will only have one change
		// with one edit
		if (result.length > 0 && result[0] != null) {
			// we know the manifest does not already have a change in the HashMap, so just add the resultant one
			fFileChanges.put(manifest, result[0]);
			parent.add(result[0]);
		}
	}

	public static InsertEdit getPropertiesInsertEdit(IDocument doc, ModelChangeElement element) {
		String nl = TextUtilities.getDefaultLineDelimiter(doc);
		StringBuilder sb = new StringBuilder(nl);
		sb.append(element.getKey());
		sb.append(" = "); //$NON-NLS-1$
		sb.append(StringHelper.preparePropertiesString(element.getValue(), nl.toCharArray()));
		return new InsertEdit(doc.getLength(), sb.toString());
	}

	public static String getPropertiesFileComment(IFile file) {
		IPluginModelBase model = PluginRegistry.findModel(file.getProject());
		if (model != null) {
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase != null)
				return NLS.bind("#Properties file for {0}", pluginBase.getId()); //$NON-NLS-1$
		}
		return NLS.bind("#{0}", file.getName()); //$NON-NLS-1$
	}
}
