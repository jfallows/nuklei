/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.nuklei.internal;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;

@SupportedAnnotationTypes("org.kaazing.nuklei.Reaktive")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ReaktiveAnnotationProcessor extends AbstractProcessor
{
    private static final String ERROR_CLASS_NOT_NUKLEUS = "@Reaktive enclosing class must implement Nukleus interface";
    private static final String ERROR_METHOD_PRIVATE = "@Reaktive method scope must not be private";
    private static final String ERROR_METHOD_NOT_RETURNING_VOID = "@Reaktive method return type must be void";
    private static final String ERROR_METHOD_THROWS_EXCEPTIONS = "@Reaktive method must not throw exceptions";

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv)
    {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Reaktive.class);

        for (Element element : elements)
        {
            switch (element.getKind())
            {
            case METHOD:
                ExecutableElement method = (ExecutableElement) element;
                validateMethod(method);
                TypeElement clazz = (TypeElement) method.getEnclosingElement();
                validateClass(clazz);
                break;

            default:
                break;
            }
        }

        return false;
    }

    private void validateMethod(
        ExecutableElement method)
    {
        Messager messager = processingEnv.getMessager();

        if (method.getModifiers().contains(Modifier.PRIVATE))
        {
            messager.printMessage(Diagnostic.Kind.ERROR, ERROR_METHOD_PRIVATE);
        }

        if (method.getReturnType().getKind() != TypeKind.VOID)
        {
            messager.printMessage(Diagnostic.Kind.ERROR, ERROR_METHOD_NOT_RETURNING_VOID);
        }

        if (!method.getThrownTypes().isEmpty())
        {
            messager.printMessage(Diagnostic.Kind.ERROR, ERROR_METHOD_THROWS_EXCEPTIONS);
        }
    }

    private void validateClass(
        Element clazz)
    {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();
        Messager messager = processingEnv.getMessager();

        TypeElement nukleusElement = elements.getTypeElement(Nukleus.class.getName());
        if (!types.isSubtype(clazz.asType(), nukleusElement.asType()))
        {
            messager.printMessage(Diagnostic.Kind.ERROR, ERROR_CLASS_NOT_NUKLEUS);
        }
    }
}
