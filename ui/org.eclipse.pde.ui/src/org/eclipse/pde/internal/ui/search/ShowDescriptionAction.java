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
package org.eclipse.pde.internal.ui.search;

import java.io.*;
import java.net.*;

import org.eclipse.help.browser.*;
import org.eclipse.help.internal.browser.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.program.*;


public class ShowDescriptionAction extends Action {
	private String pointId;
	private ISchema schema;
	private File previewFile;
	private URL cssURL;

	public ShowDescriptionAction(IPluginExtensionPoint point) {
		setExtensionPoint(point);
	}
	
	public ShowDescriptionAction(ISchema schema) {
		setSchema(schema);
	}
	
	public void setSchema(ISchema schema) {
		this.schema = schema;
		this.pointId = schema.getQualifiedPointId();
	}
	
	public void setExtensionPoint(IPluginExtensionPoint point) {
		this.pointId = point.getFullId();
		setText(PDEPlugin.getResourceString("ShowDescriptionAction.label")); //$NON-NLS-1$
		schema = null;
	}
	
	public URL getCSSURL(){
		return cssURL;
	}
	
	public void setCSSURL(String url){
		try {
			cssURL = new URL(url);
		} catch (MalformedURLException e) {
			PDE.logException(e);
		}
	}
	
	public void setCSSURL(URL url){
		cssURL = url;
	}
	
	public void run() {
		if (schema==null) {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			schema = registry.getSchema(pointId);
			if (schema==null) {
				showNoSchemaMessage();
				return;
			}
		}
		showSchemaDocument();
	}
	
	private void showNoSchemaMessage() {
		String title = PDEPlugin.getResourceString("ShowDescriptionAction.title"); //$NON-NLS-1$
		String message = PDEPlugin.getFormattedMessage("ShowDescriptionAction.noPoint.desc",pointId); //$NON-NLS-1$ //$NON-NLS-2$
		
		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}

	private void showSchemaDocument() {
		try {
			previewFile = getPreviewFile();
			if (previewFile == null)
				return;

			SchemaTransformer transformer = new SchemaTransformer();
			OutputStream os = new FileOutputStream(previewFile);
			PrintWriter printWriter = new PrintWriter(os, true);
			transformer.transform(printWriter, schema, cssURL, SchemaTransformer.TEMP); 
			os.flush();
			os.close();
			showURL(previewFile.getPath());
		} catch (Exception e) {
			PDEPlugin.logException(e);
		}
	}
	
	private File getPreviewFile(){
		try {
			File file = File.createTempFile("pde", ".html"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			return file;
		} catch (IOException e) {
		}
		return null;
	}
	
	private void showURL(String url) {
		boolean win32 = SWT.getPlatform().equals("win32"); //$NON-NLS-1$
		if (win32) {
			Program.launch(url);
		}
		else {
			IBrowser browser = BrowserManager.getInstance().createBrowser();
			try {
				browser.displayURL("file://" + url); //$NON-NLS-1$
			} catch (Exception e) {
			}
		}
	}
}
