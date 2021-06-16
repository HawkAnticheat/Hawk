# Hawk
Hawk Anticheat ver. BETA 2012 (DEV)

Hawk is a Spigot server anticheat plugin designed to detect and prevent abuse of the Minecraft protocol. Hawk does not require any dependencies, since it relies on Spigot, Netty, and NMS code. Hawk runs on Spigot/PaperSpigot 1.7_R4 and 1.8_R3 servers.

Unfortunately, because of CraftBukkit's DMCA takedown, this is not a Maven project. If you wish to compile this, you must have Spigot (or other derivatives) servers 1.7_R4 and 1.8_R3, ProtocolLib, and PacketEvents as build dependencies. If you don't want to deal with plib or packetevents, there's a few lines in the entire anticheat that you can delete and it should still compile/work. When I come back from my hiatus around May, I'll consider using Maven, and I'll probably use someone's repo to work around the Spigot 1.7.10 availability problem.

Download JAR here: https://www.spigotmc.org/resources/hawk-anticheat-mc-1-7-10-1-8-x.40343/

Hawk Anticheat Discord: https://discord.gg/rQGb5DV
Minecraft Anticheat Community Discord (you can get in touch with me and other anticheat developers here): https://discord.gg/SEYV96H

## Attribution
If you wish to incorporate code from this project into your own project for distribution, please give me proper attribution (i.e. mention my name and provide this link: https://github.com/HawkAnticheat/Hawk). I don't care if you skid as long as you give credit where credit is due. Please don't sell a Hawk clone with only petty changes.

## Regarding multi-version compatibility
I've been getting suggestions from users wanting me to update Hawk to later versions of Minecraft. I won't. If you're a developer, good news: here's a wiki page covering what needs to be changed and the problems that you will encounter. https://github.com/HawkAnticheat/Hawk/wiki/Updating-Hawk

## Contributing
Feel free to contribute. Small pull requests, please. Ghost-client, aimbot, and autoclicker detections are not allowed.
