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
package org.eclipse.pde.internal.ui.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;


public class ShowDescriptionAction extends Action {
	private String fPointID;
	private ISchema fSchema;
	private File fPreviewFile;
	private boolean forceExternal;
	
	public ShowDescriptionAction(String pointID) {
		fPointID = pointID;
	}

	public ShowDescriptionAction(IPluginExtensionPoint point) {
		this(point, false);
	}
	
	public ShowDescriptionAction(IPluginExtensionPoint point, boolean forceExternal) {
		setExtensionPoint(point);
		this.forceExternal = forceExternal;
	}
	
	public ShowDescriptionAction(ISchema schema) {
		setSchema(schema);
	}
	
	public void setSchema(ISchema schema) {
		fSchema = schema;
		fPointID = schema.getQualifiedPointId();
	}
	
	public void setExtensionPoint(IPluginExtensionPoint point) {
		fPointID = point.getFullId();
		setText(PDEUIMessages.ShowDescriptionAction_label); 
		fSchema = null;
	}
	
	public void run() {
		if (fSchema == null) {
			IPluginExtensionPoint point = PDECore.getDefault().findExtensionPoint(fPointID);
			URL url = null;
			if (point != null) {
				url = SchemaRegistry.getSchemaURL(point);
				if (url != null) {
					ISchemaDescriptor desc = new SchemaDescriptor(fPointID, url);
					fSchema = desc.getSchema(false);
				}
			}
			if (point == null|| url == null || fSchema == null) {
				showNoSchemaMessage();
				return;
			}
		} 
		showSchemaDocument();
	}
	
	private void showNoSchemaMessage() {
		String title = PDEUIMessages.ShowDescriptionAction_title; 
		String message = NLS.bind(PDEUIMessages.ShowDescriptionAction_noPoint_desc, fPointID); // 
		MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
	}

	private void showSchemaDocument() {
		try {
			fPreviewFile = getPreviewFile();
			if (fPreviewFile == null)
				return;

			SchemaTransformer transformer = new SchemaTransformer();
			OutputStream os = new FileOutputStream(fPreviewFile);
			PrintWriter printWriter = new PrintWriter(os, true);
			transformer.transform(fSchema, printWriter); 
			os.flush();
			os.close();
			showURL(fPreviewFile, forceExternal);
		} catch (IOException e) {
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
	
	private void showURL(File file, boolean forceExternal) {
		try {
			IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
			URL url = file.toURL();
			
			if (forceExternal) {
				IWebBrowser browser = support.getExternalBrowser();
				browser.openURL(url); 
			} else {
				IWebBrowser browser = support.createBrowser(
						IWorkbenchBrowserSupport.AS_EDITOR
								| IWorkbenchBrowserSupport.STATUS,
						"org.eclipse.pde", fPointID, fPointID); //$NON-NLS-1$
				browser.openURL(url); 
			}
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		}
		catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}