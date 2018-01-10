package buidergenerator;

import buidergenerator.annotation.Builder;
import buidergenerator.annotation.BuilderConstructorWithAllArgs;
import buidergenerator.internal.BuilderClassInfo;
import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("buidergenerator.annotation.Builder")
public class BuilderGeneratorProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.messager = processingEnvironment.getMessager();
        this.elementUtils = processingEnvironment.getElementUtils();
        this.filer = processingEnvironment.getFiler();
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<BuilderClassInfo> builderClassInfoList = new ArrayList<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            if (!SuperficialValidation.validateElement(element)) continue;

            builderClassInfoList.add(new BuilderClassInfo(
                    element,
                    elementUtils.getPackageOf(element).toString(),
                    hasAllArgsConstructor(element),
                    getFields(element)
                )
            );
        }

        writeJava(builderClassInfoList);

        return false;
    }

    private boolean hasAllArgsConstructor(Element element) {
        return getAllArgsConstructor(getConstructors(element)) != null;
    }

    private List<Element> getConstructors(Element parent) {
        List<Element> constructors = parent.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind().equals(ElementKind.CONSTRUCTOR))
                .filter(constructor -> constructor.getModifiers()
                        .stream()
                        .filter(modifier -> modifier.equals(Modifier.PUBLIC))
                        .count() > 0
                )
                .collect(Collectors.toList());

        if (constructors.size() == 0) {
            logError(parent, "Class %s has no public constructor.", parent.getSimpleName());
            return null;
        }

        return constructors;
    }

    private Element getAllArgsConstructor(List<Element> constructors) {
        Element allArgsConstructor = null;
        List<Element> noArgsConstructors = filterNoArgsConstructors(constructors);
        List<Element> allArgsConstructors = filterAllArgsConstructors(constructors);
        boolean hasAllArgsConstructorWithAnnotation = allArgsConstructors.stream()
                .filter(constructor -> constructor.getAnnotation(BuilderConstructorWithAllArgs.class) != null)
                .collect(Collectors.toList())
                .size() > 0;

        if(noArgsConstructors.size() == 0 && allArgsConstructors.size() > 0 && !hasAllArgsConstructorWithAnnotation) {
            Element classElement = constructors.get(0).getEnclosingElement();

            logError(
                    classElement,
                    "Class %s has constructors with arguments but none are with the @%s annotation.",
                    classElement.getSimpleName().toString(),
                    BuilderConstructorWithAllArgs.class.getSimpleName()
            );
            return null;
        }

        if(allArgsConstructors.size() > 0) {
            allArgsConstructor = allArgsConstructors.get(0);
        }

        return allArgsConstructor;
    }

    private List<Element> filterNoArgsConstructors(List<Element> allConstructors) {
        return allConstructors.stream()
                .filter(constructor -> constructor.asType().accept(noArgsConstructorVisitor, null))
                .collect(Collectors.toList());
    }

    private List<Element> filterAllArgsConstructors(List<Element> allConstructors) {
        return allConstructors.stream()
                .filter(constructor -> constructor.asType().accept(allArgsConstructorVisitor, null))
                .collect(Collectors.toList());
    }

    private static final TypeVisitor<Boolean, Void> noArgsConstructorVisitor = new SimpleTypeVisitor8<Boolean, Void>() {
        public Boolean visitExecutable(ExecutableType t, Void v) {
            return t.getParameterTypes().isEmpty();
        }
    };

    private static final TypeVisitor<Boolean, Void> allArgsConstructorVisitor = new SimpleTypeVisitor8<Boolean, Void>() {
        public Boolean visitExecutable(ExecutableType t, Void v) {
            return !t.getParameterTypes().isEmpty();
        }
    };

    private List<Element> getFields(Element element) {
        List<Element> fields = element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind().equals(ElementKind.FIELD))
                .collect(Collectors.toList());

        validateFields(element, fields);

        return fields;
    }

    private void validateFields(Element element, List<Element> fields) {
        List<Element> setters = getSetterMethods(element);

        fields.forEach(field -> {
            boolean isPrivate = field.getModifiers().stream()
                    .filter(m -> m.equals(Modifier.PRIVATE))
                    .count() > 0;

            boolean hasSetter = setters.stream()
                    .filter(setter -> setter.getSimpleName().toString().toLowerCase().contains(field.getSimpleName().toString()))
                    .count() > 0;

            if(!hasSetter && isPrivate) {
                logError(
                        field,
                        "The %s field of the %s class is private and has no a setter method. Make it accessible or create a setter method for this field.",
                        field.getSimpleName(),
                        field.getEnclosingElement().getSimpleName()
                );
            }
        });
    }

    private List<Element> getSetterMethods(Element element) {
        return element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind().equals(ElementKind.METHOD))
                .filter(e -> e.getModifiers().stream().filter(m -> m.equals(Modifier.PUBLIC)).count() > 0)
                .filter(e -> e.getSimpleName().toString().startsWith("set"))
                .collect(Collectors.toList());
    }

    private void writeJava(List<BuilderClassInfo> builderClassInfoList) {
        builderClassInfoList.forEach(builderClassInfo -> {
            try {
                JavaFile.builder(builderClassInfo.packageName, JavaFiler.brewJava(builderClassInfo))
                        .addFileComment("Generated code from Builder Generator. Do not modify!")
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                logError(builderClassInfo.element, "Unable to generate the builder class.");
            }
        });
    }

    private void logError(Element element, String message, Object... args) {
        logError(element, String.format(message, args));
    }

    private void logError(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

}