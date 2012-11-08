/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.actions;

import java.io.File;
import java.net.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.schema.SchemaEditor;

/**
 * OpenSchemaAction
 * 
 */
public class OpenSchemaAction extends Action {

	private ISchema fSchema;

	private String fFullPointID;

	/**
	 * 
	 */
	public OpenSchemaAction() {
		fSchema = null;
		fFullPointID = null;

		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		setImageDescriptor(PDEPluginImages.DESC_SCHEMA_OBJ);
		setText(PDEUIMessages.HyperlinkActionOpenSchema);
		setToolTipText(PDEUIMessages.HyperlinkActionOpenSchema);
		setEnabled(false);
	}

	/**
	 * @param schema
	 */
	public void setInput(ISchema schema) {
		// Ensure schema is defined
		if (schema == null) {
			fFullPointID = PDEUIMessages.OpenSchemaAction_msgUnknown;
			return;
		}
		fFullPointID = schema.getQualifiedPointId();
		fSchema = schema;
	}

	/**
	 * @param point
	 */
	public void setInput(IPluginExtensionPoint point) {
		// Ensure the point is defined
		if (point == null) {
			fSchema = null;
			fFullPointID = PDEUIMessages.OpenSchemaAction_msgUnknown;
			return;
		}
		fFullPointID = point.getFullId();
		// Ensure the point is fully qualified
		if (fFullPointID.indexOf('.') == -1) {
			fSchema = null;
			return;
		}
		// Find the schema
		fSchema = findSchema(point);
	}

	/**
	 * @param fullPointID
	 */
	public void setInput(String fullPointID) {
		// Ensure point ID is defined
		if (fullPointID == null) {
			fSchema = null;
			fFullPointID = PDEUIMessages.OpenSchemaAction_msgUnknown;
			return;
		}
		fFullPointID = fullPointID;
		// Find the corresponding extension point
		IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(fFullPointID);
		// Ensure the extension point is defined
		if (point == null) {
			fSchema = null;
			return;
		}
		// Find the schema
		fSchema = findSchema(point);
	}

	/**
	 * @param extension
	 */
	public void setInput(IPluginExtension extension) {
		// Ensure the extension is defined
		if (extension == null) {
			fSchema = null;
			fFullPointID = PDEUIMessages.OpenSchemaAction_msgUnknown;
			return;
		}
		// Get the full extension point ID
		fFullPointID = extension.getPoint();
		// Find the corresponding extension point
		IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(fFullPointID);
		// Ensure the extension point is defined
		if (point == null) {
			fSchema = null;
			return;
		}
		// Find the schema
		fSchema = findSchema(point);
	}

	private ISchema findSchema(IPluginExtensionPoint point) {
		// Find the corresponding schema URL for the extension point
		URL url = SchemaRegistry.getSchemaURL(point);
		// Ensure the URL is defined
		if (url == null) {
			return null;
		}
		// Create a schema descriptor
		ISchemaDescriptor descriptor = new SchemaDescriptor(fFullPointID, url);
		// Get the schema
		ISchema schema = descriptor.getSchema(false);
		// Ensure schema is defined
		if (schema == null) {
			return null;
		}
		return schema;
	}

	/**
	 * @param fullPointID
	 */
	private void displayErrorDialog() {
		String title = PDEUIMessages.OpenSchemaAction_titleExtensionPointSchema;
		String message = NLS.bind(PDEUIMessages.OpenSchemaAction_errorMsgSchemaNotFound, fFullPointID);
		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		// Ensure the schema is defined
		if (fSchema == null) {
			displayErrorDialog();
			return;
		}

		// Get unencoded schema url
		URL schemaURL = fSchema.getURL();
		if (schemaURL == null) {
			displayErrorDialog();
			return;
		}

		// Check if we are dealing with a jarred bundle
		if (schemaURL.getProtocol().startsWith("jar")) { //$NON-NLS-1$
			openSchemaJar(schemaURL);
		} else {
			openSchemaFile(schemaURL);
		}
	}

	/**
	 * @param path
	 */
	private void openSchemaFile(URL url) {
		try {
			// Convert url to an encoded URI, then try to get a local file out of it
			URI uri = URIUtil.toURI(url);
			File schemaFile = URIUtil.toFile(uri);
			if (schemaFile == null || !schemaFile.exists()) {
				displayErrorDialog();
				return;
			}

			// See if the file is actually in the workspace so we can open the editable version
			IPath schemaPath = new Path(schemaFile.getPath());
			IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
			IPath workspacePath = root.getLocation();
			if (workspacePath.isPrefixOf(schemaPath)) {
				schemaPath = schemaPath.removeFirstSegments(workspacePath.segmentCount());
				IResource res = root.findMember(schemaPath);
				if (res != null && res instanceof IFile && res.getProject().isOpen()) {
					SchemaEditor.openSchema((IFile) res);
					return;
				}
			}

			// Not in the workspace, open as absolute path
			SchemaEditor.openSchema(schemaFile);

		} catch (URISyntaxException e) {
			PDEPlugin.log(e);
			displayErrorDialog();
		}
	}

	/**
	 * @param path
	 */
	private void openSchemaJar(URL url) {
		try {
			// The url is unencoded, so we can treat it like a path, splitting it based on the jar suffix '!'
			String stringUrl = url.getPath();
			int jarSuffix = stringUrl.indexOf('!');
			if ((jarSuffix <= 0) || ((jarSuffix + 1) >= stringUrl.length())) {
				displayErrorDialog();
				return;
			}

			String fileUrl = stringUrl.substring(0, jarSuffix);
			URI uri = URIUtil.toURI(new URL(fileUrl));
			File jarFile = URIUtil.toFile(uri);
			if (jarFile == null || !jarFile.exists()) {
				displayErrorDialog();
				return;
			}

			String schemaEntryName = stringUrl.substring(jarSuffix + 1);
			if (schemaEntryName.startsWith("/")) { //$NON-NLS-1$
				schemaEntryName = schemaEntryName.substring(1);
			}

			// Open the schema in a new editor
			if (!SchemaEditor.openSchema(jarFile, schemaEntryName)) {
				displayErrorDialog();
			}
		} catch (URISyntaxException e) {
			PDEPlugin.log(e);
			displayErrorDialog();
		} catch (MalformedURLException e) {
			PDEPlugin.log(e);
			displayErrorDialog();
		}
	}

}
