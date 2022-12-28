package com.brentcroft.tools.el;

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

    @Deprecated()
    public static String consolePasswordAsString( String prompt, String defaultValue )
    {
        Console console = System.console();

        if ( console == null )
        {
            return defaultValue;
        }

        return String.valueOf( console.readPassword( prompt ) );
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
}
