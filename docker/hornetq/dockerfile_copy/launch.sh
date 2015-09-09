#!/bin/sh
#
# Determines ip-address of the container and binds JBoss EAP to that interface. Pass through of all other props.
#
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
IPADDR=$(ip a s | sed -ne '/127.0.0.1/!{s/^[ \t]*inet[ \t]*\([0-9.]\+\)\/.*$/\1/p}')

$DIR/standalone.sh -b $IPADDR -bmanagement $IPADDR -bunsecure $IPADDR $@
