package com.ustadmobile.port.sharedse.util

import com.ustadmobile.xmlpullparserkmp.XmlPullParser
import com.ustadmobile.xmlpullparserkmp.XmlPullParserConstants
import com.ustadmobile.xmlpullparserkmp.XmlSerializer

/**
 * Pass XML through from an XmlPullParser to an XmlSerializer. This will not call startDocument
 * or endDocument - that must be called separately. This allows different documents to be merged.
 *
 * @param xpp XmlPullParser XML is coming from
 * @param xs XmlSerializer XML is being written to
 * @param seperateEndTagRequiredElements An array of elements where separate closing tags are
 * required e.g. script, style etc. using &lt;/script&gt; instead
 * of &lt;script ... /&gt;
 * @param filter XmlPassThroughFilter that can be used to add to output or interrupt processing
 * @throws XmlPullParserException
 * @throws IOException
 */

interface XmlPassThroughFilter {

    /**
     * Called before the given event is passed through to the XmlSerializer.
     *
     * @param evtType The event type from the parser
     * @param parser The XmlPullParser being used
     * @param serializer The XmlSerializer being used
     *
     * @return true to continue processing, false to end processing
     * @throws IOException
     * @throws XmlPullParserException
     */
    fun beforePassthrough(evtType: Int, parser: XmlPullParser, serializer: XmlSerializer): Boolean

    /**
     * Called after the given event was passed through to the XmlSerializer.
     *
     * @param evtType The event type from the parser
     * @param parser The XmlPullParser being used
     * @param serializer The XmlSerializer being used
     *
     * @return true to continue processing, false to end processing
     * @throws IOException
     * @throws XmlPullParserException
     */
    fun afterPassthrough(evtType: Int, parser: XmlPullParser, serializer: XmlSerializer): Boolean


}

fun passXmlThrough(xpp: XmlPullParser, xs: XmlSerializer,
                   seperateEndTagRequiredElements: Array<String>?,
                   filter: XmlPassThroughFilter?) {

    var evtType = xpp.getEventType()
    var lastEvent = -1
    var tagName: String
    while (evtType != XmlPullParserConstants.END_DOCUMENT) {
        if (filter != null && !filter.beforePassthrough(evtType, xpp, xs))
            return

        when (evtType) {
            XmlPullParserConstants.DOCDECL -> {
                xs.docdecl(xpp.getText().toString())
            }

            XmlPullParserConstants.ENTITY_REF -> {
                xs.entityRef(xpp.getName())
            }

            XmlPullParserConstants.START_TAG -> {
                if(xpp.getNamespace() != null)
                    xs.setPrefix(xpp.getPrefix(), xpp.getNamespace())

                xs.startTag(xpp.getNamespace(), xpp.getName().toString())
                for (i in 0 until xpp.getAttributeCount()) {
                    xs.attribute(xpp.getAttributeNamespace(i),
                            xpp.getAttributeName(i).toString(), xpp.getAttributeValue(i).toString())
                }
            }
            XmlPullParserConstants.TEXT -> xs.text(xpp.getText().toString())
            XmlPullParserConstants.END_TAG -> {
                tagName = xpp.getName().toString()

                if (lastEvent == XmlPullParserConstants.START_TAG
                        && seperateEndTagRequiredElements != null
                        && tagName in seperateEndTagRequiredElements) {
                    xs.text(" ")
                }

                xs.endTag(xpp.getNamespace(), tagName)
            }
        }
        if (filter != null && !filter.afterPassthrough(evtType, xpp, xs))
            return

        lastEvent = evtType
        evtType = xpp.nextToken()
    }
}