// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wb.swt.SWTResourceManager;
import org.json.JSONArray;

import com.subshell.snippets.jface.tooltip.tooltipsupport.ICustomInformationControlCreator;
import com.subshell.snippets.jface.tooltip.tooltipsupport.Tooltips;
import com.subshell.snippets.jface.tooltip.tooltipsupport.TreeViewerInformationProvider;

import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackCore.IPacksChangeListener;
import phasereditor.assetpack.core.AssetPackCore.PackDelta;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.AudioSpriteAssetModel;
import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.PhysicsAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.TilemapAssetModel;
import phasereditor.assetpack.ui.editors.AssetPackEditor;
import phasereditor.assetpack.ui.preview.AtlasAssetInformationControl;
import phasereditor.assetpack.ui.preview.AtlasFrameInformationControl;
import phasereditor.assetpack.ui.preview.AudioAssetInformationControl;
import phasereditor.assetpack.ui.preview.AudioFileInformationControl;
import phasereditor.assetpack.ui.preview.AudioSpriteAssetElementInformationControl;
import phasereditor.assetpack.ui.preview.AudioSpriteAssetInformationControl;
import phasereditor.assetpack.ui.preview.BitmapFontAssetInformationControl;
import phasereditor.assetpack.ui.preview.ImageAssetInformationControl;
import phasereditor.assetpack.ui.preview.ImageFileInformationControl;
import phasereditor.assetpack.ui.preview.OtherAssetInformationControl;
import phasereditor.assetpack.ui.preview.PhysicsAssetInformationControl;
import phasereditor.assetpack.ui.preview.SpritesheetAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapAssetInformationControl;
import phasereditor.assetpack.ui.preview.TilemapTilesetInformationControl;
import phasereditor.assetpack.ui.widgets.AudioResourceDialog;
import phasereditor.assetpack.ui.widgets.ImageResourceDialog;
import phasereditor.audio.core.AudioCore;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.views.PreviewView;

public class AssetPackUI {

	private static List<ICustomInformationControlCreator> _informationControlCreators;

