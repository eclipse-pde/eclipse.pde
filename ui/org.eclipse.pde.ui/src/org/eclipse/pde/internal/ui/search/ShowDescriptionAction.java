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
package org.eclipse.pde.internal.ui.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.browser.*;
import org.eclipse.help.internal.browser.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;


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
		setText(PDEPlugin.getResourceString("ShowDescriptionAction.label"));
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
		String title = "Extension Point Description";
		String message = "Description for extension point \""+pointId+"\" cannot be found.";
		
		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}

	private void showSchemaDocument() {
		try {
			if (previewFile==null) {
				previewFile = getPreviewFile();
				if (previewFile == null)
					return;
			}

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
	
	private File getPreviewFile() throws CoreException {
		String prefix = "pde";
		String suffix = ".html";
		File file =
			PDECore.getDefault().getTempFileManager().createTempFile(
				this,
				prefix,
				suffix);
		return file;
	}
	
	private void showURL(String url) {
		boolean win32 = SWT.getPlatform().equals("win32");
		if (win32) {
			Program.launch(url);
		}
		else {
			IBrowser browser = BrowserManager.getInstance().createBrowser();
			try {
				browser.displayURL(url);
			} catch (Exception e) {
			}
		}
	}
}