/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.wait.AbstractWait;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.core.resources.DefaultProject;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.problems.Problem;
import org.eclipse.reddeer.eclipse.ui.views.log.LogMessage;
import org.eclipse.reddeer.eclipse.ui.views.log.LogView;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView;
import org.eclipse.reddeer.eclipse.ui.views.markers.ProblemsView.ProblemType;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.eclipse.ui.tests.smartimport.plugins.ImportedProject;
import org.eclipse.ui.tests.smartimport.plugins.ProjectProposal;
import org.eclipse.ui.tests.smartimport.plugins.SmartImportRootWizardPage;
import org.eclipse.ui.tests.smartimport.plugins.SmartImportWizard;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class ProjectTestTemplate {

	public static final String IGNORED_ERRORS_PROPERTY = "ignored.errors.regexp";
	private static final Logger LOG = new Logger(ProjectTestTemplate.class);

	private String ignoredErrorsRegExp = "Current Eclipse instance does not support software installation.";

	public ProjectTestTemplate() {
		String extraPattern = System.getProperty(IGNORED_ERRORS_PROPERTY);

		if (extraPattern != null && !extraPattern.isEmpty()) {
			ignoredErrorsRegExp = ignoredErrorsRegExp + "|" + extraPattern;
		}
	}

	@After
	public void cleanup() {
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		for (Project p : getProjects()) {
			p.delete(false);
		}
		// empty error log

		LogView logView = new LogView();
		logView.open();
		logView.deleteLog();
	}

	private List<DefaultProject> getProjects() {
		try {
			return new ProjectExplorer().getProjects();
		} catch (NullPointerException e) {
			// TODO: remove workaround when
			// https://github.com/eclipse/reddeer/issues/2005 is fixed
			LOG.error("https://github.com/eclipse/reddeer/issues/2005", e);
			return emptyList();
		}
	}

	@BeforeClass
	public static void setupClass() {
		AbstractWait.sleep(TimePeriod.DEFAULT);
		LogView logView = new LogView();
		logView.open();
		logView.deleteLog();
	}

	@Test
	public void testImport() {
		SmartImportWizard easymportWizard = new SmartImportWizard();
		easymportWizard.open();
		SmartImportRootWizardPage selectImportRootWizardPage = new SmartImportRootWizardPage(easymportWizard);
		String path = getProjectPath().getAbsolutePath();
		selectImportRootWizardPage.selectDirectory(path);
		selectImportRootWizardPage.setSearchForNestedProjects(true);
		selectImportRootWizardPage.setDetectAndConfigureNatures(true);
		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.LONG);

		// check proposals
		List<ProjectProposal> allProjectProposals = selectImportRootWizardPage.getAllProjectProposals();
		List<ProjectProposal> expectedProposals = getExpectedProposals();
		assertEquals(expectedProposals.size(), allProjectProposals.size());
		for (ProjectProposal projectProposal : allProjectProposals) {
			if (!expectedProposals.contains(projectProposal)) {
				fail("Expected proposals: " + expectedProposals.toString() + ", actual proposals: "
						+ allProjectProposals.toString());
			}
		}
		easymportWizard.finish();

		// check imported project
		checkErrorLog();
		checkProblemsView();

		checkImportedProject();
	}

	private void checkErrorLog() {
		LogView logView = new LogView();
		logView.open();
		List<LogMessage> errorMessages = logView.getErrorMessages();

		RegexMatcher matcher = new RegexMatcher(ignoredErrorsRegExp);
		int ignoredErrors = 0;

		Iterator<LogMessage> iterator = errorMessages.iterator();

		while (iterator.hasNext()) {
			LogMessage logMessage = iterator.next();

			if (matcher.matches(logMessage.getMessage())) {
				LOG.info("Ignoring error message: " + logMessage.getMessage());
				iterator.remove();
				// Increase exceptedErrors if log contains error which can be
				// ignored.
				ignoredErrors++;
			}
		}

		assertTrue("There are unexpected errors in error log: " + errorMessages,
				((errorMessages.size() - ignoredErrors) <= 0));
	}

	private void checkProblemsView() {
		ProblemsView problemsView = new ProblemsView();
		problemsView.open();
		List<Problem> problems = problemsView.getProblems(ProblemType.ERROR);
		assertEquals("There should be no errors in imported project", 0, problems.size());

	}

	abstract File getProjectPath();

	abstract List<ProjectProposal> getExpectedProposals();

	abstract List<ImportedProject> getExpectedImportedProjects();

	/**
	 * Checks whether the project was imported correctly.
	 *
	 */
	abstract void checkImportedProject();

}
