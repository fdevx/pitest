package org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore;

import java.util.Objects;
import java.util.Optional;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public abstract class IVariable {
  public enum VisitType {
    LOAD, STORE
  };

  Type type;
  String name;
  Optional<Type> genericType;

  public IVariable(Type t, String name, Type genericType) {
    this.type = t;
    this.name = name;
    this.genericType = Optional.ofNullable(genericType);
  }

  public IVariable(Type t, String name) {
    this(t, name, null);
  }


  /*
   * Setters needed in order to update some of the guess work to "real" data
   */

  public void setType(final Type type) {
    this.type = type;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setGenericType(final Optional<Type> genericType) {
    this.genericType = genericType;
  }


  /*
   * Getter methods for Variables
   */

  public Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public abstract String getDescription();

  public abstract int getLoadOpcode();

  public abstract int getStoreOpcode();

  public abstract boolean isFinal();

  public abstract boolean isStatic();

  // if this var entry was created by guessing stuff and not via visitFieldInsn or visitLocalVariable
  // meaning the "name" is probably just "varX" =>
  // the description off the mutation doesnt help anyone understand what happened
  public abstract boolean isTemporary();

  /*
   * Byte code generating methods
   */

  // generate code to load or store this variable
  public abstract void visit(VisitType vType, MethodVisitor mv);

  public void prepareStack(IVariable prev, MethodVisitor mv) {
    if (prev != null && prev.needsThisReference() != this.needsThisReference()) {
      if (prev.needsThisReference()) {
        // remove "this" reference form stack
        // Stack probably looks like this
        // TOP
        //    <someValue>
        //    this

        // swap "this" reference to the top of the stack and pop it
        Type thisRefType = prev.getThisRefType();
        swap(mv, thisRefType, prev.type);
        if (thisRefType.getSize() == 1) {
          mv.visitInsn(Opcodes.POP);
        } else {
          mv.visitInsn(Opcodes.POP2);
        }
      } else {
        // add "this" reference to the stack
        // Stack probably looks like this
        // TOP
        //    <someValue>
        //    -- we want to insert this here

        // push "this" on the stack and swap it with "<someValue>"
        mv.visitIntInsn(Opcodes.ALOAD, 0);
        swap(mv, this.getThisRefType(), prev.type);
      }
    } else if (prev == null && this.needsThisReference()) {
      // push "this" on the stack
      mv.visitIntInsn(Opcodes.ALOAD, 0);
    }
  }

  // needed for prepareStack
  abstract boolean needsThisReference();

  // needed for prepareStack
  protected abstract Type getThisRefType();

  // helper method for prepareStack
  // based on: http://stackoverflow.com/a/11359551
  protected static void swap(final MethodVisitor mv, final Type stackTop, final Type belowTop) {
    if (stackTop.getSize() == 1) {
      if (belowTop.getSize() == 1) {
        // Top = 1, below = 1
        mv.visitInsn(Opcodes.SWAP);
      } else {
        // Top = 1, below = 2
        mv.visitInsn(Opcodes.DUP_X2);
        mv.visitInsn(Opcodes.POP);
      }
    } else {
      if (belowTop.getSize() == 1) {
        // Top = 2, below = 1
        mv.visitInsn(Opcodes.DUP2_X1);
      } else {
        // Top = 2, below = 2
        mv.visitInsn(Opcodes.DUP2_X2);
      }
      mv.visitInsn(Opcodes.POP2);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final IVariable var = (IVariable) o;
    return type.equals(var.type) && name.equals(var.name) && genericType.equals(var.genericType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name, genericType);
  }
}
