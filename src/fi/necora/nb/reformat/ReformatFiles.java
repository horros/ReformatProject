/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.necora.nb.reformat;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.netbeans.modules.versioning.spi.VersioningSystem;
import org.openide.loaders.DataObject;

import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

@ActionID(category = "Source",
id = "fi.necora.nb.reformat.ReformatFiles")
@ActionRegistration(displayName = "#CTL_ReformatFiles")
@ActionReferences({
	@ActionReference(path = "Menu/Source", position = 350),
	@ActionReference(path = "Loaders/folder/any/Actions", position = 9200)
})
@Messages("CTL_ReformatFiles=Reformat all files")
public final class ReformatFiles implements ActionListener {

	private final DataObject context;

	public ReformatFiles(DataObject context) {
		this.context = context;
		System.out.println("Context: " + context.getName());
		System.out.println("ASDF: " + context.getPrimaryFile().getPath());

	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		File f = new File(context.getPrimaryFile().getPath());
		FileObject fileObject = FileUtil.toFileObject(f);

		if (fileObject.isFolder()) {
			ReformatFilesRunner rfr = new ReformatFilesRunner();
			rfr.setFileObject(fileObject);
			RequestProcessor.Task task =
							RequestProcessor.getDefault().post(rfr);
		}

	}

	private class ReformatFilesRunner implements Runnable {

		FileObject fileObject = null;
		private ProgressHandle createHandle;
		private int nbFiles;
		private int step;
		private int count;

		public FileObject getFileObject() {
			return fileObject;
		}

		public void setFileObject(FileObject fileObject) {
			this.fileObject = fileObject;
		}

		@Override
		public void run() {
			step = 0;
			count = 0;
			createHandle = ProgressHandleFactory.createHandle("Reformatting files");
			createHandle.setInitialDelay(0);
			createHandle.start();
			createHandle.switchToIndeterminate();
			nbFiles = countFiles(fileObject.getChildren());
			System.out.println("Count: " + nbFiles);
			createHandle.switchToDeterminate(nbFiles);
			reformat(fileObject.getChildren());
			createHandle.finish();
		}

		private void reformat(FileObject[] children) {
			for (FileObject fo : children) {

				if (FileUtil.toFile(fo).isDirectory()) {
					reformat(fo.getChildren());
				} else {

					File file = FileUtil.toFile(fo);
					VersioningSystem owner = VersioningSupport.getOwner(file);
					if (owner != null && owner.getVisibilityQuery() != null && !owner.getVisibilityQuery().isVisible(file)) {
						continue;
					}
					try {
						createHandle.progress(file.getName(), step);

						DataObject data = DataObject.find(fo);
						EditorCookie ec = (EditorCookie) data.getCookie(EditorCookie.class);
						if (ec != null) {
							final StyledDocument document = ec.openDocument();
							if (document instanceof AbstractDocument) {
								System.out.println("Formatting " + file.getName());
								BaseDocument bd = (BaseDocument) document;
								bd.runAtomic(new Runnable() {

									@Override
									public void run() {
										Reformat reformat = Reformat.get(document);
										reformat.lock();
										try {
											if (document.getLength() > 0) {
												reformat.reformat(0, document.getLength() - 1);
											}
										} catch (BadLocationException ex) {
											Exceptions.printStackTrace(ex);
										} finally {
											reformat.unlock();
										}
									}
								});
								ec.saveDocument();
								ec.close();
							}
							step++;


						}


					} catch (IOException ioe) {
						System.out.println("LOL");
					}

				}
			}

		}
	}
	private int counter = 0;

	private synchronized int countFiles(FileObject[] children) {
		for (FileObject countObject : children) {
			if (countObject.isFolder()) {
				countFiles(countObject.getChildren());
			} else {
				File file = FileUtil.toFile(countObject);
				VersioningSystem owner = VersioningSupport.getOwner(file);
				if (owner != null && owner.getVisibilityQuery() != null && !owner.getVisibilityQuery().isVisible(file)) {
					continue;
				}
				System.out.println(file.getAbsolutePath());
				counter++;
			}
		}

		return counter;
	}
}
