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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.kaazing.nuklei.Nukleus;
import org.kaazing.nuklei.Reaktive;

@SupportedAnnotationTypes({"org.kaazing.nuklei.Reaktive"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ReaktiveAnnotationProcessor extends AbstractProcessor
{
    private static final String ERROR_CLASS_NOT_EXCEPTION_OR_NUKLEUS =
            "@Reaktive class must extend Throwable or implement Nukleus interface";

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv)
    {
        validateReaktive(roundEnv.getElementsAnnotatedWith(Reaktive.class));

        return false;
    }

    private void validateReaktive(
        Set<? extends Element> elements)
    {
        for (Element element : elements)
        {
            switch (element.getKind())
            {
            case ANNOTATION_TYPE:
            case CLASS:
            case ENUM:
            case INTERFACE:
                validateReaktiveClass(element);
                break;

            default:
                break;
            }
        }
    }

    private void validateReaktiveClass(
        Element clazz)
    {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();
        Messager messager = processingEnv.getMessager();

        TypeElement throwableElement = elements.getTypeElement(Throwable.class.getName());
        TypeElement nukleusElement = elements.getTypeElement(Nukleus.class.getName());
        if (!types.isSubtype(clazz.asType(), throwableElement.asType()) &&
                !types.isSubtype(clazz.asType(), nukleusElement.asType()))
        {
            messager.printMessage(Diagnostic.Kind.ERROR, ERROR_CLASS_NOT_EXCEPTION_OR_NUKLEUS);
        }
    }
}
