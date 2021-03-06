/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.opendove.odmc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class OpenDoveNetwork extends OpenDoveObject implements IfOpenDCSTrackedObject, IfOpenDGWTrackedObject {

    @XmlElement(name="id")
    String uuid;

    @XmlElement(name="network_id")
    Integer vnid;

    @XmlElement(name="name")
    String name;

    @XmlElement(name="domain_uuid")
    String domain_uuid;

    @XmlElement (name="type")
    Integer networkType;

    String associatedOSNetworkUUID;

    List<OpenDoveServiceAppliance> gateways;
    List<OpenDoveSwitch> hostingSwitches;

    OpenDoveDomain  scopingDomain;
    Integer domain_id;


    public OpenDoveNetwork() {
        gateways = new ArrayList<OpenDoveServiceAppliance>();
        hostingSwitches = new ArrayList<OpenDoveSwitch>();
    }

    public OpenDoveNetwork(String name, int vnid, OpenDoveDomain scopingDomain, int type, String oSNetworkUUID) {
        uuid = java.util.UUID.randomUUID().toString();
        this.vnid = vnid;
        this.name = name;
        this.scopingDomain = scopingDomain;
        domain_uuid = scopingDomain.getUUID();
        domain_id = scopingDomain.getDomainId();
        tombstoneFlag = false;
        networkType = type;
        associatedOSNetworkUUID = oSNetworkUUID;
        gateways = new ArrayList<OpenDoveServiceAppliance>();
        hostingSwitches = new ArrayList<OpenDoveSwitch>();
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getVnid() {
        return vnid;
    }

    public void setVnid(Integer vnid) {
        this.vnid = vnid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain_uuid() {
        return domain_uuid;
    }

    public void setDomain_uuid(String domain_uuid) {
        this.domain_uuid = domain_uuid;
    }

    public Integer getDomain_id() {
        return domain_id;
    }

    public void setDomain_id(Integer domain_id) {
        this.domain_id = domain_id;
    }

    public Integer getNetworkType() {
        return networkType;
    }

    public void setNetworkType(Integer networkType) {
        this.networkType = networkType;
    }

    public boolean isTrackedByDCS() {
        return true;
    }

    public String getNeutronNetwork() {
        return associatedOSNetworkUUID;
    }

    public String getSBDcsUri() {
        domain_id = scopingDomain.getDomainId();
        return "/controller/sb/v2/opendove/odmc/domains/bynumber/" + domain_id + "/networks/" + vnid;
    }

    public boolean isTrackedByDGW() {
        return true;
    }

    public String getSBDgwUri() {
        domain_id = scopingDomain.getDomainId();
        return "/controller/sb/v2/opendove/odmc/networks/" + uuid;
    }

    public void addEGW(OpenDoveServiceAppliance target) {
        gateways.add(target);
    }

    public void removeEGW(OpenDoveServiceAppliance target) {
        gateways.remove(target);
    }

    public List<OpenDoveServiceAppliance> getEGWs() {
        return gateways;
    }

    public boolean networkUsesEGW(String gatewayUUID) {
        Iterator<OpenDoveServiceAppliance> iterator = gateways.iterator();
        while (iterator.hasNext()) {
            OpenDoveServiceAppliance testDSA = iterator.next();
            if (testDSA.getUUID().equalsIgnoreCase(gatewayUUID)) {
                return true;
            }
        }
        return false;
    }

    public List<OpenDoveSwitch> getHostingSwitches() {
        return hostingSwitches;
    }

    public void addHostingSwitch(OpenDoveSwitch s) {
        hostingSwitches.add(s);
    }

    public void removeHostingSwitch(OpenDoveSwitch s) {
        hostingSwitches.remove(s);
    }

    public OpenDoveDomain getScopingDomain() {
        return scopingDomain;
    }

    public void setScopingDomain(OpenDoveDomain d) {
        scopingDomain = d;
    }

    public void removeScopingDomain(OpenDoveDomain d) {
        if (scopingDomain.equals(d)) {
            scopingDomain = null;
        }
    }
}
