package io.quarkus.amazon.dynamodb.enhanced.deployment;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.amazon.dynamodb.enhanced.runtime.BeanTableSchemaSubstitutionImplementation;
import io.quarkus.amazon.dynamodb.enhanced.runtime.DynamodbEnhancedClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.gizmo.Gizmo;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

public class DynamodbEnhancedProcessor {
    private static final String FEATURE = "amazon-dynamodb-enhanced";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(DynamodbEnhancedClientProducer.class);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    public void registerClassesForReflectiveAccess(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        //Discover all DynamoDbBean annotated classes and register them
        for (AnnotationInstance i : combinedIndexBuildItem
                .getIndex()
                .getAnnotations(DotName.createSimple(DynamoDbBean.class.getName()))) {
            ClassInfo classInfo = i.target().asClass();
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, classInfo.name().toString()));
        }

        // Register classes which are used by BeanTableSchema but are not found by the classloader
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(true, false, DefaultAttributeConverterProvider.class.getName()));
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(true, false, BeanTableSchemaAttributeTags.class.getName()));

    }

    @BuildStep
    private void applyClassTransformation(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        // We rewrite the bytecode to avoid native-image issues (runtime generated lambdas not supported)
        // and class loader issues (that are only problematic in test and dev mode).
        transformers.produce(
                new BytecodeTransformerBuildItem(
                        BeanTableSchema.class.getName(), new MethodCallRedirectionVisitor()));
    }

    private static class MethodCallRedirectionVisitor
            implements BiFunction<String, ClassVisitor, ClassVisitor> {

        public static final String TARGET_METHOD_OWNER = BeanTableSchemaSubstitutionImplementation.class.getName().replace('.',
                '/');

        @Override
        public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
            return new ClassVisitor(Gizmo.ASM_API_VERSION, outputClassVisitor) {

                @Override
                public MethodVisitor visitMethod(
                        int access, String name, String descriptor, String signature, String[] exceptions) {
                    // https://stackoverflow.com/questions/45180625/how-to-remove-method-body-at-runtime-with-asm-5-2
                    MethodVisitor originalMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (name.equals("newObjectSupplierForClass")) {
                        return new ReplaceMethodBody(
                                originalMethodVisitor,
                                getMaxLocals(descriptor),
                                visitor -> {
                                    visitor.visitCode();
                                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    Type type = Type.getType(descriptor);
                                    visitor.visitMethodInsn(
                                            Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                                    visitor.visitInsn(Opcodes.ARETURN);
                                });
                    } else if (name.equals("getterForProperty")) {
                        return new ReplaceMethodBody(
                                originalMethodVisitor,
                                getMaxLocals(descriptor),
                                visitor -> {
                                    visitor.visitCode();
                                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                    Type type = Type.getType(descriptor);
                                    visitor.visitMethodInsn(
                                            Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                                    visitor.visitInsn(Opcodes.ARETURN);
                                });
                    } else if (name.equals("setterForProperty")) {
                        return new ReplaceMethodBody(
                                originalMethodVisitor,
                                getMaxLocals(descriptor),
                                visitor -> {
                                    visitor.visitCode();
                                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                    Type type = Type.getType(descriptor);
                                    visitor.visitMethodInsn(
                                            Opcodes.INVOKESTATIC, TARGET_METHOD_OWNER, name, type.getDescriptor(), false);
                                    visitor.visitInsn(Opcodes.ARETURN);
                                });
                    } else {
                        return originalMethodVisitor;
                    }
                }

                private int getMaxLocals(String descriptor) {
                    return (Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1;
                }
            };
        }
    }

    private static class ReplaceMethodBody extends MethodVisitor {
        private final MethodVisitor targetWriter;
        private final int newMaxLocals;
        private final Consumer<MethodVisitor> code;

        public ReplaceMethodBody(
                MethodVisitor writer, int newMaxL, Consumer<MethodVisitor> methodCode) {
            super(Opcodes.ASM5);
            this.targetWriter = writer;
            this.newMaxLocals = newMaxL;
            this.code = methodCode;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            targetWriter.visitMaxs(0, newMaxLocals);
        }

        @Override
        public void visitCode() {
            code.accept(targetWriter);
        }

        @Override
        public void visitEnd() {
            targetWriter.visitEnd();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return targetWriter.visitAnnotation(desc, visible);
        }

        @Override
        public void visitParameter(String name, int access) {
            targetWriter.visitParameter(name, access);
        }
    }
}
