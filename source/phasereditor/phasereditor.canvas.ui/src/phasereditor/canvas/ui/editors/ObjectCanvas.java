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
package phasereditor.canvas.ui.editors;

import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;

import javafx.embed.swt.FXCanvas;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.behaviors.CreateBehavior;
import phasereditor.canvas.ui.editors.behaviors.DragBehavior;
import phasereditor.canvas.ui.editors.behaviors.MouseBehavior;
import phasereditor.canvas.ui.editors.behaviors.PaintBehavior;
import phasereditor.canvas.ui.editors.behaviors.SelectionBehavior;
import phasereditor.canvas.ui.editors.behaviors.UpdateBehavior;
import phasereditor.canvas.ui.editors.behaviors.ZoomBehavior;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.editors.palette.PaletteComp;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.GroupControl;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class ObjectCanvas extends FXCanvas {
	private CreateBehavior _createBehavior;
	private Pane _selectionPane;
	private SelectionBehavior _selectionBehavior;
	private DragBehavior _dragBehavior;
	private WorldModel _worldModel;
	private PGrid _pgrid;
	private UpdateBehavior _updateBehavior;
	private GroupControl _worldControl;
	private TreeViewer _outline;
	private StackPane _root;
	private ZoomBehavior _zoomBehavior;
	private Pane _selectionGlassPane;
	private MouseBehavior _mouseBehavior;
	private GridPane _gridPane;
	private WorldGlassPane _worldGlassPane;
	private PaintBehavior _paintBehavior;
	private PaletteComp _palette;
	private CanvasEditor _editor;
	private SceneSettings _settingsModel;

	public ObjectCanvas(Composite parent, int style) {
		super(parent, style);
	}

	public void init(CanvasEditor editor, CanvasEditorModel model, PGrid grid, TreeViewer outline,
			PaletteComp palette) {
		_editor = editor;
		_settingsModel = model.getSettings();
		_worldModel = model.getWorld();
		_pgrid = grid;
		_outline = outline;
		_palette = palette;

		createScene();

		initDrop();

		_createBehavior = new CreateBehavior(this, palette);
		_selectionBehavior = new SelectionBehavior(this);
		_dragBehavior = new DragBehavior(this);
		_updateBehavior = new UpdateBehavior(this, _pgrid, outline);
		_zoomBehavior = new ZoomBehavior(this);
		_mouseBehavior = new MouseBehavior(this);
		_paintBehavior = new PaintBehavior(this);

		_updateBehavior.updateFromSettings();
	}

	public CanvasEditor getEditor() {
		return _editor;
	}

	public void selectAll() {
		_selectionBehavior.selectAll();
	}

	public PaintBehavior getPaintBehavior() {
		return _paintBehavior;
	}

	public PaletteComp getPalette() {
		return _palette;
	}

	public TreeViewer getOutline() {
		return _outline;
	}

	public PGrid getPGrid() {
		return _pgrid;
	}

	public SceneSettings getSettingsModel() {
		return _settingsModel;
	}

	public WorldModel getWorldModel() {
		return _worldModel;
	}

	public MouseBehavior getMouseBehavior() {
		return _mouseBehavior;
	}

	public CreateBehavior getCreateBehavior() {
		return _createBehavior;
	}

	public ZoomBehavior getZoomBehavior() {
		return _zoomBehavior;
	}

	public SelectionBehavior getSelectionBehavior() {
		return _selectionBehavior;
	}

	public DragBehavior getDragBehavior() {
		return _dragBehavior;
	}

	public UpdateBehavior getUpdateBehavior() {
		return _updateBehavior;
	}

	private void initDrop() {
		getScene().setOnDragOver(event -> {
			try {
				ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null) {
					event.consume();
				} else {
					event.acceptTransferModes(TransferMode.ANY);
					ObjectCanvas.this.setFocus();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		getScene().setOnDragDropped(event -> {
			try {
				ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				List<Node> newnodes = _createBehavior.dropAssets((IStructuredSelection) selection, event);
				_selectionBehavior.setSelection(new StructuredSelection(newnodes.toArray()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private void createScene() {
		_root = new StackPane();
		setScene(new Scene(_root));

		_gridPane = new GridPane(this);
		_gridPane.setId("__grid-pane__");

		_worldControl = new GroupControl(this, _worldModel);
		GroupNode world = _worldControl.getNode();

		_worldGlassPane = new WorldGlassPane(this);
		_worldGlassPane.setId("__world-glass-pane__");

		_selectionPane = new Pane();
		_selectionPane.setId("__selection-pane__");

		_selectionGlassPane = new Pane();
		_selectionGlassPane.setId("__selection-glass-pane__");

		_root.setAlignment(Pos.TOP_LEFT);

		_root.getChildren().setAll(_gridPane, world, _worldGlassPane, _selectionPane, _selectionGlassPane);
	}

	public GridPane getGridPane() {
		return _gridPane;
	}

	public WorldGlassPane getWorldGlassPane() {
		return _worldGlassPane;
	}

	public Pane getRootPane() {
		return _root;
	}

	public GroupNode getWorldNode() {
		return _worldControl.getNode();
	}

	public Pane getSelectionPane() {
		return _selectionPane;
	}

	public Pane getSelectionGlassPane() {
		return _selectionGlassPane;
	}

	public void dropToWorld(BaseObjectControl<?> control, double sceneX, double sceneY) {
		IObjectNode node = control.getIObjectNode();
		GroupNode worldNode = getWorldNode();

		double invScale = 1 / _zoomBehavior.getScale();
		Point2D translate = _zoomBehavior.getTranslate();

		double x = (sceneX - translate.getX()) * invScale;
		double y = (sceneY - translate.getY()) * invScale;

		double w = control.getTextureWidth() / 2;
		double h = control.getTextureHeight() / 2;

		BaseObjectModel model = control.getModel();
		model.setX(x - w);
		model.setY(y - h);

		control.updateFromModel();

		worldNode.getControl().addChild(node);

		getWorldModel().firePropertyChange(WorldModel.PROP_STRUCTURE);
	}

	public void dirty() {
		getWorldModel().setDirty(true);
	}
}
