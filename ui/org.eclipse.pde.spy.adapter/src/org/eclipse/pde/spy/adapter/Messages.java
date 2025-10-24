/*******************************************************************************
 * Copyright (c)  Lacherp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lacherp - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.adapter;

import org.eclipse.pde.spy.adapter.tools.AdapterHelper;
import org.eclipse.osgi.util.NLS;

/**
 * Messages constant class
 * @author pascal
 *
 */
public class Messages extends NLS {

	public static String rootSourceTypeTooltip;
	public static String rootDestinationTypeToolTip;
	public static String childSourceTypeToolTip;
	public static String childDestinationTypeToolTip;
	public static String adapterFactory;

	
	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(AdapterHelper.BUNDLE_ID+ ".messages.Messages", Messages.class);
	}
}
