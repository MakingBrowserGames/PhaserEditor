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
package phasereditor.canvas.ui.editors.behaviors;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.shapes.BaseObjectControl;
import phasereditor.canvas.ui.shapes.IObjectNode;

/**
 * @author arian
 *
 */
public class DragBehavior {
	private ObjectCanvas _canvas;
	private Point2D _startScenePoint;
	private List<DragInfo> _dragInfoList;
	private SelectionBehavior _selbehavior;
	private boolean _dragging;

	static class DragInfo {
		private Node _node;
		private Point2D _start;

		public DragInfo(Node node, Point2D start) {
			super();
			this._node = node;
			this._start = start;
		}

		public Node getNode() {
			return _node;
		}

		public Point2D getStart() {
			return _start;
		}

	}

	public DragBehavior(ObjectCanvas canvas) {
		super();
		_canvas = canvas;
		_selbehavior = canvas.getSelectionBehavior();
		_dragInfoList = new ArrayList<>();
	}

	void handleMouseReleased(@SuppressWarnings("unused") MouseEvent event) {
		_dragging = false;

		for (DragInfo draginfo : _dragInfoList) {
			Node node = draginfo.getNode();
			BaseObjectControl<?> control = ((IObjectNode) node).getControl();
			BaseObjectModel model = control.getModel();
			model.setLocation(node.getLayoutX(), node.getLayoutY());

			UpdateBehavior updateBehavior = _canvas.getUpdateBehavior();
			updateBehavior.update_Grid_from_PropertyChange(control.getX_property());
			updateBehavior.update_Grid_from_PropertyChange(control.getY_property());
		}

		_dragInfoList.clear();
	}

	void handleMouseDragged(MouseEvent event) {
		double dx = event.getSceneX() - _startScenePoint.getX();
		double dy = event.getSceneY() - _startScenePoint.getY();
		Point2D delta = new Point2D(dx, dy);

		double scale = _canvas.getZoomBehavior().getScale();

		for (DragInfo draginfo : _dragInfoList) {
			Node dragnode = draginfo.getNode();
			Point2D start = draginfo.getStart();

			dx = delta.getX();
			dy = delta.getY();

			double x = start.getX() + dx / scale;
			double y = start.getY() + dy / scale;

			dragnode.setLayoutX(x);
			dragnode.setLayoutY(y);
		}
		_canvas.getSelectionBehavior().updateSelectedNodes();
	}

	void handleDragDetected(MouseEvent event) {
		_dragging = true;
		
		for (IObjectNode selnode : _selbehavior.getSelectedNodes()) {
			Node dragnode = selnode.getNode();

			if (_dragInfoList.stream().anyMatch(info -> info.getNode() == dragnode)) {
				continue;
			}

			Point2D start = new Point2D(dragnode.getLayoutX(), dragnode.getLayoutY());
			_dragInfoList.add(new DragInfo(dragnode, start));
		}

		_startScenePoint = new Point2D(event.getSceneX(), event.getSceneY());
	}

	public void abort() {
		for (DragInfo draginfo : _dragInfoList) {
			Point2D start = draginfo.getStart();
			Node node = draginfo.getNode();

			node.relocate(start.getX(), start.getY());
		}
		_dragInfoList.clear();
		_selbehavior.updateSelectedNodes();
	}

	public boolean isDragging() {
		return _dragging;
	}
}