package com.brentcroft.tools.jstl.tag;

import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import jakarta.el.*;
import java.util.Map;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.util.Optional.ofNullable;

public class JstlParam extends AbstractJstlElement
{
    private final static String TAG = "param";

    private final String name;
    private final String valueEL;

    private ValueExpression valueExpression;

    private final ELTemplateManager elTemplateManager;


    public JstlParam(ELTemplateManager elTemplateManager, String name, String valueEL )
    {
        this.elTemplateManager = elTemplateManager;
        this.name = name;
        this.valueEL = valueEL;

        valueExpression = null;
        innerRenderable = new JstlTemplate( this );
    }

    private void compile()
    {
        valueExpression = elTemplateManager.getValueExpression( valueEL, EMPTY_MAP, Object.class );
    }

    @Override
    public void normalize()
    {
        compile();
    }

    public String render( Map< String, Object > bindings )
    {
        if ( isDeferred() )
        {
            return toText();
        }

        final Object value = valueExpression.getValue( elTemplateManager.getELContext( bindings ) );

        bindings.put(name, value);

        return ofNullable(value).map(Object::toString).orElse("");
    }

    public String toText()
    {
        return String.format( "<%s name=\"%s\" value=\"%s\"/>", prefix( TAG ), name, valueEL );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter ) throws SAXException
    {
        final Object value = valueExpression.getValue( elTemplateManager.getELContext( bindings ) );

        bindings.put(name, value);
    }
}
