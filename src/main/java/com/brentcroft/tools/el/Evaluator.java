package com.brentcroft.tools.el;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Evaluator
{
    Object eval( String expression, Map<String, Object> scope );


    static Stream<String> stepsStream( Object value) {
        String uncommented = Stream
                .of(value.toString().split( "\\s*[\\n\\r]+\\s*" ))
                .filter( v -> !v.isEmpty() && !v.startsWith( "#" ) )
                .map( String::trim )
                .collect( Collectors.joining(" "));
        return Stream
                .of(uncommented.split( "\\s*[;]+\\s*" ))
                .map( String::trim )
                .filter( v -> !v.isEmpty() );
    }
}
