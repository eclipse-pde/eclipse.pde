/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.pde.internal.runtime.spy.dialogs.SpyDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.Bundle;

// TODO create a SpyFormToolkit that extends FormToolkit and replace it with this
public class SpyBuilder {

	private class SpyHyperlinkAdapter extends HyperlinkAdapter {
		
		private SpyDialog dialog;
		
		public SpyHyperlinkAdapter(SpyDialog dialog) {
			this.dialog = dialog;
		}

		public void linkActivated(HyperlinkEvent e) {
			String clazz = (String) e.getHref();
			Bundle bundle = (Bundle) bundleClassByName.get(clazz);
			SpyIDEUtil.openClass(bundle.getSymbolicName(), clazz);
			dialog.close();
		}
	}
	
	private Map bundleClassByName = new HashMap();
	private SpyDialog dialog;
	private FormToolkit toolkit;
	
	public SpyBuilder(SpyDialog dialog) {
		this.dialog = dialog;
		this.toolkit = new FormToolkit(Display.getDefault());
	}
	
	public String generateInterfaceString(Class clazz) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<li bindent=\"20\" style=\"image\" value=\"interface\">"); //$NON-NLS-1$
		generateClassHref(buffer, clazz);
		buffer.append("</li>"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	// TODO refactor this to take an array of classes
	public String generateClassString(String title, Class clazz) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<p>"); //$NON-NLS-1$
		buffer.append(title);
		buffer.append("</p>"); //$NON-NLS-1$
		buffer.append(generateClassString(clazz));
		return buffer.toString();
	}

	public String generateClassString(Class clazz) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<li bindent=\"20\" style=\"image\" value=\"class\">"); //$NON-NLS-1$
		generateClassHref(buffer, clazz);
	    buffer.append("</li>"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	private void generateClassHref(StringBuffer buffer, Class clazz) {
		Bundle bundle = 
			PDERuntimePlugin.HAS_IDE_BUNDLES ? PDERuntimePlugin.getDefault().getPackageAdmin().getBundle(clazz) : null;
			if (bundle != null) {
				bundleClassByName.put(clazz.getName(),
						bundle);
				buffer.append("<a href=\"").append( //$NON-NLS-1$
						clazz.getName()).append("\">") //$NON-NLS-1$
						.append(getSimpleName(clazz)).append(
						"</a>"); //$NON-NLS-1$
			} else {
				buffer.append(clazz.getName());
			}
	}
	
	public void generatePluginDetailsText(Bundle bundle, String objectId, String objectType, StringBuffer buffer, FormText text) {
		if (bundle != null) {
			String version = (String) (bundle.getHeaders()
					.get(org.osgi.framework.Constants.BUNDLE_VERSION));
			
			buffer.append("<p>"); //$NON-NLS-1$
			buffer.append(PDERuntimeMessages.SpyDialog_contributingPluginId_title);
			buffer.append("</p>"); //$NON-NLS-1$
			buffer.append("<li bindent=\"20\" style=\"image\" value=\"plugin\">"); //$NON-NLS-1$
			buffer.append(bundle.getSymbolicName());
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(version);
			buffer.append(")"); //$NON-NLS-1$
			buffer.append("</li>"); //$NON-NLS-1$
	
			Image pluginImage = PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_PLUGIN_OBJ);
			text.setImage("plugin", pluginImage); //$NON-NLS-1$
	
			if (objectId != null) {
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(NLS.bind(PDERuntimeMessages.SpyDialog_contributingPluginId_desc, objectType));
				buffer.append("</p>"); //$NON-NLS-1$
				buffer.append("<li bindent=\"20\" style=\"image\" value=\"id\">"); //$NON-NLS-1$
				buffer.append(objectId);
				buffer.append("</li>"); //$NON-NLS-1$
			}
		}
	}
	
	public HyperlinkAdapter createHyperlinkAdapter() {
		return new SpyHyperlinkAdapter(dialog);
	}
	
	public FormToolkit getFormToolkit() {
		return toolkit;
	}
	
	public void dispose() {
		if (toolkit != null) {
			if (toolkit.getColors() != null) {
				toolkit.dispose();
			}
		}
	}

	private String getSimpleName(Class clazz) {
		String fullName = clazz.getName();
		int index = fullName.lastIndexOf('.');
		String name = fullName.substring(index + 1, fullName.length());
		if(name != null)
			return name;
		return fullName;
	}
	
}
