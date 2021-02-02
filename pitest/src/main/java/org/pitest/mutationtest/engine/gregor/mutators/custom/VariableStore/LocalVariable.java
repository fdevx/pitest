package org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore;

import java.util.Objects;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class LocalVariable extends IVariable {
  final int index;

  boolean temporary;

  public LocalVariable(Type t, final int indx) {
    super(t, "var" + indx, null);
    index = indx;
    temporary = true;
  }

  public LocalVariable(Type t, String name, Type genericType, final int indx) {
    super(t, name, genericType);
    index = indx;
    temporary = false;
  }

  /*
   * Setter needed so we know this variable inst based on guesswork
   */
  public void setTemporary(final boolean temporary) {
    this.temporary = temporary;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public String getDescription() {
    return "local variable \"" + name + "\" (\"" + index + "\")";
  }

  @Override
  public int getLoadOpcode() {
    return type.getOpcode(Opcodes.ILOAD);
  }

  @Override
  public int getStoreOpcode() {
    return type.getOpcode(Opcodes.ISTORE);
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isTemporary() {
    return temporary;
  }

  @Override
  public void visit(final VisitType vType, final MethodVisitor mv) {
    int opcode = vType == VisitType.STORE ? getStoreOpcode() : getLoadOpcode();
    mv.visitVarInsn(opcode, index);
  }

  @Override
  boolean needsThisReference() {
    return false;
  }

  @Override
  protected Type getThisRefType() {
    return null;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final LocalVariable localVar = (LocalVariable) o;
    return index == localVar.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), index);
  }

  @Override
  public String toString() {
    return "local variable \"" + name + "\"";
  }
}
