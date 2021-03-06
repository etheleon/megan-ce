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
package megan.importblast;

import jloda.gui.commands.CommandManager;
import jloda.util.ProgramProperties;
import megan.core.Document;
import megan.importblast.commands.SetUseComplexityFilterCommand;
import megan.importblast.commands.SetUseIdentityFilterCommand;
import megan.importblast.commands.SetUseReadMagnitudesCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * panel for setting LCA parameters
 * Daniel Huson, 12.2012
 */
public class LCAParametersPanel extends JPanel {
    /**
     * construct the parameters panel
     */
    public LCAParametersPanel(final ImportBlastDialog dialog) {
        final CommandManager commandManager = dialog.getCommandManager();

        setLayout(new BorderLayout());

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(14, 2));

        centerPanel.add(new JLabel("Min Score:"));
        centerPanel.add(dialog.getMinScoreField());
        dialog.getMinScoreField().setToolTipText("Minimal bitscore that a match must attain");

        centerPanel.add(new JLabel("Max Expected:"));
        centerPanel.add(dialog.getMaxExpectedField());
        dialog.getMaxExpectedField().setToolTipText("Ignore all matches whose expected values lie above this threshold");

        centerPanel.add(new JLabel("Min Percent Identity:"));
        centerPanel.add(dialog.getMinPercentIdentityField());
        dialog.getMaxExpectedField().setToolTipText("Ignore all matches whose min percent identity lie above this threshold");

        centerPanel.add(new JLabel(" "));
        centerPanel.add(new JLabel(" "));

        centerPanel.add(new JLabel("Top Percent:"));
        centerPanel.add(dialog.getTopPercentField());
        dialog.getTopPercentField().setToolTipText("Match must lie within this percentage of the best score attained for a read");

        centerPanel.add(new JLabel(" "));
        centerPanel.add(new JLabel(" "));

        centerPanel.add(new JLabel("Min Support Percent:"));
        centerPanel.add(dialog.getMinSupportPercentField());
        dialog.getMinSupportPercentField().setToolTipText("Minimum number of reads that a taxon must obtain as a percentage of total reads assigned");

        centerPanel.add(new JLabel("Min Support:"));
        centerPanel.add(dialog.getMinSupportField());
        dialog.getMinSupportField().setToolTipText("Minimum number of reads that a taxon must obtain");

        centerPanel.add(new JLabel("LCA Algorithm:"));
        centerPanel.add(new JLabel(" "));

        {
            final JComboBox<String> lcaAlgorithmComboBox = dialog.getLcaAlgorithmComboBox();
            lcaAlgorithmComboBox.setEditable(false);
            for (Document.LCAAlgorithm algorithm : Document.LCAAlgorithm.values()) {
                lcaAlgorithmComboBox.addItem(algorithm.toString());
            }
            lcaAlgorithmComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (lcaAlgorithmComboBox.getSelectedItem() != null)
                        dialog.setLcaAlgorithm(Document.LCAAlgorithm.valueOf((String) lcaAlgorithmComboBox.getSelectedItem()));
                    else
                        dialog.setLcaAlgorithm(Document.DEFAULT_LCA_ALGORITHM);
                    dialog.getWeightedLCAPercentField().setEnabled(dialog.getLcaAlgorithm().equals(Document.LCAAlgorithm.Weighted));
                    ProgramProperties.put("SelectedLCAAlgorithm", dialog.getLcaAlgorithm().toString());
                }
            });
            Document.LCAAlgorithm algorithm = Document.LCAAlgorithm.valueOfIgnoreCase(ProgramProperties.get("SelectedLCAAlgorithm", Document.DEFAULT_LCA_ALGORITHM.toString()));
            if (algorithm == null || (!dialog.isLongReads() && algorithm == Document.LCAAlgorithm.NaiveMultiGene))
                algorithm = Document.LCAAlgorithm.Naive;
            else if (dialog.isLongReads() && algorithm != Document.LCAAlgorithm.NaiveMultiGene)
                algorithm = Document.LCAAlgorithm.NaiveMultiGene;

            lcaAlgorithmComboBox.setSelectedItem(algorithm.toString());
            lcaAlgorithmComboBox.setToolTipText("Set LCA algorithm for taxonomic binning");

            dialog.getWeightedLCAPercentField().setToolTipText("Percent of weight to cover by weighted LCA");

            centerPanel.add(lcaAlgorithmComboBox);
            dialog.getWeightedLCAPercentField().setEnabled(dialog.getLcaAlgorithm().equals(Document.LCAAlgorithm.Weighted));
            centerPanel.add(dialog.getWeightedLCAPercentField());
            dialog.getWeightedLCAPercentField().setToolTipText("Percent of weight to cover by weighted LCA");

            centerPanel.add(new JLabel(" "));
            centerPanel.add(new JLabel(" "));
        }

        {
            final AbstractButton button = commandManager.getButton(SetUseComplexityFilterCommand.NAME);
            button.setText(button.getText() + ":");
            centerPanel.add(button);
            centerPanel.add(dialog.getMinComplexityField());
            dialog.getMinComplexityField().setToolTipText("Minimum complexity for a read to be considered non-repetitive\nComputed as compression ratio between 0 and 1");
        }

        JPanel three = new JPanel();
        three.setLayout(new BoxLayout(three, BoxLayout.X_AXIS));
        outerPanel.setBorder(BorderFactory.createTitledBorder("LCA and analysis parameters"));

        three.add(Box.createHorizontalGlue());
        three.add(centerPanel);
        three.add(Box.createHorizontalGlue());

        outerPanel.add(three);

        JPanel aPanel = new JPanel();
        aPanel.add(commandManager.getButton(SetUseReadMagnitudesCommand.NAME));
        aPanel.add(commandManager.getButton(SetUseIdentityFilterCommand.NAME));
        outerPanel.add(aPanel);

        add(outerPanel, BorderLayout.CENTER);
    }

}
