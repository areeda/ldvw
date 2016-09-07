#!/bin/bash
shopt -q login_shell

if [ $? -eq 0 ]; then

#caluclate uptime
s=$(awk '{print $1;}' </proc/uptime)
m=$(dc -e "$s 60 / 60 % p")
h=$(dc -e "$s 3600 / 24 % p")
d=$(dc -e "$s 3600 / 24 / p")
up=$(printf "%d days, %d:%02d" $d $h $m)

cores=$(grep -c ^processor /proc/cpuinfo 2>/dev/null)
[ "$cores" -eq "0" ] && cores=1

load=$(cut -f1 -d ' ' /proc/loadavg)
diskUsage=$(df -h --direct / | tail -n 1 | awk '{print $5 " of " $2;}')
diskUsage2=$(df -h --direct /usr1 | tail -n 1 | awk '{print $5 " of " $2;}')

nproc=$(ls -1d /proc/[0-9][0-9]*|wc -l)
nusers=$( who | wc -l)
memTotal=$(grep MemTotal: /proc/meminfo|awk '{print $2;}')
memFree=$(grep MemFree: /proc/meminfo|awk '{print $2;}')
let memUsed=${memTotal}-${memFree}
pctMemUsed=$(expr ${memUsed} \* 100 / ${memTotal})

swapTotal=$(grep SwapTotal: /proc/meminfo|awk '{print $2;}')
swapFree=$(grep SwapFree: /proc/meminfo|awk '{print $2;}')
let swapUsed=${swapTotal}-${swapFree}
pctSwapUsed=$(expr ${swapUsed} \* 100 / ${swapTotal} )

netIfaces=$(grep -v lo /proc/net/if_inet6 | awk '{print $6;}'|sort)
ethAddr[0]=""
ethAddr[1]=""
ethAddr[2]=""

i=0
for n in ${netIfaces}
do
    ethAddr[$i]="${n} $(ifconfig ${n} | grep "inet addr"|awk '{print $2;}')"
    let i=$i+1
done
echo
echo $(cat /etc/redhat-release) $(uname -r)
pr --columns=2 --omit-pagination --omit-header <<-ENDTXT
	Uptime:           ${up}
	Cores:            ${cores}
	Load:             ${load}
	Disk Usage /:     ${diskUsage}
	Disk Usage /usr1: ${diskUsage2}
	# processes:      ${nproc}
	# users:          ${nusers}
	Mem total:        ${memTotal} KB
	Mem used:         ${pctMemUsed}%
	Swap Total:       ${swapTotal} KB
	Swap used:        ${pctSwapUsed}%
	Ethernet:    
	    ${ethAddr[0]}
	    ${ethAddr[1]}
	    ${ethAddr[2]}
ENDTXT
fi
