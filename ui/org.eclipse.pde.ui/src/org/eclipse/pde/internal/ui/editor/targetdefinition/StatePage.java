/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.target.StateTree;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormPage;
import org.osgi.framework.FrameworkUtil;

public class StatePage extends FormPage {

	public static final String PAGE_ID = "state"; //$NON-NLS-1$

	private State state;

	private StateTree stateTree;

	private ITargetDefinition target;

	private Job job;

	private boolean active;

	public StatePage(TargetEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.StatePage_title);
	}

	@Override
	public void createPartControl(Composite parent) {
		stateTree = new StateTree(parent);
		stateTree.setInput(state);
	}

	@Override
	public Control getPartControl() {
		return stateTree;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
		super.setActive(active);
		loadState();
	}

	private void loadState() {
		if (!active || state != null || target == null) {
			return;
		}
		TargetBundle[] targetBundles = target.getBundles();
		if (targetBundles == null || targetBundles.length == 0) {
			return;
		}
		job = Job.create("Compute Target State", monitor -> { //$NON-NLS-1$
			try {
				State targetState = BundleHelper.getPlatformAdmin().getFactory().createState(true);
				targetState.setPlatformProperties(
						PDECore.getDefault().getModelManager().getState().getState().getPlatformProperties());
				StateObjectFactory factory = targetState.getFactory();
				long id = 1;
				for (TargetBundle targetBundle : targetBundles) {
					if (targetBundle.isSourceBundle()) {
						continue;
					}
					BundleInfo bundleInfo = targetBundle.getBundleInfo();
					String manifest = bundleInfo.getManifest();
					if (manifest != null) {
						Map<String, String> bundleManifest;
						try (ByteArrayInputStream stream = new ByteArrayInputStream(
								manifest.getBytes(StandardCharsets.UTF_8))) {
							bundleManifest = ManifestElement.parseBundleManifest(stream);
						}
						BundleDescription bundleDescription = factory.createBundleDescription(targetState,
								FrameworkUtil.asDictionary(bundleManifest),
								String.valueOf(targetBundle.getBundleInfo().getLocation()), id++);
						targetState.addBundle(bundleDescription);
					}
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
				}
				targetState.resolve(false);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				PlatformUI.getWorkbench().getDisplay().execute(() -> {
					state = targetState;
					if (stateTree != null) {
						stateTree.setInput(targetState);
					}
				});
			} catch (Exception e) {
				ILog.get().error("Computing target state failed!", e); //$NON-NLS-1$
			}
			return Status.OK_STATUS;
		});
		job.schedule();
	}

	public void reset() {
		PlatformUI.getWorkbench().getDisplay().execute(() -> {
			update(null);
			if (stateTree != null) {
				stateTree.setInput(null);
			}
		});
	}

	protected void update(ITargetDefinition target) {
		if (job != null) {
			job.cancel();
			job = null;
		}
		this.state = null;
		this.target = target;
	}

	public void updateTarget(ITargetDefinition target) {
		PlatformUI.getWorkbench().getDisplay().execute(() -> {
			update(target);
			loadState();
		});
	}
}
