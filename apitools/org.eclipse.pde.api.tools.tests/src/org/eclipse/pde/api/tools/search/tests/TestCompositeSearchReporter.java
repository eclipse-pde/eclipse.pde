/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.search.tests;

import java.util.ArrayList;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;

/**
 * Test implementation of a search reporter that delegates to two
 * reporters: The {@link TestReporter} and the {@link XMLApiSearchReporter}
 *
 * <p>The {@link TestReporter} is always called first to validate we are getting the references / skipped
 * components that we are expecting to see</p>
 *
 * @since 1.0.1
 */
public class TestCompositeSearchReporter implements IApiSearchReporter {

	private SearchTest test = null;
	ArrayList<IApiSearchReporter> reporters = new ArrayList<>(2);
	int testreporteridx = 0;

	public TestCompositeSearchReporter(SearchTest test, IApiSearchReporter[] reporters) {
		this.test = test;
		if(reporters != null) {
			for (int i = 0; i < reporters.length; i++) {
				if(!this.reporters.contains(reporters[i])) {
					if(reporters[i] instanceof TestReporter){
						testreporteridx = i;
					}
					this.reporters.add(reporters[i]);
				}
			}
		}
		else {
			this.test.reportFailure("you must specify IApiSearchReporters"); //$NON-NLS-1$
		}
	}

	@Override
	public void reportNotSearched(IApiElement[] elements) {
		if(this.testreporteridx > this.reporters.size()) {
			this.test.reportFailure("the index for the TestReporter does not exist"); //$NON-NLS-1$
		}
		IApiSearchReporter reporter = this.reporters.get(testreporteridx);
		reporter.reportNotSearched(elements);
		for (int i = 0; i < this.reporters.size(); i++) {
			if(i == this.testreporteridx) {
				continue;
			}
			this.reporters.get(i).reportNotSearched(elements);
		}
	}

	@Override
	public void reportResults(IApiElement element, IReference[] references) {
		if(this.testreporteridx > this.reporters.size()) {
			this.test.reportFailure("the index for the TestReporter does not exist"); //$NON-NLS-1$
		}
		IApiSearchReporter reporter = this.reporters.get(testreporteridx);
		reporter.reportResults(element, references);
		for (int i = 0; i < this.reporters.size(); i++) {
			if(i == this.testreporteridx) {
				continue;
			}
			this.reporters.get(i).reportResults(element, references);
		}
	}

	@Override
	public void reportMetadata(IMetadata data) {
	}

	@Override
	public void reportCounts() {
	}
}
