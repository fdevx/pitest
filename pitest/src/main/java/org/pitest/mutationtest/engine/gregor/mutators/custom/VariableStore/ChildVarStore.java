package org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ChildVarStore {
  Set<IVariable> vars;

  public ChildVarStore() {
    vars = new HashSet<>();
  }

  public Stream<IVariable> filter(Predicate<? super IVariable> p) {
    return vars.stream().filter(p);
  }

  public void addVar(IVariable v) {
    vars.add(v);
  }
}
