package org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class VariableStore {
  Set<IVariable> vars;
  Set<ChildVarStore> childStores;

  public VariableStore() {
    vars = new HashSet<>();
    childStores = new HashSet<>();
  }

  public Stream<IVariable> filter(Predicate<? super IVariable> p) {
    Stream<IVariable> childStreams = childStores.stream().map(cS -> cS.filter(p)).reduce(Stream.empty(), Stream::concat);
    return Stream.concat(childStreams, vars.stream().filter(p));
  }

  public void addVar(IVariable v) {
    vars.add(v);
  }

  public void addChildStore(ChildVarStore cs) {
    childStores.add(cs);
  }

}
