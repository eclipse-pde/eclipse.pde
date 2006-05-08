/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;

public class BundleModelTestCase extends TestCase {

	public static Test suite() {
		return new TestSuite(BundleModelTestCase.class);
	}

	protected Document fDocument;
	protected BundleModel fModel;
	protected BundleTextChangeListener fListener;
	
	protected void setUp() throws Exception {
		fDocument = new Document();
	}
	
	protected void load() {
		load(false);
	}
	
	protected void load(boolean addListener) {
		try {
			fModel = new BundleModel(fDocument, false);
			fModel.load();
			if (addListener) {
				fListener = new BundleTextChangeListener(fModel.getDocument());
				fModel.addModelChangedListener(fListener);
			}
		} catch (CoreException e) {
			fail("model cannot be loaded");
		}
		
	}
}
