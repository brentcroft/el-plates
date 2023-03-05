package com.brentcroft.tools.jstl.tag;


import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.el.Evaluator;
import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import jakarta.el.ValueExpression;
import lombok.extern.java.Log;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.stream.Collectors;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.lang.String.format;
import static java.util.Objects.isNull;

@Log
public class JstlScript extends AbstractJstlElement
{
    private final static String TAG = "script";
    private final ELTemplateManager elTemplateManager;
    private final boolean renderOutput;
    private ValueExpression script;

    public JstlScript( JstlTemplateManager.JstlTemplateHandler templateHandler, boolean renderOutput )
    {
        this.elTemplateManager = templateHandler.getELTemplateManager();
        this.renderOutput = renderOutput;
        innerRenderable = new JstlTemplate( this );
    }

    private void compile()
    {
        final String source = Evaluator
                .stepsStream( innerRenderable.render( EMPTY_MAP ) )
                .collect( Collectors.joining(";\n"));

        script = elTemplateManager
                .getValueExpression( "${" + source + "}", EMPTY_MAP, Object.class );
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
        Object result = script.getValue( elTemplateManager.getELContext( bindings ) );
        return ( renderOutput && result != null ) ? result.toString() : "";
    }

    public String toText()
    {
        return format( "<%s%s>%s</%s>",
                prefix( TAG ),
                ! renderOutput ? "" : " render=\"true\"",
                innerRenderable,
                prefix( TAG ) );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter )
    {
        String key = "COMPILED_SCRIPT";

        script = ( ValueExpression ) element.getUserData( key );

        if ( isNull( script ) )
        {
            innerRenderable.addRenderable( elTemplateManager.buildTemplate( element.getTextContent() ) );

            compile();

            element.setUserData( key, script, null );
        }

        render( bindings );
    }
}
