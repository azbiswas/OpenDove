/*
 *
 * Copyright (c) 2010-2013 IBM Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *
 *  Header File:
 *      include.h
 *
 *  Abstract:
 *      The main header file for the DCS Server. All DCS Server sources and header
 *      files should include only include this file. This file will resolve all
 *      dependencies.
 *
 *  Author:
 *      DOVE Development Team
 *
 *  Environment:
 *      User World
 *
 *  Revision History:
 *
 */

#ifndef _DPS_INCLUDE_H_
#define _DPS_INCLUDE_H_

#define LOG_TEXT "DOVE-DPS-SERVER: "


// Including local features.h since /usr/include/features.h on 9.43.95.197 
// is incompatible with python2.6 compilation flags.

// Python Headers
#include <Python.h>
// #include "features.h"

// Standard C Headers
#include <signal.h>
#include <stdio.h>
#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#include <unistd.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <syslog.h>
#include <sys/ioctl.h>
#include <asm/unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <net/if.h>
#include <netinet/in_systm.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <netinet/in.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <arpa/inet.h>
#include <sys/select.h>
#include <sys/time.h>
#include <ifaddrs.h>
#include <fcntl.h>
#include <limits.h>
#include <pthread.h>
#include <poll.h>
#include <assert.h>
#include <event2/event.h>
#include <event2/http.h>
#include <event2/buffer.h>
#include <event2/util.h>
#include <event2/http_struct.h>
#include <event2/keyvalq_struct.h>
#include <jansson.h>
#include "status.h"
#include "log.h"
#include "fd_process_public.h"
#include "dps_client_common.h"
#include "dps_pkt.h"
#include "dps_pkt_svr.h"
#include "python_interface.h"
#include "cli_interface.h"
#include "statistics.h"
#include "client_protocol_interface.h"
#include "controller_interface.h"
#include "retransmit_interface.h"
#include "rest_api.h"
#include "cluster_api.h"
#include "uuid_api.h"
#include "rest_client_dove_controller.h"
#include "rest_client_cluster_leader.h"
#include "heartbeat.h"
#include "rest_sync.h"
#include "cluster_rest_client.h"
#include "controller_rest_api.h"
#include "debug_interface.h"
//Include the OS Wrapper files
#include "osw.h"
#include "osw_list.h"
#include "osw_queue.h"
#include "osw_semaphore.h"
#include "osw_task.h"
#include "osw_timer.h"
//#include "dps_get_dmc_info.h"
#define dps_offsetof(_type, _member) ((size_t) &((_type *)0)->_member)

/**
 * \brief Contains the Local IP address of the DPS Node
 */
extern ip_addr_t dcs_local_ip;

/**
 * \brief The DPS REST Services Port
 */
extern short dps_rest_port;

/**
 * \brief Contains the IP address of the DPS Cluster Leader
 */
extern ip_addr_t controller_location;

/**
 * \brief Whether the controller location was configured by user
 */
extern int controller_location_set;

/**
 * \brief Contains the IP address of DPS Cluster Leader in Readable
 *        string format
 */
extern char *controller_location_ip_string;

/**
 * \brief Contains the DSA version string
 */
extern char *dsa_version_string;

/**
 * \brief Contains the Local IP Address
 */
extern char *dcs_local_ip_string;

/**
 * \brief Contains the IP address of the DPS Cluster Leader
 */
extern ip_addr_t dps_cluster_leader;

/**
 * \brief Contains the IP address of DPS Cluster Leader in Readable
 *        string format
 */
extern char *dps_cluster_leader_ip_string;

/*
 ******************************************************************************
 * dcs_set_service_role --                                                *//**
 *
 * \brief This routine starts or stops the DCS service role
 *
 * \param action 1:Activate/Set Local Node, 0:Reset Local Node
 *
 * \return dove_status
 *
 *****************************************************************************/
dove_status dcs_set_service_role(uint32_t action);

/*
 ******************************************************************************
 * dcs_initialize                                                         *//**
 *
 * \brief - Initializes the DCS Server
 *
 * \param[in] udp_port - The UDP Port to run the DCS Server on
 * \param[in] rest_port - The port the REST Services should run on
 * \param[in] fDebugCli - Whether the DebugCli should be started.
 * \param[in] fExitOnCtrlC - Whether the Server Process should exit on
 *                           CTRL+C being pressed. In a development environment
 *                           this should be TRUE (1) while in a Production
 *                           Build this should be FALSE (0).
 * \param[in] python_path - The Location of the Python Scripts. This should be
 *                          NULL in most cases since the code will assume the
 *                          scripts are in the "." directory (i.e. the same
 *                          directory) as the dcslib module.
 *
 * \retval -1: Should never happen - This is an infinite loop.
 *
 ******************************************************************************
 */

int dcs_initialize(int udp_port, int rest_port, int fDebugCli, int fExitOnCtrlC, char *python_path);

/*
 ******************************************************************************
 * print_console --                                                       *//**
 *
 * \brief This routine prints the message to the console. This routine should
 *        be called to print messages to the console.
 *
 * \param output_string - The String To Print
 *
 * \remarks DO NOT log in this routine. Will cause infinite loop :)
 *
 *****************************************************************************/

void print_console(const char *output_string);


#endif /* _DPS_INCLUDE_H_ */
