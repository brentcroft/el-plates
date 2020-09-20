package com.brentcroft.tools.jstl.tag;


import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
import lombok.extern.java.Log;
import org.w3c.dom.Element;

import javax.script.*;
import java.util.Map;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.lang.String.format;
import static java.util.Objects.isNull;

@Log
public class JstlScript extends AbstractJstlElement
{
    private final static String TAG = "script";

    private final boolean isPublic;

    private final ELTemplateManager elTemplateManager;

    private final boolean renderOutput;

    private final ScriptEngine engine;

    private CompiledScript script;


    private final static ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    public final static String DEFAULT_SCRIPT_ENGINE_NAME = "js";

    private final static ScriptEngine defaultScriptEngine = scriptEngineManager.getEngineByName( DEFAULT_SCRIPT_ENGINE_NAME );


    public JstlScript( JstlTemplateManager.JstlTemplateHandler templateHandler, boolean publicScope, boolean renderOutput, String engineName )
    {
        this.elTemplateManager = templateHandler.getELTemplateManager();
        this.isPublic = publicScope;
        this.renderOutput = renderOutput;

        if ( DEFAULT_SCRIPT_ENGINE_NAME.equalsIgnoreCase( engineName ) )
        {
            engine = defaultScriptEngine;
        }
        else
        {
            engine = scriptEngineManager.getEngineByName( engineName );
        }

        if ( engine == null )
        {
            throw new RuntimeException( format( TagMessages.ENGINE_NAME_NOT_FOUND, engineName ) );
        }


        innerRenderable = new JstlTemplate( this );
    }

    private void compile()
    {
        try
        {
            final String source = innerRenderable.render( EMPTY_MAP );

            script = ( ( Compilable ) engine ).compile( source );
        }
        catch ( ScriptException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void normalize()
    {
        compile();
    }


    public String render( final Map< String, Object > bindings )
    {
        if ( isDeferred() )
        {
            return toText();
        }

        if ( script == null )
        {
            return "";
        }


        /*
         * problem is that Nashorn doesn't update bindings unless it created
         * them.
         *
         * http://stackoverflow.com/questions/24142979/reading-updated-variables-
         * after-evaluating-a-script
         */
        final Bindings engineBindings = engine.createBindings();


        if ( bindings instanceof MapBindings )
        {
            ( ( MapBindings ) bindings ).copyTo( engineBindings );
        }
        else
        {
            engineBindings.putAll( bindings );
        }

        try
        {
            Object result;

            // evaluate the script
            if ( isPublic && bindings instanceof Bindings )
            {
                result = script.eval( engineBindings );

                // need to copy back all first level members
                // this must be copying out loads of other crap
                // TODO: figure out how to handle bindings
                final String[] keys = engineBindings.keySet().toArray( new String[ 0 ] );

                for ( String key : keys )
                {
                    bindings.put( key, engineBindings.get( key ) );
                }
            }
            else
            {
                if ( ! ( bindings instanceof Bindings ) && isPublic )
                {
                    log.warning( () -> "Map is not an instance of script Bindings: public visibility not available!" );
                }

                result = script.eval( engineBindings );
            }

            return ( renderOutput && result != null ) ? result.toString() : "";
        }
        catch ( ScriptException e )
        {
            throw new RuntimeException( e );
        }
    }

    public String toText()
    {
        return format( "<%s%s%s>%s</%s>",
                prefix( TAG ),
                ! isPublic ? "" : " public=\"true\"",
                ! renderOutput ? "" : " render=\"true\"",
                innerRenderable,
                prefix( TAG ) );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter )
    {
        String key = "COMPILED_SCRIPT";

        script = ( CompiledScript ) element.getUserData( key );

        if ( isNull( script ) )
        {
            innerRenderable.addRenderable( elTemplateManager.buildTemplate( element.getTextContent() ) );

            compile();

            element.setUserData( key, script, null );
        }

        render( bindings );
    }
}
