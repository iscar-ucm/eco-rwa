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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author cia
 */
public class CodifSP {
    protected ArrayList<Sequence> arrayGammaN = new ArrayList<Sequence>();
    protected ArrayList<Sequence> arrayGammaP = new ArrayList<Sequence>();
    
    public void CodifSP(){
        /* Constructor de la clase
         * El resultado son dos 'arraylist'. Uno para la GammaN y otro para la P
         * 1.Se lee el archivo de la variable 'nombre_archivo' y se genera una 
         *   configuracion de floorplan
         * 2.Se pasa componente a componente mirando en qué capa se encuentra
         *   Las capas están numeradas: 1,2,3...
         * 3.Se añade el componente a la secuencia gammaP y gammaN de esa capa
         *   Para ello:
         *      3.1.hay que restar 1 a la capa porque la secuencia empieza en 0
         *      3.2.añadir el ID del componente a la secuencia
         */
        String nombre_archivo = "ejemplo_3layers.xml"; //<-- poner aqui nombre!
        FloorplanConfiguration cfg = new FloorplanConfiguration(
                "profiles" + File.separator + nombre_archivo);
        
        Iterator <Component> iter = cfg.components.values().iterator();
        while (iter.hasNext()) {
            Component c = iter.next(); 
            int position = c.z - 1;  //restar 1 para que empiece en 0
            arrayGammaN.get(position).add(c.id);
            arrayGammaP.get(position).add(c.id);
        }
    }
}

