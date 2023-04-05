package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.StringUpcaster;
import org.xml.sax.InputSource;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ELFunctions
{
    public static void install( StaticMethodMapper em )
    {
        try
        {
            em.mapFunction( "format", ELFunctions.class.getMethod( "format", String.class, List.class ) );
            em.mapFunction( "replaceAll", ELFunctions.class.getMethod( "replaceAll", String.class, String.class, String.class ) );

            em.mapFunction( "parseBytes", ELFunctions.class.getMethod( "bytesAsString", byte[].class, String.class ) );
            em.mapFunction( "fileExists", ELFunctions.class.getMethod( "fileExists", String.class ) );

            em.mapFunction( "username", ELFunctions.class.getMethod( "username" ) );
            em.mapFunction( "userhome", ELFunctions.class.getMethod( "userhome" ) );

            em.mapFunction( "uuid", UUID.class.getMethod( "randomUUID" ) );
            em.mapFunction( "radix", Long.class.getMethod( "toString", long.class, int.class ) );

            em.mapFunction( "currentTimeMillis", System.class.getMethod( "currentTimeMillis" ) );
            em.mapFunction( "getTime", ELFunctions.class.getMethod( "getTime", String.class ) );
            em.mapFunction( "now", ELFunctions.class.getMethod( "now" ) );
            em.mapFunction( "millisBetween", ELFunctions.class.getMethod( "millisBetween", LocalDateTime.class, LocalDateTime.class ) );
            em.mapFunction( "isWorkingDay", ELFunctions.class.getMethod( "isWorkingDay", LocalDateTime.class ) );
            em.mapFunction( "dateRange", ELFunctions.class.getMethod( "dateRange", LocalDateTime.class, LocalDateTime.class ) );

            em.mapFunction( "console", ELFunctions.class.getMethod( "console", String.class, String.class ) );
            em.mapFunction( "consolePassword", ELFunctions.class.getMethod( "consolePassword", String.class, char[].class ) );
            em.mapFunction( "consoleFormat", ELFunctions.class.getMethod( "consoleFormat", String.class, List.class ) );

            em.mapFunction( "toStringSet", StringUpcaster.class.getMethod( "toStringSet", String.class ) );
            em.mapFunction( "sort", ELFunctions.class.getMethod( "sort", Collection.class, Comparator.class ) );
            em.mapFunction( "charList", ELFunctions.class.getMethod( "charList", Object.class ) );

            em.mapFunction( "println", ELFunctions.class.getMethod( "println", Object.class ) );

            em.mapFunction( "camelCase", ELFunctions.class.getMethod( "camelCase", String.class ) );
            em.mapFunction( "pause", ELFunctions.class.getMethod( "pause", String.class ) );
            em.mapFunction( "delay", ELFunctions.class.getMethod( "delay", long.class ) );

            em.mapFunction( "textToFile", ELFunctions.class.getMethod( "textToFile", String.class, String.class ) );
            em.mapFunction( "fileToText", ELFunctions.class.getMethod( "fileToText", String.class ) );

            em.mapFunction( "return", ELFunctions.class.getMethod( "raiseReturnException", Object.class ) );
            em.mapFunction( "raise", ELFunctions.class.getMethod( "raiseRuntimeException", Object.class ) );
            em.mapFunction( "throw", ELFunctions.class.getMethod( "throwAnyException", Throwable.class ) );
            em.mapFunction( "simpleTrace", ELFunctions.class.getMethod( "simpleTrace", Throwable.class ) );
            em.mapFunction( "assertTrue", ELFunctions.class.getMethod( "assertTrue", boolean.class, String.class ) );

            em.mapFunction( "inputSource", ELFunctions.class.getMethod( "inputSource", String.class ) );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to initialise function map", e );
        }
    }

    public static void raiseReturnException( Object value )
    {
        throw new ReturnException( value );
    }

    public static void raiseRuntimeException( Object value )
    {
        throw new UserException( value.toString() );
    }

    public static void throwAnyException( Throwable exception ) throws Throwable
    {
        throw exception;
    }

    public static String simpleTrace( Throwable exception )
    {
        Throwable t = exception;
        List<String> items = new ArrayList<>();
        while (t != null) {
            items.add( String.format("%s: %s", exception.getClass().getSimpleName(), exception.getMessage()) );
            t = t.getCause();
        }
        return String.join( "\n  ", items );
    }

    public static void assertTrue( boolean test, String failMessage )
    {
        if ( test )
        {
            return;
        }
        throw new AssertionError( failMessage );
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

    public static < T > List< T > sort( Collection< T > collection, Comparator< T > comparator )
    {
        List< T > list = new ArrayList<>( collection );
        list.sort( comparator );
        return list;
    }

    public static List< String > charList( Object value )
    {
        return Optional
                .ofNullable( value )
                .map( Object::toString )
                .map( s -> s
                        .chars()
                        .mapToObj( c -> String.format( "%s", ( char ) c ) )
                        .collect( Collectors.toList() ) )
                .orElseGet( Collections::emptyList );
    }

    public static String format( String pattern, List< Object > items )
    {
        return String.format( pattern, items.toArray() );
    }

    public static boolean fileExists( String filename )
    {
        return new File( filename ).exists();
    }

    public static String bytesAsString( byte[] bytes, String charset ) throws UnsupportedEncodingException
    {
        return new String( bytes, charset );
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

    public static void consoleFormat( String format, List< Object > items )
    {
        Console console = System.console();
        if ( console == null )
        {
            return;
        }
        console.format( format, items.toArray() );
    }

    public static LocalDateTime getTime( String pattern )
    {
        return LocalDateTime.parse( pattern );
    }

    public static LocalDateTime now()
    {
        return LocalDateTime.now();
    }

    public static long millisBetween( LocalDateTime earlier, LocalDateTime later )
    {
        return earlier.until( later, ChronoUnit.MILLIS );
    }

    public static boolean isWorkingDay( LocalDateTime candidate )
    {
        switch ( candidate.getDayOfWeek() )
        {
            case SATURDAY:
            case SUNDAY:
                return false;
            default:
                return true;
        }
    }

    public static List< LocalDateTime > dateRange( LocalDateTime from, LocalDateTime to )
    {
        List< LocalDateTime > dates = new ArrayList<>();
        while ( ! from.isAfter( to ) )
        {
            dates.add( from );
            from = from.plusDays( 1 );
        }
        return dates;
    }

    public static String println( Object o )
    {
        System.out.println( o );
        return "OK";
    }

    public static String delay( long millis )
    {
        try
        {
            Thread.sleep( millis );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        return "OK";
    }

    public static String pause( String message )
    {
        JOptionPane
                .showMessageDialog(
                        null,
                        message,
                        "Paused",
                        JOptionPane.INFORMATION_MESSAGE );
        return "OK";
    }

    public static String camelCase( String text )
    {
        boolean[] isFirst = { true };
        return Stream
                .of( text.split( "\\s+" ) )
                .map( t -> {
                    if ( isFirst[ 0 ] )
                    {
                        isFirst[ 0 ] = false;
                        return t.substring( 0, 1 ).toLowerCase( Locale.ROOT ) + t.substring( 1 );
                    }
                    return t.substring( 0, 1 ).toUpperCase( Locale.ROOT ) + t.substring( 1 );
                } )
                .collect( Collectors.joining() );
    }

    public static String textToFile( String text, String filename ) throws IOException
    {
        Files.write( Paths.get( filename ), text.getBytes(), StandardOpenOption.CREATE );
        return "OK";
    }

    public static String fileToText( String filename ) throws IOException
    {
        return String
                .join( "\n", Files
                        .readAllLines( Paths.get( filename ) ) );
    }

    public static InputSource inputSource( String value )
    {
        return new InputSource( new StringReader( value ) );
    }
}
