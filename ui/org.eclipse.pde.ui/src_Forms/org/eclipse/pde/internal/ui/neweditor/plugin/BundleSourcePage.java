package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.graphics.*;

public class BundleSourcePage extends KeyValueSourcePage {
	class BundleOutlineContentProvider extends DefaultContentProvider
			implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return false;
		}
		public Object getParent(Object child) {
			if (child instanceof IBuildEntry)
				return ((IBuildEntry) child).getModel();
			return null;
		}
		public Object[] getElements(Object parent) {
			if (parent instanceof IBundleModel) {
				IBundleModel model = (IBundleModel) parent;
				Dictionary manifest = model.getManifest();
				Object [] keys = new Object[manifest.size()];
				int i=0;
				for (Enumeration enum=manifest.keys(); enum.hasMoreElements();) {
					keys[i++] = enum.nextElement();
				}
				return keys;
			}
			return new Object[0];
		}
	}
	class BundleLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IBuildEntry) {
				return ((IBuildEntry) obj).getName();
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof IBuildEntry)
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_BUILD_VAR_OBJ);
			return null;
		}
	}
	
	public BundleSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	
	protected ILabelProvider createOutlineLabelProvider() {
		return new BundleLabelProvider();
	}
	
	protected ITreeContentProvider createOutlineContentProvider() {
		return new BundleOutlineContentProvider();
	}
}