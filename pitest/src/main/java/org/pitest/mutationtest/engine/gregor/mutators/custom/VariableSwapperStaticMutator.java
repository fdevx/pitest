package org.pitest.mutationtest.engine.gregor.mutators.custom;

import org.objectweb.asm.MethodVisitor;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

public enum VariableSwapperStaticMutator implements VariableSwapperMutatorBase, MethodMutatorFactory {
  VARIABLE_SWAPPER_STATIC_MUTATOR;

  @Override
  public MethodVisitor create(final MutationContext context, final MethodInfo methodInfo, final org.objectweb.asm.MethodVisitor methodVisitor) {
    return new VariableSwapperVisitorStaticFieldsOnly(
        context, methodVisitor, this, methodInfo,
        VariableSwapperMutatorBase.getFieldsForClass(context));
  }

  @Override
  public String getGloballyUniqueId() {
    return this.getClass().getName();
  }

  @Override
  public String getName() {
    return name();
  }
}
