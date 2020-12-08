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
package hero.lib.examples.floorplan;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.io.File;
import java.util.Collection;
import javax.swing.JFrame;

/**
 *
 * @author jlrisco
 */
public class Board {

    protected int idxPowerProfile;
    protected int zoom;
    protected FloorplanConfiguration cfg;

    public Board(int idxPowerProfile, int zoom) {
        this.idxPowerProfile = idxPowerProfile;
        this.zoom = zoom;
    }

    public Board(int zoom) {
        this(0, zoom);
    }

    public Board() {
        this(10);
    }

    public void buildBoard(String xmlPath) {
        cfg = new FloorplanConfiguration(xmlPath);
        buildBoard();
    }

    public void buildBoard() {
        Collection<Component> components = cfg.components.values();
        for (int layer = 0; layer < cfg.numLayers; ++layer) {
            mxGraph graph = new mxGraph();
            for (Component currentBlock : components) {

                if (currentBlock.z != layer) {
                    continue;
                }

                double x = currentBlock.x;
                double y = currentBlock.y;
                double l = currentBlock.l;
                double w = currentBlock.w;
                double dpAux = currentBlock.dps[idxPowerProfile] / cfg.maxDP;
                mxCell blockAsCell = new mxCell("[" + String.valueOf(currentBlock.id) + "]");
                if (dpAux < 0.25) {
                    blockAsCell.setStyle("fillColor=blue");
                } else if (dpAux < 0.5) {
                    blockAsCell.setStyle("fillColor=yellow");
                } else if (dpAux < 0.75) {
                    blockAsCell.setStyle("fillColor=orange");
                } else {
                    blockAsCell.setStyle("fillColor=red");
                }
                mxGeometry geometry = new mxGeometry(x * zoom, y * zoom, l * zoom, w * zoom);
                geometry.setRelative(false);
                blockAsCell.setGeometry(geometry);
                blockAsCell.setVertex(true);
                graph.addCell(blockAsCell, graph.getDefaultParent());
            }
            for (ThermalVia thermalVia : cfg.thermalVias) {
                if (thermalVia.zEnd > layer) {
                    continue;
                }
                mxCell blockAsCell = new mxCell();
                blockAsCell.setStyle("fillColor=black");
                mxGeometry geometry = new mxGeometry(thermalVia.x * zoom, thermalVia.y * zoom, 1.0 * zoom, 1.0 * zoom);
                geometry.setRelative(false);
                blockAsCell.setGeometry(geometry);
                blockAsCell.setVertex(true);
                graph.addCell(blockAsCell, graph.getDefaultParent());

            }
            JFrame frame = new JFrame("SOLUTION. LAYER " + layer);
            mxGraphComponent graphComponent = new mxGraphComponent(graph);
            frame.getContentPane().add(graphComponent);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        }

    }

    public static void main(String args[]) {
        if (args.length != 1) {
            args = new String[1];
            args[0] = "lib" + File.separator + "NiagaraC64L5.xml";
        }
        String fileName = args[0];
        Board board = new Board();
        board.buildBoard(fileName);
    }
}
