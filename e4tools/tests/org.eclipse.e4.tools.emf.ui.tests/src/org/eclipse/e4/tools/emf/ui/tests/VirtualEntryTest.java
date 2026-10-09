/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.e4.tools.emf.ui.internal.E4Properties;
import org.eclipse.e4.tools.emf.ui.internal.common.VirtualEntry;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;

public class VirtualEntryTest {

	@Test
	public void testDisposingListReleasesModelListener() {
		Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), () -> {
			final MPart part = MBasicFactory.INSTANCE.createPart();
			final int adapters = ((EObject) part).eAdapters().size();

			final VirtualEntry<MPart, ?> entry = new VirtualEntry<>("id", E4Properties.handlers(), part, "label"); //$NON-NLS-1$ //$NON-NLS-2$
			part.getHandlers().add(MCommandsFactory.INSTANCE.createHandler());
			assertEquals(1, entry.getList().size());
			assertTrue(((EObject) part).eAdapters().size() > adapters);

			entry.getList().dispose();

			assertEquals(adapters, ((EObject) part).eAdapters().size());
		});
	}
}
