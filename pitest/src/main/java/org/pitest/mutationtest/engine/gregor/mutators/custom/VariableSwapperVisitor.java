package org.pitest.mutationtest.engine.gregor.mutators.custom;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.pitest.bytecode.ASMVersion;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.ChildVarStore;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.IVariable;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.LocalVariable;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.MemberField;
import org.pitest.mutationtest.engine.gregor.mutators.custom.VariableStore.VariableStore;

public class VariableSwapperVisitor extends MethodVisitor {
  protected final MethodMutatorFactory factory;
  protected final MutationContext context;
  protected final MethodInfo methodInfo;
  protected final VariableStore varStore;
  protected Map<Integer,List<Runnable>> localVarDescToChange;

  private int curLineNum = -1;

  VariableSwapperVisitor(final MutationContext context, final MethodVisitor writer,
                         final MethodMutatorFactory factory, final MethodInfo methodInfo,
                         final ChildVarStore classFields) {
    super(ASMVersion.ASM_VERSION, writer);
    this.factory = factory;
    this.context = context;
    this.methodInfo = methodInfo;
    this.localVarDescToChange = new HashMap<>();

    this.varStore = new VariableStore();
    varStore.addChildStore(classFields);

    // never getMethodDescription() (pretty prints method) always use getMethodDescriptor() to get argument types!!
    Type[] argsForMeth = Type.getArgumentTypes(this.methodInfo.getMethodDescriptor());
    for (int i = 0; i < argsForMeth.length; i++) {
      Type t = argsForMeth[i];
      varStore.addVar(new LocalVariable(t, "arg" + i, null, i));
      // System.out.println("Adding argument variable \"" + i + "\" to the store!");
    }
  }

