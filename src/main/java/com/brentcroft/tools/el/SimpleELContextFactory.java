package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.StringUpcaster;
import lombok.Getter;
import lombok.extern.java.Log;

import jakarta.el.*;
//import javax.el.*;
import java.beans.FeatureDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.lang.String.format;


/**
 * This is a factory for making our own ELContext objects.
 *
 * <p>
 * Its probably more complicated than we need, although it doesn't use a
 * RootPropertyMapper like the SimpleContext class in JUEL.
 *
 * <p>
 * Highly influenced by this article:
 *
 * <pre>
 * http://illegalargumentexception.blogspot.co.uk/2008/04/java-using-el-outside-j2ee.html
 * </pre>
 *
 * <p>
 * Also, worth looking at the source code in JUEL.
 *
 * @author ADobson
 */
@Log
public class SimpleELContextFactory implements ELContextFactory
{
    private final Map< String, Method > mappedFunctions = new HashMap<>();

    public void mapFunction( String unprefixedName, Method staticMethod )
    {
        mappedFunctions.put( prefix( unprefixedName ), staticMethod );
    }

    public void mapFunction( String prefix, String unprefixedName, Method staticMethod )
    {
        mappedFunctions.put( prefix + ":" + unprefixedName, staticMethod );
    }

    public void mapFunctions( Map< String, Method > functions )
    {
        mappedFunctions.putAll( functions );
    }

    {
        /*
         * functions available in EL expressions
         */
        try
        {
            // see:
            // http://docs.oracle.com/javase/6/docs/api/java/util/Formatter.html
            mapFunction( "format", ELFunctions.class.getMethod( "format", String.class, List.class ) );
            mapFunction( "replaceAll", ELFunctions.class.getMethod( "replaceAll", String.class, String.class, String.class ) );


            mapFunction( "parseBytes", ELFunctions.class.getMethod( "bytesAsString", byte[].class, String.class ) );
            mapFunction( "fileExists", ELFunctions.class.getMethod( "fileExists", String.class ) );


            mapFunction( "int", Integer.class.getMethod( "valueOf", String.class ) );
            mapFunction( "double", Double.class.getMethod( "valueOf", String.class ) );
            mapFunction( "pow", Math.class.getMethod( "pow", double.class, double.class ) );

            // capture as float
            mapFunction( "float", ELFunctions.class.getMethod( "boxFloat", Float.class ) );
            mapFunction( "random", ELFunctions.class.getMethod( "random" ) );

            mapFunction( "username", ELFunctions.class.getMethod( "username" ) );

            mapFunction( "uuid", UUID.class.getMethod( "randomUUID" ) );
            mapFunction( "radix", Long.class.getMethod( "toString", long.class, int.class ) );

            mapFunction( "currentTimeMillis", System.class.getMethod( "currentTimeMillis" ) );

            mapFunction( "getTime", ELFunctions.class.getMethod( "getTime", String.class ) );
            mapFunction( "now", ELFunctions.class.getMethod( "now" ) );


            mapFunction( "console", ELFunctions.class.getMethod( "console", String.class, String.class ) );
            mapFunction( "consolePassword", ELFunctions.class.getMethod( "consolePassword", String.class, char[].class ) );
            mapFunction( "consolePasswordAsString", ELFunctions.class.getMethod( "consolePasswordAsString", String.class, String.class ) );
            mapFunction( "consoleFormat", ELFunctions.class.getMethod( "consoleFormat", String.class, Object[].class ) );
            mapFunction( "println", ELFunctions.class.getMethod( "systemOutPrintln", String.class, Object[].class ) );

            mapFunction( "toStringSet", StringUpcaster.class.getMethod( "toStringSet", String.class ) );

            mapFunction( "sort", ELFunctions.class.getMethod( "sort", Collection.class, Comparator.class ) );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to initialise function map", e );
        }

        log.fine( this::listMappedFunctions );
    }

    public String listMappedFunctions()
    {
        return mappedFunctions
                .entrySet()
                .stream()
                .map( entry -> format( "\n  %1$-30s = %2$s", entry.getKey(), entry.getValue() ) )
                .collect( Collectors.joining() );
    }


    public ELContext getELContext( Map< ?, ? > rootObjects )
    {
        return new SimpleELContext( rootObjects );
    }


    public ELContext getELConfigContext()
    {
        return new RootELContext( null );
    }


    class SimpleELContext extends ELContext
    {
        @Getter
        protected final FunctionMapper functionMapper = newFunctionMapper();

        @Getter
        protected final VariableMapper variableMapper = newVariableMapper();

        private final Map< ?, ? > rootObjects;

        protected ELResolver resolver;

        public SimpleELContext( Map< ?, ? > rootObjects )
        {
            this.rootObjects = rootObjects;
        }

        @Override
        public ELResolver getELResolver()
        {
            if ( resolver == null )
            {
                resolver = new CompositeELResolver()
                {
                    {
                        add( new SimpleELResolver( rootObjects ) );
                        add( new ArrayELResolver() );
                        add( new ListELResolver() );
                        add( new BeanELResolver() );
                        add( new MapELResolver() );
                        add( new ResourceBundleELResolver() );
                    }
                };
            }
            return resolver;
        }
    }

    FunctionMapper newFunctionMapper()
    {
        return new FunctionMapper()
        {
            @Override
            public Method resolveFunction( String prefix, String localName )
            {
                return mappedFunctions.get( ( prefix == null ? "" : prefix + ":" ) + localName );
            }
        };
    }

    class RootELContext extends SimpleELContext
    {
        public RootELContext( Map< ?, ? > rootObjects )
        {
            super( rootObjects );
        }
    }


    static class SimpleELResolver extends ELResolver
    {
        private final ELResolver delegate = new MapELResolver();

        private final Map< ?, ? > userMap;

        public SimpleELResolver( Map< ?, ? > rootObjects )
        {
            this.userMap = rootObjects;
        }

        @Override
        public Object getValue( ELContext context, Object base, Object property )
        {
            if ( base == null )
            {
                base = userMap;
            }
            return delegate.getValue( context, base, property );
        }

        @Override
        public Class< ? > getCommonPropertyType( ELContext context, Object arg1 )
        {
            return delegate.getCommonPropertyType( context, arg1 );
        }

        @Override
        public Iterator< FeatureDescriptor > getFeatureDescriptors( ELContext context, Object arg1 )
        {
            return delegate.getFeatureDescriptors( context, arg1 );
        }

        @Override
        public Class< ? > getType( ELContext context, Object arg1, Object arg2 )
        {
            return delegate.getType( context, arg1, arg2 );
        }

        @Override
        public boolean isReadOnly( ELContext context, Object arg1, Object arg2 )
        {
            return delegate.isReadOnly( context, arg1, arg2 );
        }

        @Override
        public void setValue( ELContext context, Object arg1, Object arg2, Object arg3 )
        {
            delegate.setValue( context, arg1, arg2, arg3 );
        }
    }


    static VariableMapper newVariableMapper()
    {
        return new VariableMapper()
        {
            private Map< String, ValueExpression > variableMap = Collections.emptyMap();

            @Override
            public ValueExpression resolveVariable( String name )
            {
                return variableMap.get( name );
            }

            @Override
            public ValueExpression setVariable( String name, ValueExpression variable )
            {
                if ( variableMap.isEmpty() )
                {
                    variableMap = new HashMap<>();
                }
                return variableMap.put( name, variable );
            }
        };
    }
}
