package buidergenerator;

import buidergenerator.internal.AbstractBuilder;
import buidergenerator.internal.BuilderClassInfo;
import com.squareup.javapoet.*;

import java.util.List;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class JavaFiler {

    static TypeSpec brewJava(BuilderClassInfo builderClassInfo) {
        return TypeSpec.classBuilder(builderClassInfo.className)
                .addModifiers(PUBLIC)
                .superclass(ParameterizedTypeName.get(ClassName.get(AbstractBuilder.class), ClassName.get(builderClassInfo.typeElement)))
                .addFields(createBuilderFields(builderClassInfo))
                .addMethods(createBuilderFieldMethods(builderClassInfo))
                .addMethod(createBuildMethod(builderClassInfo))
                .build();
    }

    private static List<FieldSpec> createBuilderFields(BuilderClassInfo builderClassInfo) {
        return builderClassInfo.fields.stream()
                .map(field -> FieldSpec
                        .builder(TypeName.get(field.asType()), field.getSimpleName().toString(), PRIVATE)
                        .build())
                .collect(Collectors.toList());
    }

    private static List<MethodSpec> createBuilderFieldMethods(BuilderClassInfo builderClassInfo) {
        return builderClassInfo.fields.stream()
                .map(field -> MethodSpec.methodBuilder(field.getSimpleName().toString())
                        .addModifiers(PUBLIC)
                        .addParameter(TypeName.get(field.asType()), field.getSimpleName().toString())
                        .addStatement(
                                "this.$N = $N",
                                field.getSimpleName().toString(),
                                field.getSimpleName().toString()
                        )
                        .addStatement("return this")
                        .returns(ClassName.get(builderClassInfo.packageName, builderClassInfo.className))
                        .build())
                .collect(Collectors.toList());
    }

    private static MethodSpec createBuildMethod(BuilderClassInfo builderClassInfo) {
        if(builderClassInfo.buildByAllArgsConstructor) {
            return createBuildMethodByAllArgsConstructor(builderClassInfo);
        } else {
            return createBuildMethodBySetterMethods(builderClassInfo);
        }
    }

    private static MethodSpec createBuildMethodByAllArgsConstructor(BuilderClassInfo builderClassInfo) {
        return MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addCode(createConstructorCodeBlock(builderClassInfo))
                .returns(TypeName.get(builderClassInfo.element.asType()))
                .build();
    }

    private static MethodSpec createBuildMethodBySetterMethods(BuilderClassInfo builderClassInfo) {
        return MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addStatement(
                        "$T builderProduct = new $T()",
                        builderClassInfo.element.asType(),
                        builderClassInfo.element.asType()
                )
                .addCode(createSetterMethodsCodeBlock(builderClassInfo))
                .addStatement("return builderProduct")
                .returns(TypeName.get(builderClassInfo.element.asType()))
                .build();
    }

    private static CodeBlock createConstructorCodeBlock(BuilderClassInfo builderClassInfo) {
        String format = createConstructorCodeBlockFormat(builderClassInfo);
        Object[] args = createConstructorCodeBlockArgs(builderClassInfo);
        return CodeBlock.of(format, args);
    }

    private static String createConstructorCodeBlockFormat(BuilderClassInfo builderClassInfo) {
        StringBuilder format = new StringBuilder("return new $T(");
        int length = builderClassInfo.fields.size();

        for(int i = 0; i < length; i++) {
            if(i == length -1) {
                format.append("$N");
            } else {
                format.append("$N, ");
            }
        }

        format.append(");\n");

        return format.toString();
    }

    private static Object[] createConstructorCodeBlockArgs(BuilderClassInfo builderClassInfo) {
        Object[] args = new Object[builderClassInfo.fields.size() + 1];

        int argsLength = args.length;

        args[0] = builderClassInfo.element.asType();

        for(int i = 1; i < argsLength; i++) {
            args[i] = builderClassInfo.fields.get(i - 1).getSimpleName().toString();
        }

        return args;
    }

    private static CodeBlock createSetterMethodsCodeBlock(BuilderClassInfo builderClassInfo) {
        String format = createSetterMethodsCodeBlockFormat(builderClassInfo);
        return CodeBlock.of(format);
    }

    private static String createSetterMethodsCodeBlockFormat(BuilderClassInfo builderClassInfo) {
        StringBuilder format = new StringBuilder();

        builderClassInfo.fields.forEach(field -> {
            String fieldName = field.getSimpleName().toString();
            String setterMethod = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            format.append("builderProduct.")
                    .append(setterMethod)
                    .append("(")
                    .append(fieldName)
                    .append(");\n");
        });

        return format.toString();
    }

}