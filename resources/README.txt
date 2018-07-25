HAWK ANTI-CHEAT
Thank you for using Hawk Anti-Cheat!

COMMAND PERMISSIONS:
hawk.admin - /hawk
hawk.admin.cps - /hawk cps
hawk.admin.kick - /hawk kick
hawk.admin.notify - /hawk notify
hawk.admin.reload - /hawk reload
hawk.admin.unban - /hawk unban
hawk.admin.unmute - /hawk unmute

OTHER PERMISSIONS:
hawk.notify - Notification messages
hawk.bypass.<check> - Bypass specific checks

AUTOMATED COMMAND EXECUTION
All checks have the ability to execute server commands under certain conditions.
This function is called "commandsToRun" and will appear in check configurations in config.yml.
You may list commands to doAction under here as strings, for example: "10:0:msg %player% Stop hacking!"
Supported placeholders are: %player%, %check%, %tps%, %ping%
The syntax is simple. Each string is split into three parts: violation level, delay in seconds, and
command to doAction. "VIOLATION LEVEL:DELAY IN SECONDS:COMMAND TO RUN"
NOTE: Hawk will use default commands if command list is completely empty. To prevent this, add an
empty string to the list like this: - ""

VIOLATION SYSTEM
In attempt to reduce false positives, a violation system has been implemented into Hawk; it is called
a Violation Level (VL). Every player has a VL for every check. Every check adds 1 to a player's VL
every time they fail the check, and for every pass, a small percentage is removed from their VL.

API
An API is built into this jar. There should be some documentation on this on the download page, but
if you cannot find it, there are useful classes located in me.islandscout.hawk.api