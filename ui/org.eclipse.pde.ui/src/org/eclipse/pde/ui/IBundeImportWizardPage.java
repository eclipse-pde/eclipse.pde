/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.importing.BundleImportDescription;

/**
 * A wizard page contributed by a <code>org.eclipse.pde.ui.bundleImportPages</code>
 * extension.
 * 
 * @since 3.6
 *
 */
public interface IBundeImportWizardPage extends IWizardPage {
	/**
	 * Called when the import wizard is closed by selecting 
	 * the finish button.
	 * Implementers may store the page result (new/changed bundle
	 * import descriptions in getSelection) here.
	 * 
	 * @return if the operation was successful. The wizard will only close
	 * when <code>true</code> is returned.
	 */
	public boolean finish();

	/**
	 * Returns the bundle import descriptions edited or created on the page 
	 * after the wizard has closed.
	 * Returns bundle import descriptions initially set using 
	 * <code>setSelection</code>if the wizard has not been 
	 * closed yet.
	 * 
	 * @return the bundle import descriptions edited or created on the page.
	 */
	public BundleImportDescription[] getSelection();

	/**
	 * Sets the bundle import descriptions to be edited on the page.
	 * The passed descriptions can be edited and should be 
	 * returned in getSelection().
	 * 
	 * @param descriptions the bundle import descriptions edited on the page.
	 */
	public void setSelection(BundleImportDescription[] descriptions);
}
