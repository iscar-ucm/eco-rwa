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

import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author jlrisco
 */
public class BinaryNode<T> {

  protected BinaryNode<T> parent;
  protected T value;
  protected BinaryNode<T> left;
  protected BinaryNode<T> right;

  public BinaryNode(T value, BinaryNode<T> left, BinaryNode<T> right) {
    this.parent = null;
    this.value = value;
    this.setLeft(left);
    this.setRight(right);
  }

  public BinaryNode(T value) {
    this(value, null, null);
  }

  public BinaryNode<T> getParent() {
    return parent;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }
  
  public BinaryNode<T> getLeft() {
    return left;
  }

  public void setLeft(BinaryNode<T> left) {
    if (left != null) {
      left.parent = this;
    }
    if (this.left != null) {
      this.left.parent = null;
    }
    this.left = left;
  }

  public BinaryNode<T> getRight() {
    return right;
  }

  public void setRight(BinaryNode<T> right) {
    if (right != null) {
      right.parent = this;
    }
    if (this.right != null) {
      this.right.parent = null;
    }
    this.right = right;
  }

  public boolean isOperand(Set<T> operators) {
    return (operators.contains(value)) ? false : true;
  }

  public boolean isOperator(Set<T> operators) {
    return !isOperand(operators);
  }

  public BinaryNode<T> commonAncestor(BinaryNode<T> node2) {
    if (parent == null) {
      return null;
    }
    if (parent.contains(node2)) {
      return parent;
    }
    return parent.commonAncestor(node2);
  }

  public boolean contains(BinaryNode<T> node) {
    if (this == node) {
      return true;
    }
    boolean containsLeft = false;
    boolean containsRight = false;
    if (this.left != null) {
      containsLeft = left.contains(node);
    }
    if (this.right != null) {
      containsRight = right.contains(node);
    }
    return containsLeft || containsRight;
  }

  public LinkedList<BinaryNode<T>> getNodesAtLevel(int level) {
    LinkedList<BinaryNode<T>> res = new LinkedList<BinaryNode<T>>();
    if (level == 0) {
      res.add(this);
    }
    else {
      if(this.left != null)
        res.addAll(this.left.getNodesAtLevel(level-1));
      if(this.right != null)
        res.addAll(this.right.getNodesAtLevel(level-1));
    }
    return res;
  }

  public String traverseInOrder() {
    StringBuilder buffer = new StringBuilder();
    if (this.left != null) {
      buffer.append(this.left.traverseInOrder());
    }
    buffer.append(this.value.toString()).append(" ");
    if (this.right != null) {
      buffer.append(this.right.traverseInOrder());
    }
    return buffer.toString();
  }

  public String traversePostOrder() {
    StringBuilder buffer = new StringBuilder();
    if (this.left != null) {
      buffer.append(this.left.traversePostOrder());
    }
    if (this.right != null) {
      buffer.append(this.right.traversePostOrder());
    }
    buffer.append(this.value.toString()).append(" ");
    return buffer.toString();
  }

  @Override
  public String toString() {
    String temp = this.traversePostOrder();
    return temp.substring(0, temp.length()-1); // Remove last white space
  }
}
