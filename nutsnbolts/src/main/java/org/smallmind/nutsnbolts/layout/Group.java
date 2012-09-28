package org.smallmind.nutsnbolts.layout;

import java.util.Iterator;
import java.util.LinkedList;

public abstract class Group<C, G extends Group> {

  private ParaboxLayout<C> layout;
  private Bias bias;
  private LinkedList<ParaboxElement<?>> elements = new LinkedList<ParaboxElement<?>>();

  protected Group (ParaboxLayout<C> layout, Bias bias) {

    this.layout = layout;
    this.bias = bias;
  }

  protected abstract void doLayout (double containerPosition, double containerMeasurement);

  protected ParaboxLayout<C> getLayout () {

    return layout;
  }

  public Bias getBias () {

    return bias;
  }

  protected LinkedList<ParaboxElement<?>> getElements () {

    return elements;
  }

  public synchronized void add (C component) {

    add(component, ParaboxConstraint.immutable());
  }

  public synchronized void add (C component, Spec spec) {

    add(component, spec.staticConstraint());
  }

  public synchronized void add (C component, ParaboxConstraint constraint) {

    elements.add(layout.getContainer().constructElement(component, constraint));
  }

  public synchronized void add (Group<C, ?> group) {

    add(group, ParaboxConstraint.immutable());
  }

  public synchronized void add (Group<C, ?> group, Spec spec) {

    add(group, spec.staticConstraint());
  }

  public synchronized void add (Group<C, ?> group, ParaboxConstraint constraint) {

    elements.add(new GroupParaboxElement<Group>(group, constraint));
  }

  public synchronized void remove (C component) {

    Iterator<ParaboxElement<?>> elementIter = elements.iterator();

    while (elementIter.hasNext()) {
      if (elementIter.next().getComponent().equals(component)) {
        elementIter.remove();
        break;
      }
    }
  }
}
