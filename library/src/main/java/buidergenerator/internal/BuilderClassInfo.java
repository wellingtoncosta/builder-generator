package buidergenerator.internal;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;

public class BuilderClassInfo {

    public final Element element;
    public final TypeElement typeElement;
    public final String packageName;
    public final String className;
    public final boolean buildByAllArgsConstructor;
    public final List<Element> fields;

    public BuilderClassInfo(Element element, String packageName, boolean buildByAllArgsConstructor, List<Element> fields) {
        this.element = element;
        this.typeElement = (TypeElement) element;
        this.packageName = packageName;
        this.className = element.getSimpleName().toString() + "Builder";
        this.buildByAllArgsConstructor = buildByAllArgsConstructor;
        this.fields = fields;
    }

}