  @Override
  public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end,
                                 final int index) {
    Type genericType;
    try {
      genericType = signature != null ? Type.getType(signature) : null;
    } catch (IllegalArgumentException e) {
      System.out.println("For " + this.context.getClassInfo().getName() + "::" + this.methodInfo.getDescription() + " variable \""
          + name + "\"(" + index + ") has invalid signature:" + " " + signature);
      genericType = null;
    }

    Optional<IVariable> var = findLocalVarByIndex(index);
    if (var.isPresent()) {
      // variable already exists we should update its data to match what we got here
      // (meaning we remove the guess work)

      // System.out.println("Updating local variable \"" + index + "\" (\"" + name + "\") in the store!");
      var.get().setName(name);
      var.get().setType(Type.getType(descriptor));
      var.get().setGenericType(Optional.ofNullable(genericType));
      ((LocalVariable)var.get()).setTemporary(false);
    } else {
      // System.out.println("Adding local variable \"" + index + "\" (\"" + name + "\") to the store!");
      varStore.addVar(new LocalVariable(Type.getType(descriptor), name, genericType, index));
    }

    if (localVarDescToChange.containsKey(index)) {
      for (Runnable r : localVarDescToChange.get(index)) {
        r.run();
      }
    }

    super.visitLocalVariable(name, descriptor, signature, start, end, index);
  }

  void addToDescChangeList(int indx, Runnable r) {
    if (!localVarDescToChange.containsKey(indx)) {
      localVarDescToChange.put(indx, new ArrayList<>());
    }
    localVarDescToChange.get(indx).add(r);
  }

  void addChaneDescForLocalVar(String desc, IVariable a, IVariable b) {
    if (a.isTemporary() && a instanceof LocalVariable) {
      addToDescChangeList(((LocalVariable) a).getIndex(), () -> changeString(desc, getDescription(a,b)));
    }
    if (b.isTemporary() && b instanceof LocalVariable) {
      addToDescChangeList(((LocalVariable) b).getIndex(), () -> changeString(desc, getDescription(a,b)));
    }
  }

  /*
   * I just wanted a way to know which variables get modified in what way
   * and the current implementation for descriptions doesnt allow that
   * and I didnt feel like writing a complete new description implementation
   *
   * So here is some magic
   *
   * Dont look we are breaking the Java contract for strings
   * I warned you!
   */
  void changeString(String a, String replace) {
    try {
      final Class<String> type = String.class;
      final Field valueField, hashField;
      valueField = type.getDeclaredField("value");
      hashField = type.getDeclaredField("hash");

      // yes this causes warnings to be printed on the console
      // yes I am aware this probably wont work in the future
      valueField.setAccessible(true);
      hashField.setAccessible(true);

      valueField.set(a, valueField.get(replace));
      hashField.set(a, hashField.get(replace));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      System.out.println("Unable to modify description string of mutation for easier reading!");
      e.printStackTrace();
    }
  }

  String getDescription(IVariable a, IVariable b) {
    return "swapped  " + a.toString() + " with " + b.toString() + " type: \"T-" + a.getType().toString() + "\"";
  }

  Optional<IVariable> findLocalVarByIndex(int indx) {
    return varStore.filter(
        var -> var instanceof LocalVariable
            && ((LocalVariable)var).getIndex() == indx )
        .findFirst();
  }

  Optional<IVariable> findFieldByName(String name, String owner) {
    if (!owner.equals(this.context.getClassInfo().getName())) {
      return Optional.empty();
    }

    return varStore.filter(
        var -> var instanceof MemberField
            && var.getName().equals(name) )
        .findFirst();
  }

  protected Stream<IVariable> findVarWithSameTypeButNotVar(IVariable var) {
    return varStore.filter(v -> !v.equals(var) && v.getType().equals(var.getType()));
  }

  List<IVariable> findReplacementVars(IVariable var) {
    return findVarWithSameTypeButNotVar(var).filter(
        v -> !(v instanceof MemberField)
             || this.methodInfo.isStatic() == v.isStatic()) // cannot set non static member fields in a static method
        .collect(Collectors.toList());
  }

  // TODO H_GETFIELD ** WTH is that????
  boolean isStoreIns(int opcode) {
    switch (opcode) {
      // Static var
      case Opcodes.PUTSTATIC:

      // Field var
      case Opcodes.PUTFIELD:

      case Opcodes.ISTORE:
      case Opcodes.LSTORE:
      case Opcodes.FSTORE:
      case Opcodes.DSTORE:
      case Opcodes.ASTORE:

        // Array opcodes
      case Opcodes.IASTORE:
      case Opcodes.LASTORE:
      case Opcodes.FASTORE:
      case Opcodes.DASTORE:
      case Opcodes.AASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.SASTORE:
        return true;
      default:
        return false;
    }
  }

  boolean isLoadIns(int opcode) {
    switch (opcode) {
      // Static var
      case Opcodes.GETSTATIC:

        // Field var
      case Opcodes.GETFIELD:

      case Opcodes.ILOAD:
      case Opcodes.LLOAD:
      case Opcodes.FLOAD:
      case Opcodes.DLOAD:
      case Opcodes.ALOAD:

        // Array opcodes
      case Opcodes.IALOAD:
      case Opcodes.LALOAD:
      case Opcodes.FALOAD:
      case Opcodes.DALOAD:
      case Opcodes.AALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.SALOAD:
        return true;
      default:
        return false;
    }
  }

  boolean isFieldIns(int opcode) {
    switch (opcode) {
      case Opcodes.GETFIELD:
      case Opcodes.PUTFIELD:
        return true;
      default:
        return false;
    }
  }

  boolean isStaticIns(int opcode) {
    switch (opcode) {
      case Opcodes.GETSTATIC:
      case Opcodes.PUTSTATIC:
        return true;
      default:
        return false;
    }
  }

  boolean isLocalIns(int opcode) {
    return isLoadStoreIns(opcode) && (!isFieldIns(opcode) || isStaticIns(opcode));
  }

  boolean isLoadStoreIns(int opcode) {
    return isStoreIns(opcode) || isLoadIns(opcode);
  }

  Type guessTypeFromOpcode(int opcode) {
    /* We assume here that we only need to guess types for the "store" opcodes
     * since if you load a var you should have used it before which means we
     * would have guessed the type at the previous "store" operation
     *
     * Sounds logical? There is only one slight issue:
     * Parameters dont require a store but use load
     */
    switch (opcode) {
      case Opcodes.ILOAD:
      case Opcodes.ISTORE:
        return Type.INT_TYPE;
      case Opcodes.LLOAD:
      case Opcodes.LSTORE:
        return Type.LONG_TYPE;
      case Opcodes.FLOAD:
      case Opcodes.FSTORE:
        return Type.FLOAT_TYPE;
      case Opcodes.DLOAD:
      case Opcodes.DSTORE:
        return Type.DOUBLE_TYPE;
      case Opcodes.ALOAD:
      case Opcodes.ASTORE:
        return Type.getType(Object.class); // unknown object type

        //Array opcodes
      case Opcodes.IALOAD:
      case Opcodes.IASTORE:
        return Type.getType(int[].class);
      case Opcodes.LALOAD:
      case Opcodes.LASTORE:
        return Type.getType(long[].class);
      case Opcodes.FALOAD:
      case Opcodes.FASTORE:
        return Type.getType(float[].class);
      case Opcodes.DALOAD:
      case Opcodes.DASTORE:
        return Type.getType(double[].class);
      case Opcodes.AALOAD:
      case Opcodes.AASTORE:
        return Type.getType(Object[].class);
      case Opcodes.BALOAD:
      case Opcodes.BASTORE:
        return Type.getType(boolean[].class);
      case Opcodes.CALOAD:
      case Opcodes.CASTORE:
        return Type.getType(char[].class);
      case Opcodes.SALOAD:
      case Opcodes.SASTORE:
        return Type.getType(short[].class);

      default:
        throw new RuntimeException("Unable to guess type from opcode \"" + opcode + "\"!");
    }
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
    boolean didMutate = false;
    if (isLoadStoreIns(opcode) && Objects.equals(owner, this.context.getClassInfo().getName())) { // safety check
      Optional<IVariable> var = findFieldByName(name, owner);

      if (var.isPresent()) {
        didMutate = mutateVarLoadStore(opcode, var.get(), false);
      } else {
        // System.out.println("Unable to find Field \"" + name + "\" in variable store! Maybe this field belongs to a different class?"
        //     + " (Cur class: \"" + this.context.getClassInfo().getName() + "\"  Field class: \"" + owner + "\")");
        try {
          Class<?> c = Class.forName(owner.replaceAll("/", "."));
          IVariable tmpVar = new MemberField(Type.getType(descriptor), name, c.getDeclaredField(name));
          didMutate = mutateVarLoadStore(opcode, tmpVar, false);
        } catch (Exception ignored) {

        }
      }
    }

    if (!didMutate) {
      // ignore this opcode / mutation got skipped
      super.visitFieldInsn(opcode, owner, name, descriptor);
    }
  }

  @Override
  public void visitVarInsn(final int opcode, final int varIndx) {
    boolean didMutate = false;
    if (isLoadStoreIns(opcode) && (varIndx != 0 || this.methodInfo.isStatic())) { // safety check dont change "this"
      Optional<IVariable> var = findLocalVarByIndex(varIndx);

      if (var.isPresent()) {
        didMutate = mutateVarLoadStore(opcode, var.get(), false);
      } else {
        Type t = guessTypeFromOpcode(opcode);

        // System.out.println("Unable to find local variable \"" + varIndx + "\" in variable store! Adding... (guessed type: \"" + t.getClassName() + "\")");

        IVariable localVar = new LocalVariable(t, varIndx);
        varStore.addVar(localVar);
        didMutate = mutateVarLoadStore(opcode, localVar, isStoreIns(opcode));
      }
    }

    if (!didMutate) {
      // ignore the "this" variable / opcode / mutation got skipped
      super.visitVarInsn(opcode, varIndx);
    }
  }

  public boolean loadDefaultValue(IVariable v) {
    int loadConstOpcode = -1;
    switch (v.getType().getSort()) {
      case Type.INT:
        loadConstOpcode = Opcodes.ICONST_0;
        break;
      case Type.LONG:
        loadConstOpcode = Opcodes.LCONST_0;
        break;
      case Type.FLOAT:
        loadConstOpcode = Opcodes.FCONST_0;
        break;
      case Type.DOUBLE:
        loadConstOpcode = Opcodes.DCONST_0;
        break;
      case Type.OBJECT:
        // TODO check for NotNull annotations!!
        loadConstOpcode = Opcodes.ACONST_NULL;
        break;
    }
    if (loadConstOpcode > 0) {
      this.mv.visitInsn(loadConstOpcode);
      return true;
    } else {
      return false;
    }
  }

  /**
   *
   * @param opcode opcode
   * @param var variable that the opcode references (nonnull)
   * @param firstAssignment true if the calling method couldn't find the var in the variable store
   *                        and just created it (and opcode is not a load instruction)
   * @return true on successful mutation false on failure
   */
  boolean mutateVarLoadStore(int opcode, IVariable var, boolean firstAssignment) {
    // if we swap assignment for a final variable we also need to assign it a variable ourself
    firstAssignment |= isStoreIns(opcode) && var.isFinal();

    //dont mutate enum constructor so we pass TestGregorMutater.shouldNotMutateCodeGeneratedByCompilerToImplementEnums
    if (this.context.getClassInfo().isEnum() && this.methodInfo.isConstructor()) {
      return false;
    }

    List<IVariable> replacements = findReplacementVars(var);

    if (isStoreIns(opcode)) {
      // filter out final fields since we shouldn't write to them
      replacements = replacements.stream().filter(v -> !v.isFinal()).collect(Collectors.toList());
    }

    if (replacements.size() > 0) {
      // TODO search for alternative selection methods - probably not gonna do

      IVariable replacementVar = replacements.get(replacements.size() - 1);

      String desc = getDescription(var,replacementVar);
      addChaneDescForLocalVar(desc, var,replacementVar);

      MutationIdentifier newId = this.context.registerMutation(this.factory, desc);

      if (this.context.shouldMutate(newId)) {
        replacementVar.prepareStack(var, this.mv);
        if (isLoadIns(opcode)) {
          replacementVar.visit(IVariable.VisitType.LOAD, this.mv);
        } else {
          replacementVar.visit(IVariable.VisitType.STORE, this.mv);
        }

        if (firstAssignment) {
          if (loadDefaultValue(var)) {
            var.visit(IVariable.VisitType.STORE, this.mv);
          } else {
            // System.out.println("Unable to initialize " + var.getDescription());
          }
        }
        return true;
      }
    }

    return false;
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    super.visitLineNumber(line, start);
    curLineNum = line;
  }

}
