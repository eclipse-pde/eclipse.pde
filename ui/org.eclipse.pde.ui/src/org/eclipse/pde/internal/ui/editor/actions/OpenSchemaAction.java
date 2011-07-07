/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
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
import java.io.UnsupportedEncodingException;
import java.net.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
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
		// Retrieve the schema URL
		URL schemaURL = fSchema.getURL();
		// Ensure the URL is defined
		if (schemaURL == null) {
			displayErrorDialog();
			return;
		}
		// Get the raw URL, determine if it is stored in a JAR, and handle 
		// accordingly
		String rawURL = schemaURL.toString();
		String path = null;
		try {
			path = URLDecoder.decode(schemaURL.getPath(), "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
		}
		if (path != null) {
			if (rawURL.startsWith("jar")) { //$NON-NLS-1$
				// Call to getPath removes the 'jar:' qualifier
				openSchemaJar(path);
			} else {
				openSchemaFile(path);
			}
		} else {
			displayErrorDialog();
		}

	}

	/**
	 * @param path
	 */
	private void openSchemaFile(String path) {
		// Open the schema in a new editor
		try {
			// see if schema URL is actually in workspace.  If so, open it as we would if users opened file directly
			IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
			IPath workspacePath = root.getLocation();
			String workspaceLoc = workspacePath.toFile().toURL().getPath();
			if (path.startsWith(workspaceLoc)) {
				String relativeLocation = path.substring(workspaceLoc.length());
				IResource res = root.findMember(relativeLocation);
				if (res != null && res instanceof IFile && res.getProject().isOpen()) {
					SchemaEditor.openSchema((IFile) res);
					return;
				}
			}
		} catch (MalformedURLException e) {
		}
		if (!SchemaEditor.openSchema(new File(path)))
			displayErrorDialog();
	}

	/**
	 * @param path
	 */
	private void openSchemaJar(String path) {
		// Remove the 'file:' qualifier
		if (path.startsWith("file:") == false) { //$NON-NLS-1$
			displayErrorDialog();
			return;
		}
		path = path.substring(5);
		// An exclaimation point separates the jar filename from the
		// schema file entry in the jar file
		// Get the index of the '!'
		int exclPointIndex = path.indexOf('!');
		// Ensure there is an '!' and that the schema file entry is defined
		// and the jar file name is defined
		if ((exclPointIndex <= 0) || ((exclPointIndex + 1) >= path.length())) {
			displayErrorDialog();
			return;
		}
		// Extract the jar file name - not including '!'
		String jarFileName = path.substring(0, exclPointIndex);
		// Extract the schema entry name - not including the '!' 
		String schemaEntryName = path.substring(exclPointIndex + 1);
		// If the schema entry starts with a '/', remove it
		if (schemaEntryName.startsWith("/")) { //$NON-NLS-1$
			schemaEntryName = schemaEntryName.substring(1);
		}
		// Open the schema in a new editor
		if (!SchemaEditor.openSchema(new File(jarFileName), schemaEntryName))
			displayErrorDialog();
	}

}
