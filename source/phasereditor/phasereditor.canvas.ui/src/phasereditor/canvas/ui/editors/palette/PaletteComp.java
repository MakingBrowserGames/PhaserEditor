// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.canvas.ui.editors.palette;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.json.JSONArray;
import org.json.JSONObject;

import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetElementModel;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;

/**
 * @author arian
 *
 */
public class PaletteComp extends Composite {
	TableViewer _viewer;
	ArrayList<Object> _list;
	private boolean _paletteVisible;

	public PaletteComp(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout(SWT.HORIZONTAL));

		_viewer = new TableViewer(this, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {
		// _viewer.getTable().setBackgroundImage(EditorSharedImages.getImage("icons/preview-pattern.png"));

		{
			int options = DND.DROP_MOVE | DND.DROP_DEFAULT;
			DropTarget target = new DropTarget(_viewer.getTable(), options);
			Transfer[] types = { LocalSelectionTransfer.getTransfer() };
			target.setTransfer(types);
			target.addDropListener(new PaletteDropAdapter(this));
		}
		{
			Transfer[] types = { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance() };
			_viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_DEFAULT, types, new DragSourceAdapter() {

				private Object[] _data;

				@Override
				public void dragStart(DragSourceEvent event) {
					LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
					_data = ((IStructuredSelection) _viewer.getSelection()).toArray();
					transfer.setSelection(new StructuredSelection(_data));
				}

				@Override
				public void dragSetData(DragSourceEvent event) {
					JSONArray array = new JSONArray();
					for (Object elem : _data) {
						if (elem instanceof IAssetKey) {
							array.put(AssetPackCore.getAssetJSONReference((IAssetKey) elem));
						}
					}
					event.data = array.toString();
				}
			});
		}

		_list = new ArrayList<>();
		_viewer.setLabelProvider(new LabelProvider() {

			@Override
			public Image getImage(Object element) {
				return AssetLabelProvider.GLOBAL_48.getImage(element);
			}

			@Override
			public String getText(Object element) {
				return "";
			}

		});
		_viewer.setContentProvider(new ArrayContentProvider());
		_viewer.setInput(_list);

		{
			addControlListener(new ControlListener() {

				@Override
				public void controlResized(ControlEvent e) {
					e.display.asyncExec(PaletteComp.this::updateWidth);
				}

				@Override
				public void controlMoved(ControlEvent e) {
					e.display.asyncExec(PaletteComp.this::updateWidth);
				}
			});
		}

		AssetPackUI.installAssetTooltips(_viewer);
	}

	void drop(int index, Object[] data) {
		Object reveal = processDrop(index, data);
		if (reveal != null) {
			_viewer.refresh();
			_viewer.reveal(reveal);
			// _viewer.getTable().setTopIndex(_list.indexOf(reveal));
		}

		updateWidth();
	}

	public void drop(Object[] items) {
		drop(_list.size(), items);
	}

	public void updateWidth() {
		Table table = _viewer.getTable();

		if (table.isDisposed()) {
			return;
		}

		ScrollBar bar = table.getVerticalBar();

		Object layoutData = getLayoutData();
		if (layoutData instanceof GridData) {
			int hint;
			if (isPaletteVisible()) {
				hint = computeSize(SWT.DEFAULT, getBounds().height).x;
				if (!bar.isVisible()) {
					hint -= bar.getSize().x;
				}

				if (hint < 48) {
					hint = 48;
				}

			} else {
				hint = 0;
			}

			GridData gd = (GridData) layoutData;

			if (gd.widthHint != hint) {
				gd.widthHint = hint;
				getParent().layout();
			}
		}
	}

	private Object processDrop(int index, Object[] data) {
		Object reveal = null;
		for (Object elem : data) {
			if (elem instanceof IAssetFrameModel || elem instanceof ImageAssetModel) {
				if (!_list.contains(elem)) {
					_list.add(index, elem);
					reveal = elem;
				}
			} else if (elem instanceof AssetModel) {
				List<? extends IAssetElementModel> elems = ((AssetModel) elem).getSubElements();
				if (elems != null) {
					reveal = processDrop(index, elems.toArray());
				}
			}
		}
		return reveal;
	}

	public JSONObject toJSON() {
		JSONObject data = new JSONObject();
		JSONArray items = new JSONArray();
		for (Object obj : _list) {
			JSONObject item = AssetPackCore.getAssetJSONReference((IAssetKey) obj);
			if (item != null) {
				items.put(item);
			}
		}
		data.put("assets", items);
		data.put("visible", isPaletteVisible());
		return data;
	}

	public void updateFromJSON(JSONObject data) {
		_list.clear();
		JSONArray list = data.getJSONArray("assets");
		for (int i = 0; i < list.length(); i++) {
			JSONObject assetRef = list.getJSONObject(i);
			Object asset = AssetPackCore.findAssetElement(assetRef);
			if (asset != null) {
				_list.add(asset);
			}

		}
		_viewer.refresh();

		boolean visible = data.optBoolean("visible", true);
		setPaletteVisble(visible);
	}

	public TableViewer getViewer() {
		return _viewer;
	}

	public void deleteSelected() {
		_list.removeAll(((StructuredSelection) _viewer.getSelection()).toList());
		_viewer.refresh();
		updateWidth();
	}

	public void setPaletteVisble(boolean visible) {
		_paletteVisible = visible;
		updateWidth();
	}

	public boolean isPaletteVisible() {
		return _paletteVisible;
	}

}