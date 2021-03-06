/*
 *  Copyright (C) 2017 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.commands.export;

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.FastaFileFilter;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.ReadsExporter;
import megan.fx.NotificationsInSwing;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ExportReadsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export what=reads [data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "}] file=<filename>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=reads");

        Director dir = getDir();
        Document doc = dir.getDocument();

        String data = ClassificationType.Taxonomy.toString();
        if (np.peekMatchIgnoreCase("data")) {
            np.matchIgnoreCase("data=");
            data = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        }
        Set<Integer> classIds = new HashSet<>();
        if (data.equals(ClassificationType.Taxonomy.toString()))
            classIds.addAll(dir.getMainViewer().getSelectedIds());
        else {
            ClassificationViewer classificationViewer = (ClassificationViewer) getDir().getViewerByClassName(data);
            if (classificationViewer != null)
                classIds.addAll(classificationViewer.getSelectedIds());
        }

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        int count;
        if (classIds.size() == 0)
            count = ReadsExporter.exportAll(doc.getConnector(), outputFile, doc.getProgressListener());
        else
            count = ReadsExporter.export(data, classIds, doc.getConnector(), outputFile, doc.getProgressListener());

        NotificationsInSwing.showInformation(getViewer().getFrame(), "Wrote " + count + " reads to file: " + outputFile);
    }

    public boolean isApplicable() {
        return getDoc().getMeganFile().hasDataConnector();
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();
        if (!dir.getDocument().getMeganFile().hasDataConnector())
            return;
        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-ex.fasta");
        File lastOpenFile = new File(name);

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new FastaFileFilter(), new FastaFileFilter(), event, "Save all READs to file", ".fasta");

        String data;
        if (getViewer() instanceof ClassificationViewer)
            data = ((ClassificationViewer) getViewer()).getClassName();
        else
            data = ClassificationType.Taxonomy.toString();

        if (file != null) {
            String cmd;
            cmd = ("export what=reads data=" + data + " file='" + file.getPath() + "';");
            execute(cmd);
        }
    }

    public String getName() {
        return "Reads...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    public String getDescription() {
        return "Export all reads to a text file (or only those for selected nodes, if any selected)";
    }
}


