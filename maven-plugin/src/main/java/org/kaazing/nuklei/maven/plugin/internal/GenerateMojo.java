/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.nuklei.maven.plugin.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstSpecificationNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstStructNode;
import org.kaazing.nuklei.maven.plugin.internal.ast.AstType;
import org.kaazing.nuklei.maven.plugin.internal.ast.parse.AstParser;
import org.kaazing.nuklei.maven.plugin.internal.ast.visit.ScopeVisitor;
import org.kaazing.nuklei.maven.plugin.internal.generate.FlyweightGenerator;
import org.kaazing.nuklei.maven.plugin.internal.generate.ListFlyweightGenerator;
import org.kaazing.nuklei.maven.plugin.internal.generate.StringFlyweightGenerator;
import org.kaazing.nuklei.maven.plugin.internal.generate.TypeResolver;
import org.kaazing.nuklei.maven.plugin.internal.generate.TypeSpecGenerator;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiLexer;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser;
import org.kaazing.nuklei.maven.plugin.internal.parser.NukleiParser.SpecificationContext;

import com.squareup.javapoet.JavaFile;

@Mojo(name = "generate",
      defaultPhase = GENERATE_SOURCES,
      requiresDependencyResolution = COMPILE,
      requiresProject = true)
@SuppressWarnings("squid:S1192")
public final class GenerateMojo extends org.apache.maven.plugin.AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "src/main/nuklei")
    private File inputDirectory;

    @Parameter(defaultValue = "src/main/resources/META-INF/nuklei")
    private File metaDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/nuklei")
    private File outputDirectory;

    @Parameter(defaultValue = "")
    private String packageName;

    @Parameter(required = true)
    private String scopeNames;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            executeImpl();
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Unable to generate sources", e);
        }
    }

    private void executeImpl() throws IOException
    {
        List<String> targetScopes = unmodifiableList(asList(scopeNames.split("\\s+")));
        TypeResolver resolver = new TypeResolver(packageName);
        ClassLoader loader = createLoader();

        List<AstSpecificationNode> specifications = new LinkedList<>();
        SortedSet<String> parsedResourceNames = new TreeSet<>();
        Set<String> remainingScopes = new LinkedHashSet<>(targetScopes);
        while (!remainingScopes.isEmpty())
        {
            String remainingScope = remainingScopes.iterator().next();
            remainingScopes.remove(remainingScope);
            String resourceName = remainingScope.replaceAll("([^:]+).*", "$1.idl");
            if (parsedResourceNames.add(resourceName))
            {
                getLog().debug("loading: " + resourceName);

                URL resource = loader.getResource(resourceName);
                if (resource == null)
                {
                    getLog().warn(String.format("Resource %s not found", resourceName));
                    continue;
                }

                AstSpecificationNode specification = parseSpecification(resourceName, resource);
                specifications.add(specification);

                resolver.visit(specification);

                Set<String> referencedTypes = specification.accept(new ReferencedTypeResolver());
                getLog().debug("referenced types: " + referencedTypes);

                String regex = "((:?[^:]+(?:\\:\\:[^:]+)*)?)\\:\\:[^:]+";
                Set<String> referencedScopes = referencedTypes.stream()
                                                              .map(t -> t.replaceAll(regex, "$1"))
                                                              .collect(toSet());
                getLog().debug("referenced scopes: " + referencedScopes);

                remainingScopes.addAll(referencedScopes);
            }
        }

        Collection<TypeSpecGenerator<?>> typeSpecs = new HashSet<>();
        for (AstSpecificationNode specification : specifications)
        {
            String scopeName = specification.scope().name();
            ScopeVisitor visitor = new ScopeVisitor(scopeName, packageName, resolver, targetScopes);
            typeSpecs.addAll(specification.accept(visitor));
        }

        typeSpecs.add(new FlyweightGenerator(resolver.resolveClass(AstType.STRUCT)));
        typeSpecs.add(new StringFlyweightGenerator(resolver.resolveClass(AstType.STRUCT)));
        typeSpecs.add(new ListFlyweightGenerator(resolver.resolveClass(AstType.STRUCT)));

        for (TypeSpecGenerator<?> typeSpec : typeSpecs)
        {
            JavaFile sourceFile = JavaFile.builder(typeSpec.className().packageName(), typeSpec.generate())
                    .addFileComment("TODO: license")
                    .skipJavaLangImports(true)
                    .build();
            sourceFile.writeTo(outputDirectory);
        }

        project.addCompileSourceRoot(outputDirectory.getPath());
    }

    private AstSpecificationNode parseSpecification(
        String resourceName,
        URL resource) throws IOException
    {
        try (InputStream input = resource.openStream())
        {
            ANTLRInputStream ais = new ANTLRInputStream(input);
            NukleiLexer lexer = new NukleiLexer(ais);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            NukleiParser parser = new NukleiParser(tokens);
            parser.setErrorHandler(new BailErrorStrategy());

            SpecificationContext ctx = parser.specification();
            return new AstParser().visitSpecification(ctx);
        }
        catch (ParseCancellationException ex)
        {
            Throwable cause = ex.getCause();
            if (cause instanceof RecognitionException)
            {
                RecognitionException re = (RecognitionException) cause;
                Token token = re.getOffendingToken();
                if (token != null)
                {
                    String message = String.format("Parse failed in %s at %d:%d on \"%s\"",
                            resourceName, token.getLine(), token.getCharPositionInLine(), token.getText());
                    getLog().error(message);
                }
            }

            throw ex;
        }
    }

    private ClassLoader createLoader() throws IOException
    {
        List<URL> resourcePath = new LinkedList<>();

        resourcePath.add(inputDirectory.getAbsoluteFile().toURI().toURL());
        resourcePath.add(metaDirectory.getAbsoluteFile().toURI().toURL());

        try
        {
            for (Object resourcePathEntry : project.getTestClasspathElements())
            {
                File reosourcePathFile = new File(resourcePathEntry.toString());
                URI resourcePathURI = reosourcePathFile.getAbsoluteFile().toURI();
                resourcePath.add(URI.create(String.format("jar:%s!/META-INF/nuklei/", resourcePathURI)).toURL());
            }
        }
        catch (DependencyResolutionRequiredException e)
        {
            throw new IOException(e);
        }

        getLog().debug("resource path: " + resourcePath);

        ClassLoader parent = getClass().getClassLoader();
        return new URLClassLoader(resourcePath.toArray(new URL[resourcePath.size()]), parent);
    }

    private static final class ReferencedTypeResolver extends AstNode.Visitor<Set<String>>
    {
        private final Set<String> qualifiedNames = new HashSet<>();

        @Override
        public Set<String> visitStruct(
            AstStructNode structNode)
        {
            String supertype = structNode.supertype();
            if (supertype != null)
            {
                qualifiedNames.add(supertype);
            }

            return super.visitStruct(structNode);
        }

        @Override
        protected Set<String> defaultResult()
        {
            return qualifiedNames;
        }
    }
}
