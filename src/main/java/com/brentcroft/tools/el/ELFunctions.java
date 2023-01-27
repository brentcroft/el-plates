package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.StringUpcaster;
import jakarta.el.LambdaExpression;

import javax.swing.*;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ELFunctions
{
    public static void install( StaticMethodMapper em) {
        try
        {
            em.mapFunction( "format", ELFunctions.class.getMethod( "format", String.class, List.class ) );
            em.mapFunction( "replaceAll", ELFunctions.class.getMethod( "replaceAll", String.class, String.class, String.class ) );

            em.mapFunction( "parseBytes", ELFunctions.class.getMethod( "bytesAsString", byte[].class, String.class ) );
            em.mapFunction( "fileExists", ELFunctions.class.getMethod( "fileExists", String.class ) );


            em.mapFunction( "int", Integer.class.getMethod( "valueOf", String.class ) );
            em.mapFunction( "double", Double.class.getMethod( "valueOf", String.class ) );
            em.mapFunction( "pow", Math.class.getMethod( "pow", double.class, double.class ) );

            // capture as float
            em.mapFunction( "float", ELFunctions.class.getMethod( "boxFloat", Float.class ) );
            em.mapFunction( "random", ELFunctions.class.getMethod( "random" ) );

            em.mapFunction( "username", ELFunctions.class.getMethod( "username" ) );
            em.mapFunction( "userhome", ELFunctions.class.getMethod( "userhome" ) );

            em.mapFunction( "uuid", UUID.class.getMethod( "randomUUID" ) );
            em.mapFunction( "radix", Long.class.getMethod( "toString", long.class, int.class ) );

            em.mapFunction( "currentTimeMillis", System.class.getMethod( "currentTimeMillis" ) );

            em.mapFunction( "getTime", ELFunctions.class.getMethod( "getTime", String.class ) );
            em.mapFunction( "now", ELFunctions.class.getMethod( "now" ) );


            em.mapFunction( "console", ELFunctions.class.getMethod( "console", String.class, String.class ) );
            em.mapFunction( "consolePassword", ELFunctions.class.getMethod( "consolePassword", String.class, char[].class ) );
            em.mapFunction( "consoleFormat", ELFunctions.class.getMethod( "consoleFormat", String.class, Object[].class ) );

            em.mapFunction( "toStringSet", StringUpcaster.class.getMethod( "toStringSet", String.class ) );
            em.mapFunction( "sort", ELFunctions.class.getMethod( "sort", Collection.class, Comparator.class ) );

            em.mapFunction( "println", ELFunctions.class.getMethod( "println", Object.class ) );
            em.mapFunction( "camelCase", ELFunctions.class.getMethod( "camelCase", String.class ) );
            em.mapFunction( "pause", ELFunctions.class.getMethod( "pause", String.class ) );
            em.mapFunction( "textToFile", ELFunctions.class.getMethod( "textToFile", String.class, String.class ) );

            em.mapFunction( "return", ELFunctions.class.getMethod("raiseReturnException", Object.class) );

//            em.mapFunction( "ifThen", ELFunctions.class.getMethod( "ifThen", LambdaExpression.class, LambdaExpression.class ) );
//            em.mapFunction( "ifThenElse", ELFunctions.class.getMethod( "ifThenElse", LambdaExpression.class, LambdaExpression.class, LambdaExpression.class ) );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to initialise function map", e );
        }
    }

    public static void raiseReturnException(Object value) {
        throw new ReturnException( value );
    }

    public static String username()
    {
        return System.getProperty( "user.name" );
    }

    public static String userhome()
    {
        return System.getProperty( "user.home" );
    }

    public static String replaceAll( String source, String regex, String rep )
    {
        return Optional
                .ofNullable( source )
                .map( s -> s.replaceAll(
                        Optional
                                .ofNullable( regex )
                                .orElseThrow( () -> new RuntimeException( "regex cannot be null" ) ),
                        Optional
                                .ofNullable( rep )
                                .orElseThrow( () -> new RuntimeException( "replacement cannot be null" ) )
                ) )
                .orElseThrow( () -> new RuntimeException( "source cannot be null" ) );
    }

    public static <T> List<T> sort( Collection<T> collection, Comparator<T> comparator )
    {
        List<T> list =  new ArrayList<>( collection );
        list.sort( comparator );
        return list;
    }

    public static String format(String pattern, List<Object> items) {
        return String.format( pattern, new ArrayList<>( items ).toArray() );
    }

    public static Float boxFloat( Float f )
    {
        return f;
    }

    public static Double asDouble( Double f )
    {
        return f;
    }

    public static Double pow( Double base, Double exponent )
    {
        return Math.pow( base, exponent );
    }

    public static boolean fileExists( String filename )
    {
        return new File( filename ).exists();
    }

    public static String bytesAsString( byte[] bytes )
    {
        return new String( bytes );
    }

    public static String bytesAsString( byte[] bytes, String charset ) throws UnsupportedEncodingException
    {
        return new String( bytes, charset );
    }


    public static Random random()
    {
        return new Random();
    }

    public static String console( String prompt, String defaultValue )
    {
        Console console = System.console();

        if ( console == null )
        {
            return defaultValue;
        }

        return console.readLine( prompt );
    }

    public static char[] consolePassword( String prompt, char[] defaultValue )
    {
        Console console = System.console();

        if ( console == null )
        {
            return defaultValue;
        }

        return console.readPassword( prompt );
    }

    public static void consoleFormat( String format, Object... args )
    {
        Console console = System.console();

        if ( console == null )
        {
            return;
        }

        console.format( format, args );
    }

    public static LocalDateTime getTime( String pattern )
    {
        return LocalDateTime.parse( pattern );
    }

    public static LocalDateTime now()
    {
        return LocalDateTime.now();
    }

    public static String println( Object o )
    {
        System.out.println( o );
        return "OK";
    }

    public static String pause(String message) {
        JOptionPane
                .showMessageDialog(
                        null,
                        message,
                        "Paused",
                        JOptionPane.INFORMATION_MESSAGE );
        return "OK";
    }

    public static String camelCase(String text) {
        boolean[] isFirst = {true};
        return Stream
                .of( text.split("\\s+"))
                .map( t -> {
                    if (isFirst[0]) {
                        isFirst[0] = false;
                        return t.substring( 0, 1 ).toLowerCase( Locale.ROOT ) + t.substring( 1 );
                    }
                    return t.substring( 0, 1 ).toUpperCase( Locale.ROOT ) + t.substring( 1 );
                } )
                .collect( Collectors.joining());
    }

    public static String textToFile(String text, String filename) throws IOException
    {
        Files.write( Paths.get(filename), text.getBytes(), StandardOpenOption.CREATE );
        return "OK";
    }



    public static void ifThen( LambdaExpression test, LambdaExpression thenOperation )
    {
        final Object [] empty = new Object[]{};
        if ((Boolean)test.invoke( empty ) ) {
            thenOperation.invoke( empty );
        }
    }

    public static void ifThenElse( LambdaExpression test, LambdaExpression thenOperation, LambdaExpression elseOperation )
    {
        final Object[] empty = new Object[]{};
        if ((Boolean)test.invoke(new Object[]{}) ) {
            thenOperation.invoke(empty);
        } else {
            elseOperation.invoke(empty);
        }
    }
}
