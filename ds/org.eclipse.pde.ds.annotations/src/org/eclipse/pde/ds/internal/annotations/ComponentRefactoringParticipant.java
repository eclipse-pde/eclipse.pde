/*******************************************************************************
 * Copyright (c) 2015, 2016 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;

/**
 * Common protocol for DS {@link RefactoringParticipant refactoring participants}
 * since they need to extend different classes.
 */
public interface ComponentRefactoringParticipant {

	String getComponentNameRoot(IJavaElement renamedElement, RefactoringArguments args);
}
