/*******************************************************************************
 *  Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.plugin;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * CSPluginHandler
 *
 */
public class AbbreviatedPluginHandler extends PluginHandler {

	private final String[] fExtensionPointIDs;

	/**
	 * @param extensionPointIDs
	 */
	public AbbreviatedPluginHandler(String[] extensionPointIDs) {
		super(true);

		fExtensionPointIDs = extensionPointIDs;
	}

	@Override
	protected boolean isInterestingExtension(Element element) {
		String point = element.getAttribute("point"); //$NON-NLS-1$
		for (String extensionPointId : fExtensionPointIDs) {
			if (point.equals(extensionPointId)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void characters(char[] characters, int start, int length) throws SAXException {
		processCharacters(characters, start, length);
	}

}
