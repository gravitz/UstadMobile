package com.ustadmobile.door

import fi.iki.elonen.router.RouterNanoHTTPD
import fi.iki.elonen.NanoHTTPD

/**
 * This is the parent class for all UriResponders that are generated by door. It simply
 * implements http methods that are not used and returns a METHOD_NOT_ALLOWED respond
 */
abstract class AbstractDoorUriResponder : RouterNanoHTTPD.UriResponder {

    override fun put(
            _uriResource: RouterNanoHTTPD.UriResource,
            _urlParams: Map<String, String>,
            _session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response =
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    DoorConstants.MIME_TYPE_PLAIN, "")

    override fun delete(
            _uriResource: RouterNanoHTTPD.UriResource,
            _urlParams: Map<String, String>,
            _session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response =
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    DoorConstants.MIME_TYPE_PLAIN, "")

    override fun other(
            methodName: String,
            _uriResource: RouterNanoHTTPD.UriResource,
            _urlParams: Map<String, String>,
            _session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response =
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    DoorConstants.MIME_TYPE_PLAIN, "")
}