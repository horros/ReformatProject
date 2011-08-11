/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.necora.nb.reformat;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.indent.api.Reformat;
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
	private StyledDocument document;

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
			reformat(fileObject.getChildren());
		}

	}

	private void reformat(FileObject[] children) {
		for (FileObject fileObject : children) {
			if (fileObject.isFolder()) {
				reformat(fileObject.getChildren());
			}

			try {

				DataObject data = DataObject.find(fileObject);
				System.out.println("Attempting to reformat " + data.getName());
				EditorCookie ec = (EditorCookie) data.getCookie(EditorCookie.class);
				if (ec != null) {
					this.document = ec.getDocument();
					if (document instanceof BaseDocument) {
						BaseDocument bd = (BaseDocument) document;
						bd.runAtomic(new Runnable() {

							@Override
							public void run() {
								Reformat reformat = Reformat.get(document);
								reformat.lock();
								try {
									reformat.reformat(0, document.getLength() - 1);
								} catch (BadLocationException ex) {
									Exceptions.printStackTrace(ex);
								}
							}
						});
						ec.saveDocument();
						ec.close();
					}
					this.document = null;
				}

			} catch (IOException ioe) {
				System.out.println("LOL");
			}

		}

	}
}
