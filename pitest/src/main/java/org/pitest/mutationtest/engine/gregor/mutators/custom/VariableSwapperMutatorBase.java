package org.pitest.mutationtest.engine.gregor.mutators.custom;

import java.lang.reflect.Field;
import org.objectweb.asm.Type;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.ChildVarStore;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.MemberField;

public interface VariableSwapperMutatorBase {
  static ChildVarStore getFieldsForClass(final MutationContext context) {
    // TODO shareable between all methods of the same class
    final ChildVarStore classFields = new ChildVarStore();
    try {
      Class<?> c = Class.forName(context.getClassInfo().getName().replaceAll("/", "."));
      for (Field f : c.getDeclaredFields()) {
        classFields.addVar(new MemberField(Type.getType(f.getType()), f.getName(), f));
      }
    } catch (ClassNotFoundException ignored) {
      // Do nothing
    }
    return classFields;
  }
}