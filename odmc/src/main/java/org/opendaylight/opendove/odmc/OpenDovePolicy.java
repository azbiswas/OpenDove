/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.opendove.odmc;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class OpenDovePolicy extends OpenDoveObject implements IfOpenDCSTrackedObject {

    @XmlElement(name="id")
    String uuid;

    @XmlElement(name="type")
    Integer policyType;

    @XmlElement(name="src_network")
    Integer sourceVNID;

    @XmlElement(name="dst_network")
    Integer destinationVNID;

    @XmlElement(name="ttl")
    Integer timeToLive;    // legacy

    @XmlElement(name="action")
    Integer policyAction;

    @XmlElement(name="domain_uuid")
    String domainUUID;

    @XmlElement(name="traffic_type")
    Integer trafficType;

    Integer domainID;

    public OpenDovePolicy() { }

    public OpenDovePolicy(Integer src_vnid, Integer dst_vnid, String dom_UUID, Integer domain_id, Integer tType) {
        this.uuid = java.util.UUID.randomUUID().toString();
        this.sourceVNID = src_vnid;
        this.destinationVNID = dst_vnid;
        this.timeToLive = 1000;
        this.policyAction = 1;
        this.policyType = 1;
        this.domainUUID = dom_UUID;
        this.trafficType = tType;
        this.domainID = domain_id;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }


    public Integer getPolicyType() {
        return policyType;
    }

    public void setPolicyType(Integer policyType) {
        this.policyType = policyType;
    }

    public Integer getSourceVNID() {
        return sourceVNID;
    }

    public void setSourceVNID(Integer sourceVNID) {
        this.sourceVNID = sourceVNID;
    }

    public Integer getDestinationVNID() {
        return destinationVNID;
    }

    public void setDestinationVNID(Integer destinationVNID) {
        this.destinationVNID = destinationVNID;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    public Integer getPolicyAction() {
        return policyAction;
    }

    public void setPolicyAction(Integer policyAction) {
        this.policyAction = policyAction;
    }

    public String getDomainUUID() {
        return domainUUID;
    }

    public void setDomainUUID(String domainUUID) {
        this.domainUUID = domainUUID;
    }

    public Integer getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(Integer trafficType) {
        this.trafficType = trafficType;
    }

    public boolean isTrackedByDCS() {
        return true;
    }

    public String getSBDcsUri() {
        return "/controller/sb/v2/opendove/odmc/domains/bynumber/" + domainID + "/policy/" + uuid;
    }

    public static void removeAllowPolicies(IfSBDovePolicyCRUD dovePolicyDB,
            OpenDoveNetwork newODN, OpenDoveNetwork oldODN) {
        removeAllowPolicy(dovePolicyDB, newODN, oldODN, 0);
        removeAllowPolicy(dovePolicyDB, newODN, oldODN, 1);
        removeAllowPolicy(dovePolicyDB, oldODN, newODN, 0);
        removeAllowPolicy(dovePolicyDB, oldODN, newODN, 1);
    }

    private static void removeAllowPolicy(IfSBDovePolicyCRUD dovePolicyDB,
            OpenDoveNetwork newODN, OpenDoveNetwork oldODN, int traffic_type) {
        List<OpenDovePolicy> policies = dovePolicyDB.getPolicies();
        Iterator<OpenDovePolicy> policyIterator = policies.iterator();
        boolean found = false;
        while (policyIterator.hasNext() && !found) {
            OpenDovePolicy policy = policyIterator.next();
            if (policy.getSourceVNID() == newODN.getVnid() &&
                    policy.getDestinationVNID() == oldODN.getVnid() &&
                    policy.getPolicyAction() == 1 && policy.getTrafficType() == traffic_type) {
                OpenDovePolicy delta = new OpenDovePolicy();
                delta.setPolicyAction(0);
                found = true;
                dovePolicyDB.updatePolicy(policy, delta);
            }
        }
    }

    public static void createAllowPolicies(IfSBDovePolicyCRUD dovePolicyDB,
            OpenDoveNetwork newODN, OpenDoveNetwork oldODN) {
        createAllowPolicy(dovePolicyDB, newODN, oldODN, 0);
        createAllowPolicy(dovePolicyDB, newODN, oldODN, 1);
        createAllowPolicy(dovePolicyDB, oldODN, newODN, 0);
        createAllowPolicy(dovePolicyDB, oldODN, newODN, 1);
    }

    private static void createAllowPolicy(IfSBDovePolicyCRUD dovePolicyDB,
            OpenDoveNetwork newODN, OpenDoveNetwork oldODN, int traffic_type) {
        List<OpenDovePolicy> policies = dovePolicyDB.getPolicies();
        Iterator<OpenDovePolicy> policyIterator = policies.iterator();
        boolean found = false;
        while (policyIterator.hasNext() && !found) {
            OpenDovePolicy policy = policyIterator.next();
            if (policy.getSourceVNID() == newODN.getVnid() &&
                    policy.getDestinationVNID() == oldODN.getVnid() &&
                    policy.getPolicyAction() == 0 && policy.getTrafficType() == traffic_type) {
                OpenDovePolicy delta = new OpenDovePolicy();
                delta.setPolicyAction(1);
                found = true;
                dovePolicyDB.updatePolicy(policy, delta);
            }
        }
        if (!found) {
            OpenDovePolicy newPolicy = new OpenDovePolicy(newODN.getVnid(),
                    oldODN.getVnid(), newODN.getDomain_uuid(), newODN.getDomain_id(), traffic_type);
            newPolicy.setTombstoneFlag(false);
            dovePolicyDB.addPolicy(newPolicy.getUUID(), newPolicy);
        }
    }

    public boolean overwrite(OpenDovePolicy delta) {
        Method[] methods = this.getClass().getMethods();

        for(Method toMethod: methods){
            if(toMethod.getDeclaringClass().equals(this.getClass())
                    && toMethod.getName().startsWith("set")){

                String toName = toMethod.getName();
                String fromName = toName.replace("set", "get");

                try {
                    Method fromMethod = delta.getClass().getMethod(fromName);
                    Object value = fromMethod.invoke(delta, (Object[])null);
                    if(value != null){
                        toMethod.invoke(this, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }
}
