![](img/banner-1024x256.png)
# WebStats
WebStats is a spigot plugin that can gather player statistics from multiple
sources and display it on a webpage. It can get data from the **scoreboard**,
from any plugin that stores its data in a **database**, from **PlaceholderAPI**
and player **online/AFK** status. It can also be used as a **Discord webhook**.

[View demo page here](https://dantevg.nl/mods-plugins/WebStats/demo)

![a screenshot of our server a while ago](img/screenshot.png)

## [Getting started](https://github.com/Dantevg/WebStats/wiki/Getting-Started)
To use WebStats, you need:
- A Spigot Minecraft server
- A web server (note: the plugin will not work over https, so make sure the
  webpage isn't served over https either)

See [the wiki](https://github.com/Dantevg/WebStats/wiki/Getting-Started) for installation instructions.

## Plugin config file
<!-- don't remove/rename this heading because it was linked to by config.yml until v1.6 -->
For information about the config file, head over to the [wiki page](https://github.com/Dantevg/WebStats/wiki/Config-file).

## Contributing
If you want to help make WebStats more awesome, you can do so by reporting
a bug, suggesting a feature or helping with documentation. Anything is welcome!

If you like to write something yourself, pull requests are also open. You'll
need to setup the project in a specific way, because I did things in a
maybe-not-so-good way: (for IntelliJ idea)
1. Clone this repo and open in IntelliJ
3. *Project Structure -> Project*: Set project SDK to 1.8
4. *Project Structure -> Libraries*: Download spigot-api (shaded), EssentialsX
   and PlaceholderAPI and add each of them as a Java library
5. *Project Structure -> Libraries*: Add org.jetbrains:annotations from maven
6. *Project Structure -> Artifacts*: Add an empty JAR artifact and add
   "WebStats compile output" to the jar

### Contributors
Thank you to these people for helping out with the plugin by suggesting features and reporting bugs!
- [@Mr_Coco](https://github.com/coco0325): top-100 and performance improvements suggestion ([#12](https://github.com/Dantevg/WebStats/issues/12)),
  HTTPS reverse proxy idea ([#12](https://github.com/Dantevg/WebStats/issues/12)),
  bug report ([#14](https://github.com/Dantevg/WebStats/issues/14))
- [@Dancull47](https://github.com/Dancull47): MySQL feature suggestion ([#2](https://github.com/Dantevg/WebStats/issues/2))
- [@draexo (spigotmc)](https://www.spigotmc.org/members/draexo.2905/): bug report ([spigotmc #13](https://www.spigotmc.org/threads/web-stats.492833/#post-4308888)),
  Discord feature suggestion ([#13](https://github.com/Dantevg/WebStats/issues/13))
- [@valenvaio](https://github.com/valenvaio): bug report ([#16](https://github.com/Dantevg/WebStats/issues/16))
- [@zeus1921](https://github.com/zeus1921): PlaceholderAPI feature suggestion ([#3](https://github.com/Dantevg/WebStats/issues/3)),
  online player filter suggestion ([#6](https://github.com/Dantevg/WebStats/issues/6)),
  column ordering suggestion ([#7](https://github.com/Dantevg/WebStats/issues/7)),
  placeholder storer suggestion ([#8](https://github.com/Dantevg/WebStats/issues/8)),
  bug reports ([#5 (comment)](https://github.com/Dantevg/WebStats/issues/5#issuecomment-902033169))

[1]: https://github.com/Dantevg/WebStats/releases
