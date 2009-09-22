/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.StringTokenizer;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionPointNode;

public class IdUtil {
	public static boolean isValidCompositeID(String name) {
		if (name.length() <= 0) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c) && (c < '0' || '9' < c) && c != '_') {
				if (i == 0 || i == name.length() - 1 || c != '.') {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isValidCompositeID3_0(String name) {
		if (name.length() <= 0) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c) && (c < '0' || '9' < c) && c != '_' && c != '-') {
				if (i == 0 || i == name.length() - 1 || c != '.') {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isValidSimpleID(String name) {
		if (name.length() <= 0) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c) && (c < '0' || '9' < c) && c != '_') {
				return false;
			}
		}
		return true;
	}

	public static String getValidId(String projectName) {
		return projectName.replaceAll("[^a-zA-Z0-9\\._-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * nameFieldQualifier must contain a placeholder variable
	 * ie. {0} Plug-in
	 */
	public static String getValidName(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens()) {
				String name = Character.toUpperCase(token.charAt(0)) + ((token.length() > 1) ? token.substring(1) : ""); //$NON-NLS-1$
				return name;
			}
		}
		return ""; //$NON-NLS-1$
	}

	public static String getValidProvider(String id) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		int count = tok.countTokens();
		if (count > 2 && tok.nextToken().equals("com")) //$NON-NLS-1$
			return tok.nextToken().toUpperCase();
		return ""; //$NON-NLS-1$
	}

	public static String getFullId(IPluginExtension extension) {
		String id = extension.getId();
		IPluginBase plugin = extension.getPluginBase();
		String schemaVersion = plugin.getSchemaVersion();
		if (schemaVersion != null && Double.parseDouble(schemaVersion) >= 3.2) {
			if (id.indexOf('.') > 0)
				return id;
		}

		if (plugin instanceof IFragment)
			return ((IFragment) plugin).getPluginId() + '.' + id;
		return plugin.getId() + '.' + id;
	}

	/**
	 * @param point
	 * @param fModel
	 */
	public static String getFullId(IPluginExtensionPoint point, IPluginModelBase model) {

		if ((point instanceof PluginExtensionPointNode) && (model != null)) {
			String pointId = point.getId();
			String schemaVersion = model.getPluginBase().getSchemaVersion();
			if (schemaVersion != null && Double.parseDouble(schemaVersion) >= 3.2 && pointId.indexOf('.') > 0) {
				return pointId;
			}
			String id = null;
			if (model instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) model).getFragment();
				if (fragment != null)
					id = fragment.getPluginId();
			}
			if (id == null)
				id = model.getPluginBase().getId();
			return id + '.' + pointId;
		}
		return point.getFullId();
	}

	public static boolean isInterestingExtensionPoint(String point) {
		return "org.eclipse.pde.core.source".equals(point) //$NON-NLS-1$
				|| "org.eclipse.core.runtime.products".equals(point) //$NON-NLS-1$
				|| "org.eclipse.pde.core.javadoc".equals(point) //$NON-NLS-1$
				|| "org.eclipse.ui.intro".equals(point); //$NON-NLS-1$
	}
}
