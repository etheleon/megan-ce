/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.samplesviewer.commands.algorithms;

import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.fx.Dialogs;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * compute biome
 * Daniel Huson, 2.2013
 */
public class RareBiomeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        float threshold = (float) ProgramProperties.get("RareBiomeThreshold", 50.0);
        String result = Dialogs.showInput(getViewer().getFrame(), "Rare biome input parameter", "Set maximum percentage", "" + threshold);
        if (result != null && Basic.isFloat(result)) {
            final Collection<String> samples = ((SamplesViewer) getViewer()).getSamplesTable().getSelectedSamplesInOrder();
            if (samples.size() > 1) {
                threshold = Basic.parseInt(result);
                ProgramProperties.put("RareBiomeThreshold", threshold);
                execute("compute biome=rare threshold=" + result + " samples='" + Basic.toString(samples, "' '") + "';");
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer && ((SamplesViewer) getViewer()).getSamplesTable().getNumberOfSelectedSamples() > 1;
    }

    public boolean isCritical() {
        return true;
    }

    public String getName() {
        return "Compute Rare Biome...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Determine taxa and functions that appear in a minority of samples";
    }
}

