package org.pitest.mutationtest.engine.gregor.mutators.custom;

import java.util.List;
import java.util.stream.Collectors;
import org.objectweb.asm.MethodVisitor;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.ChildVarStore;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.IVariable;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.MemberField;

public class VariableSwapperVisitorStaticFieldsOnly extends VariableSwapperVisitor {

  VariableSwapperVisitorStaticFieldsOnly(final MutationContext context, final MethodVisitor writer, final MethodMutatorFactory factory,
                                         final MethodInfo methodInfo, final ChildVarStore classFields) {
    super(context, writer, factory, methodInfo, classFields);
  }

  @Override
  List<IVariable> findReplacementVars(final IVariable var) {
    return findVarWithSameTypeButNotVar(var).filter(v -> v instanceof MemberField && v.isStatic()).collect(Collectors.toList());
  }
}
