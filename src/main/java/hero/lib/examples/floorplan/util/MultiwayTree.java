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
package hero.lib.examples.floorplan.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rooted multiway tree where each node of the tree corresponds to an element
 *
 * @author J.M. Colmenar
 */
public class MultiwayTree<T> {
    private T node;
    private MultiwayTree<T> parent;
    private boolean isRoot = false;
    private ArrayList<MultiwayTree<T>> children;

    public static final int FIRST_CHILD = 0;

    // Helpful for travel the tree
    private int visitedNodes = 0;

    public void setRoot(boolean b) {
        if (b) parent = null;
        isRoot = b;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public T getNode() {
        return node;
    }

    public MultiwayTree<T> getParent() {
        return parent;
    }

    public void setNode(T newNode) {
        node = newNode;
    }

    public void setParent(MultiwayTree<T> newParent) {
        parent = newParent;
    }

    @Override
    public String toString() {
        String cad = " "+node+"";
        cad += ": (";
        if (children != null)
            for (int i=0; i<children.size();i++)
                cad += " "+children.get(i).node;

        cad += " ) ";
        if (children != null)
            for (int i=0; i<children.size();i++)
                cad += children.get(i);
        return cad;

    }

    /** Adds a new node as the next children of
     * the tree.
     *
     * @param newNode node to be added
     * @return reference to the subtree created for the new node
     */
    public MultiwayTree<T> addChildren(T newNode) {
        if (children == null)
            children = new ArrayList<MultiwayTree<T>>();

        MultiwayTree<T> newChild = new MultiwayTree<T>();
        newChild.setRoot(false);
        newChild.setParent(this);
        newChild.setNode(newNode);
        children.add(newChild);

        return newChild;
    }

    /** Adds a new node as the i-th children of
     * the tree.
     *
     * @param newNode node to be added
     * @return reference to the subtree created for the new node
     */
    public MultiwayTree<T> addChildren(int index,T newNode) {

        MultiwayTree<T> newChild = null;

        if (children == null || index > children.size()) {
            Logger.getLogger(MultiwayTree.class.getName()).log(Level.SEVERE, "Cannot add children in the desired position ("+index+"). # children: "+children.size());
        } else {
            newChild = new MultiwayTree<T>();
            newChild.setRoot(false);
            newChild.setParent(this);
            newChild.setNode(newNode);
            children.add(index,newChild);
        }     

        return newChild;
    }


    /** Returns the ith child from left to right **/
    public MultiwayTree<T> getChild(int i) {
        return children.get(i);
    }

    public ArrayList<MultiwayTree<T>> getChildren() {
        return children;
    }

     @Override
    public MultiwayTree<T> clone() {
        MultiwayTree<T> cloned = new MultiwayTree<T>();

        // Cannot be cloned !!???
        cloned.setNode(node);
        cloned.setRoot(this.isRoot);

        // Children
        if (this.children != null) {
            ArrayList<MultiwayTree<T>> clonedChildren = new ArrayList<MultiwayTree<T>>();
            for (int i=0; i<this.children.size();i++) {
                MultiwayTree<T> clonedChild = this.children.get(i).clone();
                clonedChild.parent = cloned;
                clonedChildren.add(clonedChild);
            }

            cloned.children = clonedChildren;
        }

        return cloned;
    }


    /**
     * Searches from a node and removes it from the tree.
     * 
     * @param node to be removed
     * @return true if node was found and successfully removed; false otherwise
     */
    public boolean removeNode(T node, MultiwayTree<T> tree) {
        boolean found = false;

        // If node found
        if (tree.getNode() != null && tree.getNode().equals(node)) {

            // The new parent of the children is the tree parent
            if (tree.children != null) {
                for (int i = 0; i<tree.children.size(); i++) {
                    tree.children.get(i).setParent(tree.parent);
                }
                // Insert children in the list of children of the tree parent
                int index = tree.parent.children.indexOf(tree);
                tree.parent.children.addAll(index, tree.children);
                tree.children = null;
            }
            
            // Drop node
            tree.parent.children.remove(tree);
            tree = null;

            found = true;

        } else {

            // Node not found, search into the children:
            if (tree.children != null) {
                Iterator<MultiwayTree<T>> iter = tree.children.iterator();

                while (!found && iter.hasNext()) {
                    found = removeNode(node,iter.next());
                }
            }

        }

        return found;
    }

    /**
     * Returns a reference to the subtree that results in the position
     * given by the parameter considering that nodes are visited using
     * a pre-order sequence. The root has index=0.
     *
     * @param index
     * @param tree current subtree
     * @param visited number of already visited nodes
     * @return subtree
     */
    public MultiwayTree<T> getSubTree(int index, MultiwayTree<T> tree) {
        MultiwayTree<T> subtree = null;

        // If search found
        if (index == visitedNodes) {

            subtree = tree;
            // Reset the counter
            visitedNodes = 0;

        } else {

            // Node not found, search into the children:
            if (tree.children != null) {
                Iterator<MultiwayTree<T>> iter = tree.children.iterator();

                while ((subtree == null) && iter.hasNext()) {
                    visitedNodes++;
                    subtree = getSubTree(index,iter.next());
                }
            }

        }

        return subtree;
    }

    /**
     * Mantaining the current root value, generates a new tree for the given
     * list of nodes.
     *
     * @param nodeList list with the objects that will be nodes
     * @param seed random seed
     */
    public void randomTree(Collection<T> inputNodeList) {
        // Shuffle node list
        ArrayList<T> nodeList = new ArrayList<T>();
        Iterator<T> it = inputNodeList.iterator();
        while (it.hasNext()) nodeList.add(it.next());
        Collections.shuffle(nodeList);

        // Reset current tree maintaining the root
        this.children = null;

        // Start at the root
        MultiwayTree<T> currentNode = this;

        Random rnd = new Random();
        Iterator<T> iter = nodeList.iterator();
        while (iter.hasNext()) {
            T newNode = iter.next();
            if (currentNode.isRoot) {
                // Insert as new child and go down
                currentNode = currentNode.addChildren(newNode);
            } else {
                switch (rnd.nextInt(3)) {
                    case 0:
                        // Insert new node as left brother
                        currentNode.parent.addChildren(currentNode.parent.children.indexOf(currentNode),newNode);
                        break;
                    case 1:
                        // Insert new node as right brother
                        currentNode.parent.addChildren(currentNode.parent.children.indexOf(currentNode)+1,newNode);
                        break;
                    default:
                        // Insert as new child and go down
                        currentNode = currentNode.addChildren(newNode);
                }
            }
        }

    }


    /**
     * Returns the list of nodes from a  breadth-first traversal of the tree.
     */
    public List<T>  breadthFirstTraversal() {
        List<T> elems = new ArrayList<T>();
        
        LinkedList<MultiwayTree<T>> queue = new LinkedList<MultiwayTree<T>>();
        queue.add(this);
        
        MultiwayTree<T> curr = null;
        while (queue.size() > 0) {
            curr = queue.poll();
            elems.add(curr.getNode());
            if ((curr.getChildren() != null) && (curr.getChildren().size() > 0)) 
                queue.addAll(curr.getChildren());
        }
        
        return elems;
    }
    
    
    /**
     * Returns the subtree where the node corresponds to the given node.
     * Performs a breadth-first search.
     */
    public MultiwayTree<T> findNode(T nodeToFind) {
        MultiwayTree<T> result = null;
        
        LinkedList<MultiwayTree<T>> queue = new LinkedList<MultiwayTree<T>>();
        queue.add(this);
        
        MultiwayTree<T> curr = null;
        boolean found = false;
        while ((queue.size() > 0) && !found) {
            curr = queue.poll();
            if (curr.getNode() == nodeToFind) {
                found = true;
                result = curr;
            } else {
                if ((curr.getChildren() != null) && (curr.getChildren().size() > 0)) 
                    queue.addAll(curr.getChildren());                
            }
        }
        
        return result;
    }
    
    /**
     * Test of tree
     *
     * @param args
     * /
    public static void main(String[] args) {

        MultiwayTree<Component> tree = new MultiwayTree<Component>();
        tree.isRoot = true;

       ArrayList<Component> nodeList = new ArrayList<Component>();
       nodeList.add(new Component(0,"a",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       nodeList.add(new Component(0,"b",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       nodeList.add(new Component(0,"c",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       nodeList.add(new Component(0,"d",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       nodeList.add(new Component(0,"e",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       nodeList.add(new Component(0,"f",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));

       tree.randomTree(nodeList);
       
       System.out.println(tree);
       System.out.println(tree.breadthFirstTraversal());


       tree.isRoot = true;
       tree.addChildren(new Component(0,"a",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       tree.addChildren(new Component(0,"b",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       tree.addChildren(new Component(0,"c",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       tree.children.get(1).addChildren(new Component(0,"d",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       tree.children.get(1).getChild(0).addChildren(new Component(0,"e",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       tree.children.get(1).getChild(0).addChildren(new Component(0,"f",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}));
       System.out.println(tree);

       tree.removeNode(new Component(0,"d",-1,0,0,0,0,0,0,0,0,0,0,0,0,new double[]{0.0}),tree);

       System.out.println(tree);
    }

/**/
}
