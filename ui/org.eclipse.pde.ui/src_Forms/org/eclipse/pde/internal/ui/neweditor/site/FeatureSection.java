/*
 * Created on Sep 29, 2003
 */
package org.eclipse.pde.internal.ui.neweditor.site;
import java.lang.reflect.*;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.build.BuildSiteOperation;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.TableSection;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author melhem
 */
public class FeatureSection extends TableSection {
	private TableViewer fFeaturesViewer;
	private ISiteModel fModel;
	private ISiteBuildModel fBuildModel;
	private TablePart fFeaturesTablePart;
	class FeatureContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			ISiteBuildModel model = (ISiteBuildModel) inputElement;
			return model.getSiteBuild().getFeatures();
		}
	}
	public FeatureSection(PDEFormPage formPage, Composite parent) {
		super(formPage, 
				parent,
				Section.DESCRIPTION,
				new String[] {PDEPlugin.getResourceString("SiteEditor.add"),
				PDEPlugin.getResourceString("SiteEditor.buildAll")});
		getSection().setText(PDEPlugin
				.getResourceString("SiteEditor.FeatureSection.header")); //$NON-NLS-1$
		getSection().setDescription(PDEPlugin
				.getResourceString("SiteEditor.FeatureSection.desc")); //$NON-NLS-1$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	public void dispose() {
		super.dispose();
		fModel.removeModelChangedListener(this);
		if (fBuildModel != null)
			fBuildModel.removeModelChangedListener(this);
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		fModel = (ISiteModel) getPage().getModel();
		fModel.addModelChangedListener(this);
		fBuildModel = fModel.getBuildModel();
		if (fBuildModel != null)
			fBuildModel.addModelChangedListener(this);		
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fFeaturesTablePart = getTablePart();

		fFeaturesViewer = fFeaturesTablePart.getTableViewer();
		fFeaturesViewer.setContentProvider(new FeatureContentProvider());
		fFeaturesViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fFeaturesViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				ISiteBuildFeature f1 = (ISiteBuildFeature) e1;
				ISiteBuildFeature f2 = (ISiteBuildFeature) e2;
				int compare = super.compare(viewer, f1.getId(), f2.getId());
				return (compare == 0) ? super.compare(viewer, f1.getVersion(),
						f2.getVersion()) : compare;
			}
		});
		
		// add drag support
		Transfer[] transfers = new Transfer[] { ModelDataTransfer.getInstance()};
		fFeaturesViewer.addDragSupport(DND.DROP_LINK, transfers,
				new DragSourceListener() {
					public void dragStart(DragSourceEvent event) {
						ISelection selection = fFeaturesViewer.getSelection();
						if (selection == null || selection.isEmpty()) {
							event.doit = false;
						}
					}
					public void dragSetData(DragSourceEvent event) {
						IStructuredSelection ssel = (IStructuredSelection)fFeaturesViewer.getSelection();
						event.data = ssel.toArray();
					}
					public void dragFinished(DragSourceEvent event) {
					}
				});

		fFeaturesViewer.setInput(fBuildModel);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		refresh();
	}

	public void fillContextMenu(IMenuManager manager) {
		final ISelection selection = fFeaturesViewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			manager.add(new Action(PDEPlugin.getResourceString("SiteEditor.publish")) {
				public void run() {
					Object[]selected = ((IStructuredSelection)selection).toArray();
					for (int i = 0; i < selected.length; i++) {
						ISiteBuildFeature sbFeature = (ISiteBuildFeature)selected[i];
						ISiteFeature feature = findMatchingSiteFeature(fModel, sbFeature);
						try {
							if (feature == null)
								fModel.getSite().addFeatures(new ISiteFeature[]{createSiteFeature(fModel, sbFeature)});
						} catch (CoreException e) {
						}
					}
				}
			});
			manager.add(new Action(PDEPlugin.getResourceString("SiteEditor.build")) {
				public void run() {
					List list = ((IStructuredSelection)selection).toList();
					handleBuild((ISiteBuildFeature[])list.toArray(new ISiteBuildFeature[list.size()]));
				}
			});
			manager.add(new Separator());
			manager.add(new Action(PDEPlugin.getResourceString("SiteEditor.remove")) {
				public void run() {
					doGlobalAction(ActionFactory.DELETE.getId());
				}
			});
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	public void refresh() {
		fFeaturesViewer.refresh();
		int featureCount = fFeaturesViewer.getTable().getItemCount();
		fFeaturesTablePart.setButtonEnabled(1, featureCount > 0);
		super.refresh();
	}
	
	public void modelChanged(IModelChangedEvent event) {
		markStale();
	}

	public void commit(boolean onSave) {
		if (onSave && fBuildModel instanceof WorkspaceSiteBuildModel
				&& ((WorkspaceSiteBuildModel) fBuildModel).isDirty()) {
			((WorkspaceSiteBuildModel) fBuildModel).save();
		}
		super.commit(onSave);
	}
	
	public boolean canPaste(Clipboard clipboard) {
		return false;
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) 
			return true;
		return (actionId.equals(ActionFactory.DELETE.getId()))
				? handleRemoveFeature()
				: false;
	}
	public void handleNewFeature() {
		final Control control = fFeaturesViewer.getTable();
		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
			public void run() {
				BuiltFeaturesWizard wizard = new BuiltFeaturesWizard(
						fBuildModel);
				WizardDialog dialog = new WizardDialog(control.getShell(),
						wizard);
				if (dialog.open() == WizardDialog.OK) {
					markDirty();
				}
			}
		});
	}
	private boolean handleRemoveFeature() {
		try {
			IStructuredSelection ssel = (IStructuredSelection) fFeaturesViewer
					.getSelection();
			if (ssel != null && ssel.size() > 0) {
				ISiteBuildFeature[] sbFeatures = (ISiteBuildFeature[]) ssel
						.toList().toArray(new ISiteBuildFeature[ssel.size()]);
				for (int i = 0; i < sbFeatures.length; i++) {
					ISiteFeature feature = findMatchingSiteFeature(fModel, sbFeatures[i]);
					if (feature != null) {
						ISite site = fModel.getSite();
						site.removeFeatures(new ISiteFeature[]{feature});
					}
				}
				fBuildModel.getSiteBuild().removeFeatures(sbFeatures);
				markDirty();
				return true;
			}
		} catch (CoreException e) {
		}
		return false;
	}
	
	public static ISiteFeature findMatchingSiteFeature(ISiteModel model, ISiteBuildFeature sbfeature) {
		ISiteFeature[] sfeatures = model.getSite().getFeatures();
		for (int j = 0; j < sfeatures.length; j++) {
			ISiteFeature sfeature = sfeatures[j];
			if (matches(sfeature, sbfeature))
				return sfeature;
		}
		return null;
	}
	
	private static boolean matches(ISiteFeature sfeature,
			ISiteBuildFeature sbfeature) {
		return sbfeature.getId().equals(sfeature.getId())
				&& sbfeature.getVersion().equals(sfeature.getVersion());
	}	
	
	public static ISiteFeature createSiteFeature(ISiteModel model, ISiteBuildFeature sbfeature)
	throws CoreException {
		ISiteFeature sfeature = model.getFactory().createFeature();
		sfeature.setId(sbfeature.getId());
		sfeature.setVersion(sbfeature.getVersion());
		sfeature.setURL(model.getBuildModel().getSiteBuild().getFeatureLocation() + "/" + sbfeature.getId() + "_" + sbfeature.getVersion()+".jar");
		IFeature refFeature = sbfeature.getReferencedFeature();
		sfeature.setOS(refFeature.getOS());
		sfeature.setWS(refFeature.getWS());
		sfeature.setArch(refFeature.getArch());
		sfeature.setNL(refFeature.getNL());
		return sfeature;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TableSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		//getFormPage().setSelection(selection);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch(index) {
			case 0:
				handleNewFeature();
				break;
			case 1:
				handleBuild(fBuildModel.getSiteBuild().getFeatures());
		}
	}

	private void handleBuild(ISiteBuildFeature[] sbFeatures) {
		if (sbFeatures.length == 0)
			return;
		BuildSiteOperation op = new BuildSiteOperation(sbFeatures, fModel
				.getUnderlyingResource().getProject(), fBuildModel);
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(PDEPlugin
				.getActiveWorkbenchShell());
		try {
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString("SiteBuild.errorDialog"),
					PDEPlugin.getResourceString("SiteBuild.errorMessage"));
		} catch (InterruptedException e) {
		}
	}
}