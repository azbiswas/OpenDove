/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.opendove.odmc.rest.southbound;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.opendove.odmc.IfSBDoveGwIpv4CRUD;
import org.opendaylight.opendove.odmc.OpenDoveCRUDInterfaces;
import org.opendaylight.opendove.odmc.rest.OpenDoveGwIpv4Request;

/**
 * Open DOVE Southbound REST APIs for Gw Ipv4 Addresses.<br>
 *
 * <br>
 * <br>
 * Authentication scheme [for now]: <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in
 * tomcat-server.xml after adding a proper keystore / SSL certificate from a
 * trusted authority.<br>
 * More info :
 * http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 *
 */

@Path("/odgw/ipv4")
public class OpenDoveGwIpv4Southbound {

    @Path("/{ipv4UUID}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 204, condition = "No content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 404, condition = "Not Found"),
            @ResponseCode(code = 500, condition = "Internal Error") })
    public Response showIpv4(
            @PathParam("ipv4UUID") String ipv4UUID
            ) {
        IfSBDoveGwIpv4CRUD sbInterface = OpenDoveCRUDInterfaces.getIfSBDoveGwIpv4CRUD(this);
        if (sbInterface == null) {
            throw new ServiceUnavailableException("OpenDove SB Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (!sbInterface.gwIpv4Exists(ipv4UUID)) {
            throw new ResourceNotFoundException("Gateway IPv4 record doesn't exist");
        }
        return Response.status(200).entity(new OpenDoveGwIpv4Request(sbInterface.getGwIpv4(ipv4UUID))).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 204, condition = "No content"),
            @ResponseCode(code = 401, condition = "Unauthorized"),
            @ResponseCode(code = 500, condition = "Internal Error") })
    public Response listIpv4Interfaces() {
        IfSBDoveGwIpv4CRUD sbInterface = OpenDoveCRUDInterfaces.getIfSBDoveGwIpv4CRUD(this);
        if (sbInterface == null) {
            throw new ServiceUnavailableException("OpenDove SB Interface "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return Response.status(200).entity(new OpenDoveGwIpv4Request(sbInterface.getGwIpv4Pool())).build();
    }
}
