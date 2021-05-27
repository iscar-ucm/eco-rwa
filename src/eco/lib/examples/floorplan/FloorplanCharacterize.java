/*
 * Copyright (C) 2010-2016 José Luis Risco Martín <jlrisco@ucm.es>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
  *  - José Luis Risco Martín
 */
package eco.lib.examples.floorplan;

import java.io.File;

/**
 * Class to calculate wire and temperature from a given XML file.
 * The file describes a floorplan configuration.
 *
 * @author J. M. Colmenar
 */
public class FloorplanCharacterize {

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Parameters: XMLFile [displayZoom]");
            System.out.println(" ---> XMLFile is the floorplanning file");
            System.out.println(" ---> [displayZoom] (optional). If 0, no graphical display\n");

            args = new String[2];
            args[0] = "test_fp" + File.separator + "N3HC48.xml";
            args[1] = "10";
            return;
        }

        String xmlFilePath = args[0];
        FloorplanConfiguration cfg = new FloorplanConfiguration(xmlFilePath);

        double wiring = cfg.computeWireObj();
        double[] temp = cfg.computeTempObj();

        System.out.println("\nFile; "+xmlFilePath+"; Wire Length; "+wiring+"; Temperature; "+temp[0]+"\n");

        if ((args.length == 2) && (!args[1].equals("0"))) {
            int zoom = Integer.valueOf(args[1]);
            Board board = new Board(0, zoom);
            board.buildBoard(xmlFilePath);
        }

    }

}
