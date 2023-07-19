/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.views.APIToolingView;
import org.eclipse.pde.internal.core.util.PDEXmlProcessorFactory;

/**
 * Drop-down action to select the active session.
 */
@SuppressWarnings("restriction")
public class ExportSessionAction extends Action {
	private static final String DELTAS_XSLT_TRANSFORM_PATH = "/compare.xsl"; //$NON-NLS-1$
	private static final String XML_FILE_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String HTML_FILE_EXTENSION = ".html"; //$NON-NLS-1$
	APIToolingView view;

	public ExportSessionAction(APIToolingView view) {
		setText(ActionMessages.ExportSessionAction_label);
		setToolTipText(ActionMessages.ExportSessionAction_tooltip);
		ImageDescriptor enabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_ELCL_EXPORT);
		setImageDescriptor(enabledImageDescriptor);
		ImageDescriptor disabledImageDescriptor = ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_DLCL_EXPORT);
		setDisabledImageDescriptor(disabledImageDescriptor);
		setEnabled(false);
		this.view = view;
	}

	@Override
	public void run() {
		final ISession activeSession = ApiPlugin.getDefault().getSessionManager().getActiveSession();
		if (activeSession == null) {
			return;
		}
		ExportDialog dialog = new ExportDialog(view.getSite().getShell(), ActionMessages.ExportActionTitle);
		int returnCode = dialog.open();
		if (returnCode != Window.OK) {
			return;
		}
		final String reportFileName = dialog.getValue();
		if (reportFileName == null) {
			return;
		}
		final String lowerCase = reportFileName.toLowerCase();
		if (!lowerCase.endsWith(HTML_FILE_EXTENSION) && !lowerCase.endsWith(XML_FILE_EXTENSION)) {
			return;
		}

		Job job = new Job(ActionMessages.CompareWithAction_comparing_apis) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor progress = SubMonitor.convert(monitor, 100);
				progress.subTask(ActionMessages.CompareDialogCollectingElementTaskName);
				boolean isHtmlFile = lowerCase.endsWith(HTML_FILE_EXTENSION);
				File xmlOutputFile = null;
				progress.subTask(ActionMessages.CompareDialogComputeDeltasTaskName);
				File reportFile = new File(reportFileName);
				try {
					progress.split(25);
					BufferedWriter writer = null;
					try {
						if (isHtmlFile) {
							xmlOutputFile = Util.createTempFile(String.valueOf(System.currentTimeMillis()),
									XML_FILE_EXTENSION);
						} else {
							xmlOutputFile = reportFile;
						}
						if (xmlOutputFile.exists()) {
							xmlOutputFile.delete();
						} else {
							File parent = xmlOutputFile.getParentFile();
							if (!parent.exists() && !parent.mkdirs()) {
								return Status.error(ActionMessages.ExportSessionAction_failed_to_create_parent_folders);
							}
						}
						writer = new BufferedWriter(new FileWriter(xmlOutputFile));
						DeltaXmlVisitor visitor = new DeltaXmlVisitor();
						Object data = activeSession.getModel().getRoot().getData();
						if (data instanceof IDelta) {
							IDelta delta = (IDelta) data;
							progress.split(25);
							delta.accept(visitor);
							writer.write(visitor.getXML());
							writer.flush();
							progress.worked(25);
						}
					} catch (IOException | CoreException e) {
						ApiPlugin.log(e);
					} finally {
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException e) {
								// ignore
							}
						}
					}
					if (isHtmlFile) {
						// remaining part is to convert the xml file to html
						// using XSLT
						progress.split(10);
						Source xmlSource = new StreamSource(xmlOutputFile);
						InputStream stream = ApiPlugin.class.getResourceAsStream(DELTAS_XSLT_TRANSFORM_PATH);
						Source xsltSource = new StreamSource(stream);
						try {
							if (reportFile.exists()) {
								reportFile.delete();
							} else {
								File parent = reportFile.getParentFile();
								if (!parent.exists() && !parent.mkdirs()) {
									return Status.error(ActionMessages.ExportSessionAction_failed_to_create_parent_folders);
								}
							}
							writer = new BufferedWriter(new FileWriter(reportFile));
							Result result = new StreamResult(writer);
							TransformerFactory f = PDEXmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
							Transformer trans = f.newTransformer(xsltSource);
							trans.transform(xmlSource, result);
						} catch (TransformerException | IOException e) {
							ApiUIPlugin.log(e);
						} finally {
							if (writer != null) {
								try {
									writer.close();
								} catch (IOException e) {
									// ignore
								}
							}
						}
					}
					return Status.OK_STATUS;
				} catch (OperationCanceledException e) {
					// ignore
					if (xmlOutputFile != null && xmlOutputFile.exists()) {
						xmlOutputFile.delete();
					}
					if (reportFile.exists()) {
						reportFile.delete();
					}
				}
				return Status.CANCEL_STATUS;
			}
		};
		job.setSystem(false);
		job.setPriority(Job.LONG);
		job.schedule();
	}

	public void dispose() {
	}

}