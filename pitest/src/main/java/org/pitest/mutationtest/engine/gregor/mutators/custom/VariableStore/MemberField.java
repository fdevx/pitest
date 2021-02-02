package org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MemberField extends IVariable {
  final Field field;

  public MemberField(Type t, String name, Field f) {
    super(t, name);
    field = f;
  }

  @Override
  public String getDescription() {
    return "member field \"" + name + "\"";
  }

  @Override
  public int getLoadOpcode() {
    return isStatic() ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
  }

  @Override
  public int getStoreOpcode() {
    return isStatic() ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
  }

  @Override
  public boolean isFinal() {
    return Modifier.isFinal(field.getModifiers());
  }

  @Override
  public boolean isStatic() {
    return Modifier.isStatic(field.getModifiers());
  }

  @Override
  public boolean isTemporary() {
    return false;
  }

  @Override
  public void visit(final VisitType vType, final MethodVisitor mv) {
    int opcode = vType == VisitType.STORE ? getStoreOpcode() : getLoadOpcode();
    mv.visitFieldInsn(opcode, field.getDeclaringClass().getName().replaceAll("\\.", "/"), name, type.getDescriptor());
  }

  @Override
  boolean needsThisReference() {
    return !isStatic();
  }

  @Override
  protected Type getThisRefType() {
    return isStatic() ? null : Type.getType(field.getDeclaringClass());
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
    final MemberField that = (MemberField) o;
    return field.equals(that.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), field);
  }

  @Override
  public String toString() {
    return "member field \"" + name + "\"";
  }
}
