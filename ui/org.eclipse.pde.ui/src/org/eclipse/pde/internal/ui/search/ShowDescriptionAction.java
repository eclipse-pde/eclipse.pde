/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 215523
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.pde.internal.core.ICoreConstants;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class ShowDescriptionAction extends Action {
	private String fPointID;
	private ISchema fSchema;
	private File fPreviewFile;
	private boolean fForceExternal;

	private static File fTempWorkingDir;

	public ShowDescriptionAction(String pointID) {
		fPointID = pointID;
		initialize();
	}

	public ShowDescriptionAction(IPluginExtensionPoint point) {
		this(point, false);
	}

	public ShowDescriptionAction(IPluginExtensionPoint point, boolean forceExternal) {
		setExtensionPoint(point.getFullId());
		fForceExternal = forceExternal;
		initialize();
	}

	public ShowDescriptionAction(IPluginExtensionPoint point, String pointID) {
		setExtensionPoint(pointID);
		fForceExternal = false;
		initialize();
	}

	public ShowDescriptionAction(ISchema schema) {
		setSchema(schema);
		initialize();
	}

	private void initialize() {
		setImageDescriptor(PDEPluginImages.DESC_DOC_SECTION_OBJ);
	}

	public void setSchema(ISchema schema) {
		fSchema = schema;
		fPointID = schema.getQualifiedPointId();
	}

	public void setExtensionPoint(String pointID) {
		fPointID = pointID;
		setText(PDEUIMessages.ShowDescriptionAction_label);
		fSchema = null;
	}

	public void run() {
		if (fSchema == null) {
			IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(fPointID);
			URL url = null;
			if (point != null) {
				url = SchemaRegistry.getSchemaURL(point);
				if (url != null) {
					ISchemaDescriptor desc = new SchemaDescriptor(fPointID, url);
					fSchema = desc.getSchema(false);
				}
			}
			if (point == null || url == null || fSchema == null) {
				showNoSchemaMessage();
				return;
			}
		}
		showSchemaDocument();
	}

	private void showNoSchemaMessage() {
		String title = PDEUIMessages.ShowDescriptionAction_title;
		String message;
		if (fPointID == null || fPointID.startsWith("null")) //$NON-NLS-1$
			message = PDEUIMessages.ShowDescriptionAction_schemaNotAvail;
		else
			message = NLS.bind(PDEUIMessages.ShowDescriptionAction_noPoint_desc, fPointID);
		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}

	private void showSchemaDocument() {
		try {
			fPreviewFile = getTempPreviewFile();
			if (fPreviewFile == null)
				return;

			SchemaTransformer transformer = new SchemaTransformer();
			OutputStream os = new FileOutputStream(fPreviewFile);
			PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, ICoreConstants.UTF_8), true);
			transformer.transform(fSchema, printWriter);
			os.flush();
			os.close();
			showURL(fPreviewFile, fForceExternal);
			// Associate the generated preview file with the schema file
			// to enable automatic preview file updates on schema file changes
			linkPreviewFileToSchemaFile();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

	/**
	 * @return the temporary working directory
	 * 
	 * @throws IOException
	 */
	private File getTempWorkingDir() throws IOException {
		if (fTempWorkingDir == null) {
			fTempWorkingDir = Utilities.createWorkingDirectory();
		}
		return fTempWorkingDir;
	}

	/**
	 * @return the temporary preview file
	 */
	private File getTempPreviewFile() {
		// Get the temporary working directory
		File tempWorkingDir = null;
		try {
			tempWorkingDir = getTempWorkingDir();
		} catch (IOException e) {
			return null;
		}
		// Generate a consistent unique preview file name for this schema
		StringBuffer previewFileName = new StringBuffer();
		previewFileName.append("pde_schema_"); //$NON-NLS-1$
		previewFileName.append(fSchema.getQualifiedPointId().replace('.', '-'));
		previewFileName.append("_preview.html"); //$NON-NLS-1$
		File previewFile = new File(tempWorkingDir.getPath() + File.separatorChar + previewFileName.toString());
		// If the file does not exist yet, create it within the temporary
		// working diretory
		if (previewFile.exists() == false) {
			try {
				previewFile.createNewFile();
			} catch (IOException e) {
				return null;
			}
			// Mark file for deletion on VM exit
			previewFile.deleteOnExit();
		}

		return previewFile;
	}

	/**
	 * 
	 */
	private void linkPreviewFileToSchemaFile() {
		// Ensure the preview file is defined
		if (fPreviewFile == null) {
			return;
		}
		// Get the schema file
		IFile schemaFile = getSchemaFile();
		// Ensure we found the workspace Eclipse schema file
		if (schemaFile == null) {
			return;
		}
		// Set the preview file on the Eclipse schema file resource.
		// Later on, content changes to the Eclipse schema file in the workspace
		// will result in an automatic regeneration of the schema preview
		// contents
		// This is handled in
		// org.eclipse.pde.internal.core.WorkspacePluginModelManager.handleEclipseSchemaDelta(IFile, IResourceDelta)
		try {
			schemaFile.setSessionProperty(PDECore.SCHEMA_PREVIEW_FILE, fPreviewFile);
		} catch (CoreException e) {
			// Ignore
		}
	}

	/**
	 * @return the schema file
	 */
	private IFile getSchemaFile() {
		// Ensure the schema is defined
		if (fSchema == null) {
			return null;
		}
		// Get the Java schema file
		File javaSchemaFile = new File(fSchema.getURL().getFile());
		// Get the Eclipse schema file
		IFile[] eclipseSchemaFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(javaSchemaFile.toURI());
		// Ensure the file was found in the workspace
		if (eclipseSchemaFiles.length == 0) {
			return null;
		}
		return eclipseSchemaFiles[0];
	}

	private void showURL(File file, boolean forceExternal) {
		try {
			IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
			URL url = file.toURL();

			if (forceExternal) {
				IWebBrowser browser = support.getExternalBrowser();
				browser.openURL(url);
			} else {
				IWebBrowser browser = support.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.STATUS, file.getName(), fPointID, fPointID);
				browser.openURL(url);
			}
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}
