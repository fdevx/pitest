package org.pitest.mutationtest.engine.gregor.mutators.custom;


import static org.pitest.mutationtest.engine.gregor.mutators.custom.VariableSwapperFieldMutator.VARIABLE_SWAPPER_FIELD_MUTATOR;
import java.util.concurrent.Callable;
import org.junit.Before;
import org.junit.Test;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.gregor.MutatorTestBase;

public class VariableSwapperMutatorTest extends MutatorTestBase {

  @Before
  public void setupEngineToUseReplaceMethodWithArgumentOfSameTypeAsReturnValueMutator() {
    createTesteeWith(VARIABLE_SWAPPER_FIELD_MUTATOR);
  }

  /**
   * This test is not a real test - it has only been used for development
   */
  @Test
  public void testForDevelpoment() throws Exception {
    final Mutant mutant = getFirstMutant(Scratch.class);
    assertMutantCallableReturns(new Scratch(), mutant,
        2);
  }

  class Scratch implements Callable<Integer> {
    private int someInt;
    //private int anotherField;

    @Override
    public Integer call() throws Exception {
      int a = 2;
      return someInt;
      //return someInt;
    }

    Scratch() {
      someInt = -1;
      //someInt = -1;
      //int a  = 2; // assignment should be replaced with:

      //someInt = 2;
      //int a = 0;


      /*someInt = 0;
      anotherField = 2;*/
      //someInt = a + 1;
    }
  }
}
