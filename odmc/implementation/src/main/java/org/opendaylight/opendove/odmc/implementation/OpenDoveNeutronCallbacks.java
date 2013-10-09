/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.opendove.odmc.implementation;

import java.util.Iterator;
import java.util.List;

import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter_Interface;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.opendove.odmc.IfNBSystemRU;
import org.opendaylight.opendove.odmc.IfOpenDoveServiceApplianceCRU;
import org.opendaylight.opendove.odmc.IfSBDoveDomainCRU;
import org.opendaylight.opendove.odmc.IfSBDoveEGWFwdRuleCRUD;
import org.opendaylight.opendove.odmc.IfSBDoveEGWSNATPoolCRUD;
import org.opendaylight.opendove.odmc.IfSBDoveGwIpv4CRUD;
import org.opendaylight.opendove.odmc.IfSBDoveNetworkCRU;
import org.opendaylight.opendove.odmc.IfSBDoveNetworkSubnetAssociationCRUD;
import org.opendaylight.opendove.odmc.IfSBDovePolicyCRUD;
import org.opendaylight.opendove.odmc.IfSBDoveSubnetCRUD;
import org.opendaylight.opendove.odmc.OpenDoveCRUDInterfaces;
import org.opendaylight.opendove.odmc.OpenDoveDomain;
import org.opendaylight.opendove.odmc.OpenDoveEGWFwdRule;
import org.opendaylight.opendove.odmc.OpenDoveEGWSNATPool;
import org.opendaylight.opendove.odmc.OpenDoveGwIpv4;
import org.opendaylight.opendove.odmc.OpenDoveNetwork;
import org.opendaylight.opendove.odmc.OpenDoveNetworkSubnetAssociation;
import org.opendaylight.opendove.odmc.OpenDoveNeutronControlBlock;
import org.opendaylight.opendove.odmc.OpenDovePolicy;
import org.opendaylight.opendove.odmc.OpenDoveServiceAppliance;
import org.opendaylight.opendove.odmc.OpenDoveSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenDoveNeutronCallbacks implements INeutronNetworkAware, INeutronSubnetAware, INeutronPortAware,
INeutronRouterAware, INeutronFloatingIPAware {
    protected static final Logger logger = LoggerFactory
    .getLogger(Activator.class);

    // callbacks for INeutronNetworkAware

    public int canCreateNetwork(NeutronNetwork network) {
        if (network.getAdminStateUp() != null && !network.isAdminStateUp())
            return 400;
        return 200;
    }
    public void neutronNetworkCreated(NeutronNetwork input) {
        IfNBSystemRU systemDB = OpenDoveCRUDInterfaces.getIfSystemRU(this);
        IfSBDoveDomainCRU domainDB = OpenDoveCRUDInterfaces.getIfDoveDomainCRU(this);
        IfSBDoveNetworkCRU doveNetworkDB = OpenDoveCRUDInterfaces.getIfDoveNetworkCRU(this);
        if (!input.isRouterExternal()) {            // don't map router external networks
            if (input.isShared()) {                    // map shared network
                OpenDoveNeutronControlBlock controlBlock = systemDB.getSystemBlock(); //get system block
                if (!controlBlock.getDomainSeparation()) { //if domain separation not supported, map to shared domain
                    OpenDoveDomain sharedDomain = createDoveDomain("SharedDomain", input.getID(), domainDB, doveNetworkDB);
                    int vnid = doveNetworkDB.allocateVNID();
                    String networkName = "Neutron "+input.getID();
                    OpenDoveNetwork doveNetwork = new OpenDoveNetwork(networkName, vnid, sharedDomain, 0, input.getID());
                    doveNetworkDB.addNetwork(doveNetwork.getUUID(), doveNetwork);
                }
            } else {                                // map dedicated network
                String domainName = "Neutron "+input.getTenantID();
                OpenDoveDomain domain = createDoveDomain(domainName, input.getID(), domainDB, doveNetworkDB);
                int vnid = doveNetworkDB.allocateVNID();
                String networkName = "Neutron "+input.getID();
                OpenDoveNetwork doveNetwork = new OpenDoveNetwork(networkName, vnid, domain, 0, input.getID());
                doveNetworkDB.addNetwork(doveNetwork.getUUID(), doveNetwork);
            }
        }
    }

    private OpenDoveDomain createDoveDomain(String domainName, String netUUID,
            IfSBDoveDomainCRU domainDB, IfSBDoveNetworkCRU doveNetworkDB) {
        OpenDoveDomain domain;
        if (!domainDB.domainExistsByName(domainName)) { // look up domain
            domain = new OpenDoveDomain(domainName); // if doesn't exist, create
            domainDB.addDomain(domain.getUUID(), domain);
            //create EXT MCAST network
            int vnid = doveNetworkDB.allocateVNID();
            String networkName = "Ext_MCast_"+vnid;
            OpenDoveNetwork extMCastNet = new OpenDoveNetwork(networkName, vnid, domain, 1, netUUID);
            doveNetworkDB.addNetwork(extMCastNet.getUUID(), extMCastNet);
        } else
            domain = domainDB.getDomainByName(domainName);
        return domain;
    }

    public int canUpdateNetwork(NeutronNetwork delta, NeutronNetwork original) {
        /*
         * transitions forbidden by openDove
         */
        if (delta.getNetworkName() != null || delta.getAdminStateUp() != null ||
                delta.getShared() != null || delta.getRouterExternal() != null)
            return 403;
        return 200;
    }

    public void neutronNetworkUpdated(NeutronNetwork network) {
        // openDove doesn't do anything here
        ;
    }

    public int canDeleteNetwork(NeutronNetwork network) {
        return 200;
    }

    public void neutronNetworkDeleted(NeutronNetwork network) {
        IfSBDoveNetworkCRU doveNetworkDB = OpenDoveCRUDInterfaces.getIfDoveNetworkCRU(this);
        // mark open dove networks for deletion
        // TODO: need to release EGW and SNAT pools for networks.
        Iterator<OpenDoveNetwork> i = doveNetworkDB.getNetworks().iterator();
        while (i.hasNext()) {
            OpenDoveNetwork oDN = i.next();
            if (oDN.getNeutronNetwork().equalsIgnoreCase(network.getID()))
                oDN.setTombstoneFlag(true);
        }
    }

    // callbacks for INeutronSubnetAware

    public int canCreateSubnet(NeutronSubnet subnet) {
        return canAllocateEGW(subnet.getNetworkUUID(), true);
    }

    public void neutronSubnetCreated(NeutronSubnet neutronSubnet) {
        IfNBSystemRU systemDB = OpenDoveCRUDInterfaces.getIfSystemRU(this);
        IfSBDoveDomainCRU domainDB = OpenDoveCRUDInterfaces.getIfDoveDomainCRU(this);
        IfSBDoveNetworkCRU networkDB = OpenDoveCRUDInterfaces.getIfDoveNetworkCRU(this);
        IfSBDoveSubnetCRUD subnetDB = OpenDoveCRUDInterfaces.getIfDoveSubnetCRUD(this);
        IfSBDoveNetworkSubnetAssociationCRUD networkSubnetAssociationDB =
            OpenDoveCRUDInterfaces.getIfDoveNetworkSubnetAssociationCRUD(this);
        INeutronNetworkCRUD neutronNetworkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork neutronNetwork = neutronNetworkIf.getNetwork(neutronSubnet.getNetworkUUID());
        OpenDoveNeutronControlBlock controlBlock = systemDB.getSystemBlock(); //get system block
        if (!neutronNetwork.isRouterExternal()) {
            if (neutronNetwork.isShared()) {
                if (!controlBlock.getDomainSeparation()) {
                    OpenDoveDomain sharedDomain = domainDB.getDomainByName("SharedDomain");
                    String networkName = "Neutron "+neutronSubnet.getNetworkUUID();
                    OpenDoveNetwork network = networkDB.getNetworkByName(networkName);
                    OpenDoveSubnet subnet = createDoveSubnet(neutronSubnet,
                            subnetDB, networkSubnetAssociationDB,
                            neutronNetwork, sharedDomain, network);
                    subnetDB.addSubnet(subnet.getUUID(), subnet);
                    //assign egws
                    IfOpenDoveServiceApplianceCRU serviceApplianceDB = OpenDoveCRUDInterfaces.getIfDoveServiceApplianceCRU(this);
                    IfSBDoveGwIpv4CRUD gatewayIPDB = OpenDoveCRUDInterfaces.getIfSBDoveGwIpv4CRUD(this);
                    OpenDoveGwIpv4.assignEGWs(serviceApplianceDB, gatewayIPDB, neutronSubnet, controlBlock, network);
                }
            } else {
                String domainName = "Neutron "+neutronSubnet.getTenantID();
                OpenDoveDomain domain = domainDB.getDomainByName(domainName);
                String networkName = "Neutron "+neutronSubnet.getNetworkUUID();
                OpenDoveNetwork network = networkDB.getNetworkByName(networkName);
                OpenDoveSubnet subnet = createDoveSubnet(neutronSubnet,
                        subnetDB, networkSubnetAssociationDB, neutronNetwork,
                        domain, network);
                subnetDB.addSubnet(subnet.getUUID(), subnet);
            }
        }
    }

    private OpenDoveSubnet createDoveSubnet(NeutronSubnet neutronSubnet,
            IfSBDoveSubnetCRUD subnetDB,
            IfSBDoveNetworkSubnetAssociationCRUD networkSubnetAssociationDB,
            NeutronNetwork neutronNetwork, OpenDoveDomain sharedDomain,
            OpenDoveNetwork network) {
        OpenDoveSubnet subnet = new OpenDoveSubnet(neutronSubnet, sharedDomain, neutronNetwork, network);
        // link subnet to network and track
        OpenDoveNetworkSubnetAssociation nSA = new OpenDoveNetworkSubnetAssociation();
        nSA.setOpenDoveNetworkVnid(network.getVnid());
        nSA.setOpenDoveNetworkSubnetUuid(subnet.getUUID());
        networkSubnetAssociationDB.addNetworkSubnetAssociation(nSA);
        subnetDB.addSubnet(subnet.getUUID(), subnet);
        sharedDomain.addSubnet(subnet);
        return subnet;
    }

    public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original) {
        /*
         * updates restricted by openDove
         */
        if (delta.getGatewayIP() != null)
            return 403;
        return 200;
    }

    public void neutronSubnetUpdated(NeutronSubnet subnet) {
        ; //empty because there isn't anything that passes through to the subnet
    }

    public int canDeleteSubnet(NeutronSubnet subnet) {
        return 200;
    }

    public void neutronSubnetDeleted(NeutronSubnet subnet) {
        IfSBDoveDomainCRU domainDB = OpenDoveCRUDInterfaces.getIfDoveDomainCRU(this);
        IfSBDoveSubnetCRUD doveSubnetDB = OpenDoveCRUDInterfaces.getIfDoveSubnetCRUD(this);
        // mark open dove networks for deletion
        Iterator<OpenDoveSubnet> i = doveSubnetDB.getSubnets().iterator();
        while (i.hasNext()) {
            OpenDoveSubnet oDS = i.next();
            if (oDS.getAssociatedOSSubnetUUID().equalsIgnoreCase(subnet.getID())) {
                // need to remove from the domain
                String domainName = oDS.getDomainUUID();
                OpenDoveDomain domain = domainDB.getDomainByName(domainName);
                domain.removeSubnet(oDS);
                // need to remove from the systemdb
                doveSubnetDB.removeSubnet(oDS.getUUID());
            }
        }
    }

    // INeutronPortAware methods

    public int canCreatePort(NeutronPort port) {
        // openDove specific requirement on create
        if (port.getAdminStateUp() != null && !port.isAdminStateUp())
            return 400;
        return canAllocateEGW(port.getNetworkUUID(), false);
    }

    public void neutronPortCreated(NeutronPort port) {
        IfNBSystemRU systemDB = OpenDoveCRUDInterfaces.getIfSystemRU(this);
        IfSBDoveDomainCRU domainDB = OpenDoveCRUDInterfaces.getIfDoveDomainCRU(this);
        IfSBDoveNetworkCRU networkDB = OpenDoveCRUDInterfaces.getIfDoveNetworkCRU(this);
        IfSBDoveSubnetCRUD subnetDB = OpenDoveCRUDInterfaces.getIfDoveSubnetCRUD(this);
        IfSBDoveNetworkSubnetAssociationCRUD networkSubnetAssociationDB =
            OpenDoveCRUDInterfaces.getIfDoveNetworkSubnetAssociationCRUD(this);
        INeutronNetworkCRUD neutronNetworkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork neutronNetwork = neutronNetworkIf.getNetwork(port.getNetworkUUID());
        INeutronSubnetCRUD neutronSubnetIf = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        NeutronSubnet neutronSubnet = neutronSubnetIf.getSubnet(port.getFixedIPs().get(0).getSubnetUUID());
        if (!neutronNetwork.isRouterExternal()) {
            if (neutronNetwork.isShared()) {
                OpenDoveNeutronControlBlock controlBlock = systemDB.getSystemBlock(); //get system block
                if (controlBlock.getDomainSeparation()) {
                    //create dove tenant
                    String domainName = "Neutron "+neutronNetwork.getTenantID();
                    OpenDoveDomain domain = createDoveDomain(domainName, neutronNetwork.getID(), domainDB, networkDB);
                    int vnid = networkDB.allocateVNID();
                    //create dove network
                    String networkName = "Neutron "+neutronNetwork.getID();
                    OpenDoveNetwork doveNetwork = new OpenDoveNetwork(networkName, vnid, domain, 0, neutronNetwork.getID());
                    networkDB.addNetwork(doveNetwork.getUUID(), doveNetwork);
                    //create dove subnet
                    OpenDoveSubnet subnet = createDoveSubnet(neutronSubnet,
                            subnetDB, networkSubnetAssociationDB, neutronNetwork,
                            domain, doveNetwork);
                    subnetDB.addSubnet(subnet.getUUID(), subnet);
                    //assign egw to network
                    IfOpenDoveServiceApplianceCRU serviceApplianceDB = OpenDoveCRUDInterfaces.getIfDoveServiceApplianceCRU(this);
                    IfSBDoveGwIpv4CRUD gatewayIPDB = OpenDoveCRUDInterfaces.getIfSBDoveGwIpv4CRUD(this);
                    OpenDoveGwIpv4.assignEGWs(serviceApplianceDB, gatewayIPDB, neutronSubnet, controlBlock, doveNetwork);
                }
            }
        }
    }

    public int canUpdatePort(NeutronPort delta, NeutronPort original) {
        // openDove specific things that can't be changed
        if (delta.getAdminStateUp() != null)
            return 403;
        return 200;
    }
    public void neutronPortUpdated(NeutronPort port) {
        // nothing openDove related changes

    }
    public int canDeletePort(NeutronPort port) {
        // nothing openDove related changes
        return 200;
    }
    public void neutronPortDeleted(NeutronPort port) {
        // nothing openDove related changes
    }

    // INeutronRouterAware methods
    public int canCreateRouter(NeutronRouter router) {
        /*
         * openDove specific requirement on create
         */
        if (router.getAdminStateUp() != null && !router.isAdminStateUp())
            return 400;
        return 200;
    }

    public void neutronRouterCreated(NeutronRouter router) {
        // nothing done here by openDove
        ;
    }

    public int canUpdateRouter(NeutronRouter delta, NeutronRouter original) {
        /*
         * attribute changes blocked by openDove
         */
        if (delta.getAdminStateUp() != null || delta.getExternalGatewayInfo() != null)
            return 403;
        return 200;
    }

    public void neutronRouterUpdated(NeutronRouter router) {
        // nothing done here by openDove
        ;
    }

    public int canDeleteRouter(NeutronRouter router) {
        //openDove doesn't block router deletes
        return 200;
    }

    public void neutronRouterDeleted(NeutronRouter router) {
        //openDove doesn't do anything special on router deletes
        ;
    }

    public int canAttachInterface(NeutronRouter router,
            NeutronRouter_Interface routerInterface) {
        //See if a EGW will need to be configured
        //If so, and there aren't GWs available, return BADREQUEST with a message
        INeutronSubnetCRUD neutronSubnetIf = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        NeutronSubnet neutronSubnet = neutronSubnetIf.getSubnet(routerInterface.getSubnetUUID());
        INeutronNetworkCRUD neutronNetworkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork neutronNetwork = neutronNetworkIf.getNetwork(neutronSubnet.getNetworkUUID());
        if (neutronNetwork.isRouterExternal()) {
            IfOpenDoveServiceApplianceCRU serviceApplianceDB = OpenDoveCRUDInterfaces.getIfDoveServiceApplianceCRU(this);
            List<OpenDoveServiceAppliance> oDSAs = serviceApplianceDB.getAppliances();
            Iterator<OpenDoveServiceAppliance> iterator = oDSAs.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().get_isDGW())
                    return 200;
            }
            return 400;
        }
        return 200;
    }

    public void neutronRouterInterfaceAttached(NeutronRouter router,
            NeutronRouter_Interface routerInterface) {
        INeutronSubnetCRUD neutronSubnetIf = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        NeutronSubnet neutronSubnet = neutronSubnetIf.getSubnet(routerInterface.getSubnetUUID());
        INeutronNetworkCRUD neutronNetworkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork neutronNetwork = neutronNetworkIf.getNetwork(neutronSubnet.getNetworkUUID());
        String networkName = "Neutron " + neutronNetwork.getID();
        IfSBDoveSubnetCRUD doveSubnetDB = OpenDoveCRUDInterfaces.getIfDoveSubnetCRUD(this);
        IfSBDoveNetworkCRU doveNetworkDB = OpenDoveCRUDInterfaces.getIfDoveNetworkCRU(this);
        OpenDoveNetwork newODN = doveNetworkDB.getNetworkByName(networkName);
        IfSBDovePolicyCRUD dovePolicyDB = OpenDoveCRUDInterfaces.getIfDovePolicyCRUD(this);
        IfSBDoveEGWSNATPoolCRUD snatPoolDB = OpenDoveCRUDInterfaces.getIfDoveEGWSNATPoolCRUD(this);
        IfNBSystemRU systemDB = OpenDoveCRUDInterfaces.getIfSystemRU(this);
        OpenDoveNeutronControlBlock controlBlock = systemDB.getSystemBlock(); //get system block

        Iterator<NeutronRouter_Interface> interfaceIterator = router.getInterfaces().values().iterator();
        while (interfaceIterator.hasNext()) {
            NeutronRouter_Interface oldInterface = interfaceIterator.next();
            NeutronSubnet oldNeutronSubnet = neutronSubnetIf.getSubnet(oldInterface.getSubnetUUID());
            NeutronNetwork oldNeutronNetwork = neutronNetworkIf.getNetwork(oldNeutronSubnet.getNetworkUUID());
            String oldNetworkUUID = "Neutron " + oldNeutronNetwork.getID();
            OpenDoveNetwork oldODN = doveNetworkDB.getNetworkByName(oldNetworkUUID);

            if (!oldInterface.equals(routerInterface)) {
               logger.debug("Adding connection between "+oldInterface+" and "+routerInterface);
                if (neutronNetwork.isRouterExternal() || oldNeutronNetwork.isRouterExternal()) {
                    if (neutronNetwork.isRouterExternal()) {
                        Iterator<OpenDoveSubnet> i = doveSubnetDB.getSubnets().iterator();
                        while (i.hasNext()) {
                            OpenDoveSubnet oDS = i.next();
                            if (oDS.getAssociatedOSSubnetUUID().equalsIgnoreCase(oldNeutronSubnet.getID()) &&
                                    oDS.getDomainUUID() == oldODN.getDomain_uuid()) {
                                IfOpenDoveServiceApplianceCRU serviceApplianceDB = OpenDoveCRUDInterfaces.getIfDoveServiceApplianceCRU(this);
                                IfSBDoveGwIpv4CRUD gatewayIPDB = OpenDoveCRUDInterfaces.getIfSBDoveGwIpv4CRUD(this);
                                OpenDoveGwIpv4.assignEGWs(serviceApplianceDB, gatewayIPDB, neutronSubnet, controlBlock, oldODN);
                                OpenDoveEGWSNATPool.configureEGWSNATPool(neutronSubnet, snatPoolDB,
                                        controlBlock, oldODN);
                            }
                        }
                    } else {
                        Iterator<OpenDoveSubnet> i = doveSubnetDB.getSubnets().iterator();
                        while (i.hasNext()) {
                            OpenDoveSubnet oDS = i.next();
                            if (oDS.getAssociatedOSSubnetUUID().equalsIgnoreCase(neutronSubnet.getID()) &&
                                    oDS.getDomainUUID() == newODN.getDomain_uuid()) {
                                IfOpenDoveServiceApplianceCRU serviceApplianceDB = OpenDoveCRUDInterfaces.getIfDoveServiceApplianceCRU(this);
                                IfSBDoveGwIpv4CRUD gatewayIPDB = OpenDoveCRUDInterfaces.getIfSBDoveGwIpv4CRUD(this);
                                OpenDoveGwIpv4.assignEGWs(serviceApplianceDB, gatewayIPDB, oldNeutronSubnet, controlBlock, newODN);
                                OpenDoveEGWSNATPool.configureEGWSNATPool(oldNeutronSubnet, snatPoolDB,
                                        controlBlock, newODN);
                            }
                        }
                    }
                } else {
                    if (newODN.getDomain_uuid().equalsIgnoreCase(oldODN.getDomain_uuid())) {
                        OpenDovePolicy.setAllowPolicies(dovePolicyDB, newODN, oldODN);
                    }
                }
            }
        }
    }

     public int canDetachInterface(NeutronRouter router,
            NeutronRouter_Interface routerInterface) {
        // openDove doesn't limit this
        return 200;
    }

    public void neutronRouterInterfaceDetached(NeutronRouter router,
            NeutronRouter_Interface routerInterface) {
        INeutronSubnetCRUD neutronSubnetIf = NeutronCRUDInterfaces.getINeutronSubnetCRUD(this);
        NeutronSubnet neutronSubnet = neutronSubnetIf.getSubnet(routerInterface.getSubnetUUID());
        INeutronNetworkCRUD neutronNetworkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork neutronNetwork = neutronNetworkIf.getNetwork(neutronSubnet.getNetworkUUID());
        String networkUUID = "Neutron " + neutronNetwork.getID();
        IfSBDoveSubnetCRUD doveSubnetDB = OpenDoveCRUDInterfaces.getIfDoveSubnetCRUD(this);
        IfSBDoveNetworkCRU doveNetworkDB = OpenDoveCRUDInterfaces.getIfDoveNetworkCRU(this);
        OpenDoveNetwork newODN = doveNetworkDB.getNetworkByName(networkUUID);
        IfSBDovePolicyCRUD dovePolicyDB = OpenDoveCRUDInterfaces.getIfDovePolicyCRUD(this);
        IfSBDoveEGWSNATPoolCRUD snatPoolDB = OpenDoveCRUDInterfaces.getIfDoveEGWSNATPoolCRUD(this);
        IfNBSystemRU systemDB = OpenDoveCRUDInterfaces.getIfSystemRU(this);
        OpenDoveNeutronControlBlock controlBlock = systemDB.getSystemBlock(); //get system block

        Iterator<NeutronRouter_Interface> interfaceIterator = router.getInterfaces().values().iterator();
        while (interfaceIterator.hasNext()) {
            NeutronRouter_Interface oldInterface = interfaceIterator.next();
            NeutronSubnet oldNeutronSubnet = neutronSubnetIf.getSubnet(oldInterface.getSubnetUUID());
            NeutronNetwork oldNeutronNetwork = neutronNetworkIf.getNetwork(oldNeutronSubnet.getNetworkUUID());
            String oldNetworkUUID = "Neutron " + oldNeutronNetwork.getID();
            OpenDoveNetwork oldODN = doveNetworkDB.getNetworkByName(oldNetworkUUID);

            if (!oldInterface.equals(routerInterface)) {
                logger.debug("Removing connection between "+oldInterface+" and "+routerInterface);
                if (neutronNetwork.isRouterExternal() || oldNeutronNetwork.isRouterExternal()) {
                    if (neutronNetwork.isRouterExternal()) {
                        Iterator<OpenDoveSubnet> i = doveSubnetDB.getSubnets().iterator();
                        while (i.hasNext()) {
                            OpenDoveSubnet oDS = i.next();
                            if (oDS.getAssociatedOSSubnetUUID().equalsIgnoreCase(oldNeutronSubnet.getID()) &&
                                    oDS.getDomainUUID() == oldODN.getDomain_uuid()) {
                                OpenDoveEGWSNATPool.removeEGWSNATPool(neutronSubnet, snatPoolDB,
                                        controlBlock, oldODN);
                            }
                        }
                    } else {
                        Iterator<OpenDoveSubnet> i = doveSubnetDB.getSubnets().iterator();
                        while (i.hasNext()) {
                            OpenDoveSubnet oDS = i.next();
                            if (oDS.getAssociatedOSSubnetUUID().equalsIgnoreCase(neutronSubnet.getID()) &&
                                    oDS.getDomainUUID() == newODN.getDomain_uuid()) {
                                OpenDoveEGWSNATPool.removeEGWSNATPool(oldNeutronSubnet, snatPoolDB,
                                        controlBlock, newODN);
                            }
                        }
                    }
                } else {
                    if (newODN.getDomain_uuid().equalsIgnoreCase(oldODN.getDomain_uuid())) {
                        OpenDovePolicy.removeAllowPolicies(dovePolicyDB, newODN, oldODN);
                    }
                }
            }
        }
    }

    // INeutronFloatingIPAware methods

    public int canCreateFloatingIP(NeutronFloatingIP floatingIP) {
        // opendove doesn't block anything here
        return 200;
    }

    public void neutronFloatingIPCreated(NeutronFloatingIP floatingIP) {
        OpenDoveEGWFwdRule.mapFloatingIPtoEGWFwdRule(floatingIP, this);
    }

    public int canUpdateFloatingIP(NeutronFloatingIP delta,
            NeutronFloatingIP original) {
        // opendove doesn't block anything here
        return 200;
    }
    public void neutronFloatingIPUpdated(NeutronFloatingIP floatingIP) {
        // if Port-ID is null, look through all EGWFwdRules and set tombstone flag for each
        // if Port-ID is not null, repeat create steps (refactor)
        if (floatingIP.getPortUUID() == null)
            OpenDoveEGWFwdRule.removeEgwFwdRulesForFloatingIP(floatingIP, this);
        else
            OpenDoveEGWFwdRule.mapFloatingIPtoEGWFwdRule(floatingIP, this);
    }
    public int canDeleteFloatingIP(NeutronFloatingIP floatingIP) {
        // opendove doesn't block anything here
        return 200;
    }
    public void neutronFloatingIPDeleted(NeutronFloatingIP floatingIP) {
        // look through all EGWFwdRules and set tombstone flag for each (refactor)
        OpenDoveEGWFwdRule.removeEgwFwdRulesForFloatingIP(floatingIP, this);
    }

    private int canAllocateEGW(String uuid, boolean negate) {
        /* if object is shared and domains are not separated,
         * then we need to configure an external gateway at this point.  If we don't have one
         * don't create the port
         */
        IfNBSystemRU systemDB = OpenDoveCRUDInterfaces.getIfSystemRU(this);
        INeutronNetworkCRUD neutronNetworkIf = NeutronCRUDInterfaces.getINeutronNetworkCRUD(this);
        NeutronNetwork neutronNetwork = neutronNetworkIf.getNetwork(uuid);
        if (!neutronNetwork.isRouterExternal()) {
            if (neutronNetwork.isShared()) {
                OpenDoveNeutronControlBlock controlBlock = systemDB.getSystemBlock(); //get system block
                if ((!negate && controlBlock.getDomainSeparation()) ||
                        (negate && !controlBlock.getDomainSeparation())) {
                    IfOpenDoveServiceApplianceCRU serviceApplianceDB = OpenDoveCRUDInterfaces.getIfDoveServiceApplianceCRU(this);
                    List<OpenDoveServiceAppliance> oDSAs = serviceApplianceDB.getAppliances();
                    Iterator<OpenDoveServiceAppliance> iterator = oDSAs.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().get_isDGW())
                            return 200;
                    }
                    return 400;
                }
            }
        }
        return 200;
    }
}
