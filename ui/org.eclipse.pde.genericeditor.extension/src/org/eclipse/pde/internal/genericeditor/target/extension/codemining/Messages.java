/********************************************************************************
 * Copyright (c) 2018 vogella GmbH and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel (vogella GmbH) - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 534758
 ********************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.codemining;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.genericeditor.target.extension.codemining.messages"; //$NON-NLS-1$
	public static String TargetDefinitionActivationCodeMining_e_location_outside_lfs;
	public static String TargetDefinitionCodeMiningProvider_e_format_invalid;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
