/*******************************************************************************
 * Copyright (c) 2020, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Peter Kriens <peter.kriens@aqute.biz> - initial API and implementation
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.internal;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.service.clipboard.Clipboard;

@Component(service = Clipboard.class)
public class SWTClipboard implements Clipboard {
	private static final Transfer[] TEXT_TRANSFER = new Transfer[] {
		TextTransfer.getInstance()
	};

	@Override
	public <T> boolean copy(T content) {
		Display d = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
		AtomicBoolean ok = new AtomicBoolean();
		d.syncExec(() -> {
			final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(d);
			if (content instanceof String) {
				cb.setContents(new Object[] {
					content
				}, TEXT_TRANSFER);
				ok.set(true);
			}
		});
		return ok.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> paste(Class<T> type) {
		Display d = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
		AtomicReference<Optional<T>> ok = new AtomicReference<>(Optional.empty());
		d.syncExec(() -> {
			final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(null);
			if (type == String.class) {
				String data = (String) cb.getContents(TEXT_TRANSFER[0]);
				ok.set((Optional<T>) Optional.ofNullable(data));
			}
		});
		return ok.get();
	}
}
