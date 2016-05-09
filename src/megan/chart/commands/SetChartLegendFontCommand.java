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
package megan.chart.commands;

import jloda.gui.ChooseFontDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * set font
 * Daniel Huson, 9.2012
 */
public class SetChartLegendFontCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        ChartViewer chartViewer = (ChartViewer) getViewer();

        String target = ChartViewer.FontKeys.LegendFont.toString();
        Font font = ProgramProperties.get(target, ChartViewer.defaultFont);
        Color color = ProgramProperties.get(target + "Color", (Color) null);
        Pair<Font, Color> result = ChooseFontDialog.showChooseFontDialog(chartViewer.getFrame(), "Choose legend font", font, color);
        if (result != null)
            execute("set chartFont='" + Basic.encode(result.get1())
                    + "' color=" + (result.get2() != null ? Basic.toString3Int(result.get2()) : "default") + " target='" + target + "';");
    }

    public boolean isApplicable() {
        ChartViewer viewer = (ChartViewer) getViewer();
        return viewer != null;
    }

    public String getName() {
        return "Legend Font...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Set the font used for values";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