	/**
	 * Open the given element in an asset pack editor.
	 * 
	 * @param elem
	 *            An asset pack element (section, group, asset, etc..)
	 */
	public static boolean openElementInEditor(Object elem) {
		if (elem == null) {
			return false;
		}

		AssetPackModel pack = null;
		if (elem instanceof AssetModel) {
			pack = ((AssetModel) elem).getPack();
		} else if (elem instanceof AssetSectionModel) {
			pack = ((AssetSectionModel) elem).getPack();
		} else if (elem instanceof AssetPackModel) {
			pack = (AssetPackModel) elem;
		} else if (elem instanceof AssetGroupModel) {
			pack = ((AssetGroupModel) elem).getSection().getPack();
		} else {
			return false;
		}

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			AssetPackEditor editor = (AssetPackEditor) page.openEditor(new FileEditorInput(pack.getFile()),
					AssetPackEditor.ID);
			if (editor != null) {
				editor.revealElement(elem);
			}
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	public static void setAssetViewerUpdater(Composite comp, Runnable refresh, Supplier<AssetModel> getAsset) {
		IPacksChangeListener listener = new IPacksChangeListener() {

			@Override
			public void packsChanged(PackDelta packDelta) {
				AssetModel asset = getAsset.get();
				if (asset != null) {
					if (packDelta.contains(asset)) {
						PhaserEditorUI.swtRun(comp, (c) -> {
							if (c.isVisible()) {
								refresh.run();
							}
						});
					}
				}
			}
		};

		AssetPackCore.addPacksChangedListener(listener);

		comp.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				AssetPackCore.removePacksChangedListener(listener);
			}
		});
	}

	public static String browseAssetFile(AssetPackModel packModel, String objectName, IFile curFile, List<IFile> files,
			Shell shell, Consumer<String> action) {

		Set<IFile> usedFiles = packModel.sortFilesByNotUsed(files);

		// ok, but we want to put the current file at the head of the list
		if (curFile != null && files.contains(curFile)) {
			files.remove(curFile);
			files.add(0, curFile);
		}

		IFile result = null;

		IFile initial = curFile;
		if (initial == null && !files.isEmpty()) {
			initial = files.get(0);
		}

		ListDialog dlg = new ListDialog(shell);
		dlg.setTitle(objectName);
		dlg.setMessage("Select the " + objectName + " path. Those in bold are not used.");
		dlg.setLabelProvider(createFilesLabelProvider(packModel, usedFiles, shell));
		dlg.setContentProvider(new ArrayContentProvider());
		dlg.setInput(files);

		if (initial != null) {
			dlg.setInitialSelections(new Object[] { initial });
		}

		if (dlg.open() == Window.OK && dlg.getResult().length > 0) {
			result = (IFile) dlg.getResult()[0];
			if (result != null) {
				String path = packModel.getAssetUrl(result);
				action.accept(path);
				return path;
			}
		}

		return "";
	}

	public static String browseImageUrl(AssetPackModel packModel, String objectName, IFile curImageFile,
			List<IFile> imageFiles, Shell shell, Consumer<String> action) {

		Set<IFile> usedFiles = packModel.sortFilesByNotUsed(imageFiles);

		// ok, but we want to put the current file at the head of the list
		if (curImageFile != null && imageFiles.contains(curImageFile)) {
			imageFiles.remove(curImageFile);
			imageFiles.add(0, curImageFile);
		}

		IFile result = null;

		IFile initial = curImageFile;
		if (initial == null && !imageFiles.isEmpty()) {
			initial = imageFiles.get(0);
		}

		ImageResourceDialog dlg = new ImageResourceDialog(shell);
		dlg.setLabelProvider(createFilesLabelProvider(packModel, usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName(objectName);
		if (initial != null) {
			dlg.setInitial(initial);
		}

		if (dlg.open() == Window.OK) {
			result = (IFile) dlg.getSelection();
			if (result != null) {
				String path = packModel.getAssetUrl(result);
				action.accept(path);
				return path;
			}
		}

		return "";
	}

	public static String browseAudioUrl(AssetPackModel packModel, List<IFile> curAudioFiles, List<IFile> audioFiles,
			Shell shell, Consumer<String> action) {

		// Set<IFile> usedFiles = packModel.sortFilesByNotUsed(audioFiles);
		Set<IFile> usedFiles = packModel.findUsedFiles();

		// remove from the current files those are not part of the available
		// files
		for (IFile file : new ArrayList<>(curAudioFiles)) {
			if (!audioFiles.contains(file)) {
				curAudioFiles.remove(file);
			}
		}

		List<IFile> initialFiles = curAudioFiles;
		if (initialFiles == null && !audioFiles.isEmpty()) {
			initialFiles = new ArrayList<>(Arrays.asList(audioFiles.get(0)));
		}

		AudioResourceDialog dlg = new AudioResourceDialog(shell);
		dlg.setLabelProvider(createFilesLabelProvider(packModel, usedFiles, shell));
		dlg.setInput(audioFiles);

		if (initialFiles != null) {
			dlg.setInitialFiles(initialFiles);
		}

		if (dlg.open() == Window.OK) {
			List<IFile> selection = dlg.getSelection();
			if (selection != null) {
				JSONArray array = new JSONArray();
				for (IFile file : selection) {
					String url = packModel.getAssetUrl(file);
					array.put(url);
				}
				String json = array.toString();
				action.accept(json);
				return json;
			}
		}

		return "";
	}

	private static LabelProvider createFilesLabelProvider(AssetPackModel packModel, Set<IFile> usedFiles, Shell shell) {
		class FilesLabelProvider extends LabelProvider implements IFontProvider {
			@Override
			public Font getFont(Object element) {
				Font font = shell.getFont();
				if (usedFiles.contains(element)) {
					return font;
				}
				font = SWTResourceManager.getBoldFont(font);
				return font;
			}

			@Override
			public String getText(Object element) {
				return packModel.getAssetUrl((IFile) element);
			}
		}
		return new FilesLabelProvider();
	}

	public static class FrameData {
		public Rectangle src;
		public Rectangle dst;
	}

	public static List<FrameData> generateSpriteSheetRects(SpritesheetAssetModel s, Rectangle src, Rectangle dst) {

		List<FrameData> list = new ArrayList<>();

		int w = s.getFrameWidth();
		int h = s.getFrameHeight();
		int margin = s.getMargin();
		int spacing = s.getSpacing();

		if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
			// invalid parameters
			return list;
		}

		Rectangle b = src;
		int dstX = dst.x;
		int dstY = dst.y;
		int dstW = dst.width;
		int dstH = dst.height;
		double wfactor = dstW / (double) b.width;
		double hfactor = dstH / (double) b.height;

		int max = s.getFrameMax();
		if (max <= 0) {
			max = Integer.MAX_VALUE;
		}

		int i = 0;
		int x = margin;
		int y = margin;
		while (true) {
			if (i >= max || y >= b.height) {
				break;
			}

			int x1 = (int) (x * wfactor);
			int y1 = (int) (y * hfactor);
			int w1 = (int) (w * wfactor);
			int h1 = (int) (h * hfactor);

			FrameData fd = new FrameData();
			fd.src = new Rectangle(x, y, w, h);
			fd.dst = new Rectangle(dstX + x1, dstY + y1, w1, h1);

			list.add(fd);

			x += w + spacing;
			if (x >= b.width) {
				x = margin;
				y += h + spacing;
			}
			i++;
		}
		return list;
	}

	static void registerPreviewUpdater() {
		// global listeners
		AssetPackCore.addPacksChangedListener(new IPacksChangeListener() {

			@Override
			public void packsChanged(PackDelta delta) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						if (PlatformUI.getWorkbench().isClosing()) {
							return;
						}

						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						IWorkbenchPage page = window.getActivePage();
						IViewReference[] refs = page.getViewReferences();

						for (IViewReference ref : refs) {
							if (ref.getId().equals(PreviewView.ID)) {
								PreviewView view = (PreviewView) ref.getView(true);
								Object elem = view.getPreviewElement();

								if (elem != null) {
									if (elem instanceof AssetModel) {
										AssetModel asset = (AssetModel) elem;
										boolean alive = asset.isOnWorkspace();
										if (alive) {
											if (delta.contains(asset, asset.getPack())) {
												view.preview(asset);
											}
										} else {
											view.preview(null);
										}
									}
								}
							}
						}
					}
				});
			}
		});
	}

	static void registerProjectExplorerTooltips() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(1_000);
				} catch (InterruptedException e) {
					//
				}
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						registerProjectExplorerTooltips2();
					}
				});

			}
		}).start();
	}

	static void registerProjectExplorerTooltips2() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		for (IViewReference ref : page.getViewReferences()) {
			if (ref.getId().endsWith(ProjectExplorer.VIEW_ID)) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					installAssetTooltips(((ProjectExplorer) part).getCommonViewer());
				}
			}
		}
		page.addPartListener(new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				if (part instanceof ProjectExplorer) {
					installAssetTooltips(((ProjectExplorer) part).getCommonViewer());
				}
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				// nothing
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				// nothing
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				// nothing
			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				// nothing
			}
		});
	}

	public static void installAssetTooltips(TreeViewer viewer) {
		List<ICustomInformationControlCreator> creators = getInformationControlCreatorsForTooltips();

		Tooltips.install(viewer.getControl(), new TreeViewerInformationProvider(viewer), creators, false);
	}

	public static List<ICustomInformationControlCreator> getInformationControlCreatorsForTooltips() {
		if (_informationControlCreators == null) {
			_informationControlCreators = new ArrayList<>();

			// FILES

			// image file

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new ImageFileInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					if (info instanceof IFile) {
						return AssetPackCore.isImage((IFile) info);
					}
					return false;
				}
			});

			// audio file

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioFileInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					if (info instanceof IFile) {
						return AudioCore.isSupportedAudio((IFile) info);
					}
					return false;
				}
			});

			// ASSET MODELS

			// image

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new ImageAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof ImageAssetModel;
				}
			});

			// spritesheet

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new SpritesheetAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof SpritesheetAssetModel;
				}
			});

			// audio

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info != null && info.getClass() == AudioAssetModel.class;
				}
			});

			// audio sprites

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioSpriteAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info != null && info.getClass() == AudioSpriteAssetModel.class;
				}
			});

			// audio sprites elements

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AudioSpriteAssetElementInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AudioSpriteAssetModel.AssetAudioSprite;
				}
			});

			// atlas

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AtlasAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AtlasAssetModel;
				}
			});

			// atlas frame

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new AtlasFrameInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AtlasAssetModel.FrameItem;
				}
			});

			// bitmap font

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new BitmapFontAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof BitmapFontAssetModel;
				}
			});

			// physics

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new PhysicsAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof PhysicsAssetModel || info instanceof PhysicsAssetModel.SpriteData;
				}
			});

			// tilemap

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new TilemapAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof TilemapAssetModel;
				}
			});

			// tilemap tileset

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new TilemapTilesetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof TilemapAssetModel.Tileset;
				}
			});

			// the others

			_informationControlCreators.add(new ICustomInformationControlCreator() {

				@Override
				public IInformationControl createInformationControl(Shell parent) {
					return new OtherAssetInformationControl(parent);
				}

				@Override
				public boolean isSupported(Object info) {
					return info instanceof AssetModel;
				}
			});

		}
		return _informationControlCreators;
	}
}