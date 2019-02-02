HAWK ANTICHEAT
Thank you for using Hawk Anticheat!

COMMAND PERMISSIONS:
hawk.cmd - /hawk
hawk.cmd.kick - /hawk kick
hawk.cmd.mouserec - /hawk mouserec
hawk.cmd.reload - /hawk reload
hawk.cmd.talerts - /hawk talerts
hawk.cmd.unban - /hawk unban
hawk.cmd.unmute - /hawk unmute

OTHER PERMISSIONS:
hawk.alerts - Notification messages
hawk.bypass.<check> - Bypass specific checks
hawk.gui - Access to GUI

AUTOMATED COMMAND EXECUTION
All checks have the ability to execute server commands under certain conditions.
This function is called "punishCommands" and will appear in check configurations in config.yml.
You may list commands to run under here as strings, for example: "10:0:msg %player% Stop hacking!"
Supported placeholders are: %player%, %check%, %tps%, %ping%
The syntax is simple. Each string is split into three parts: violation level, delay in seconds, and
command to run. "VIOLATION LEVEL:DELAY IN SECONDS:COMMAND TO RUN"
NOTE: Hawk will use default commands if command list is completely empty. To prevent this, add an
empty string to the list like this: - ""

VIOLATION SYSTEM:
In attempt to reduce false positives, a violation system has been implemented into Hawk; it is called
a Violation Level (VL). Every player has a VL for every check. Every check adds 1 to a player's VL
every time they fail the check, and for every pass, a small percentage is removed from their VL.

MOUSE RECORDING FUNCTIONALITY:
Hawk features a mouse recording function. It is designed to help users manually identify combat
cheaters on their server. You can record a suspect's mouse movements by running the command "/hawk
mouserec <player> [duration in seconds]". A PNG file will be generated in "/plugins/Hawk/recordings"
for you to analyze.
