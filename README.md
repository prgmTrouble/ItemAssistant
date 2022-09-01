# Item Assistant
## Usage
### `/itemEntityCheckpoint`
The item entity checkpoint command allows players to monitor marked positions for item entities. You may add a checkpoint by simply using the `add` keyword followed by the desired location. You may remove the checkpoint by using the `remove` keyword followed by its location. You can view a list of existing checkpoints by running the command without arguments. Specifying a position followed by either the `whitelist` or `blacklist` keywords and finally the name of an item to add said item to the checkpoint at the said position. An item's name is typically the same as the one used in a `/give` command. If the item has a custom name, however, use the custom name instead. This is intended to help detect missing filter items, however may pose an issue if there are several acceptable items which have custom names. To circumvent this issue, see the `/nameCleanse` section.

#### Shortcuts
 - The `remove` keyword can take an additional position. All monitors between the two selected points will be removed.
 - The `clear` keyword can be used to remove all checkpoints.
 - Instead of specifying a name after the `whitelist` and `blacklist` keywords, you may specify a position. If the said position has a container, all the items in that container will be added to the checkpoint.
 - If no arguments are specified after the `whitelist` and `blacklist` keywords and a container is located at the specified checkpoint's position, all the items in the container will be added to the checkpoint.

### `/nameCleanse`
The name cleansing command allows players to recursively remove custom names from items. This is intended to support the `/itemEntityCheckpoint` command. Specifying no further arguments will cleanse the command sender's inventory. Specifying a block position will cleanse the block's inventory. Specifying two block positions will cleanse the inventories of all containers between the two points.

### `/inventoryInspector`
The inventory inspector allows players to record changes to various inventories for later playback.

#### Monitors
Monitors are marked block positions or entities whose contents are actively recorded. Monitors listening to block inventories will persist regardless of whether an inventory is present or not, which allows multiple shulker boxes to be recorded in a unified playback. Similarly, entities with the same UUID are also considered a part of the same playback. You can summon an entity with a specific UUID by running a command of the form `/summon <entity> <position> {UUID:[I;<a>,<b>,<c>,<d>]}`, where `a`,`b`,`c`, and `d` are 32-bit signed integers of your choice. You can view the UUID of an entity by running a command of the form `/data get entity <entity> UUID`.

Monitors are controlled via the `monitor` keyword in the command. Simply specify a block position, entity selector, or UUID to add monitors. specify a block position, entity selector, or UUID and add the `remove` keyword to remove the monitors associated with the targets.

#### Playbacks
Playbacks are the recorded inventories selected for review. Monitors selected for playback are linked to their own playback inventory. Recorded modifications will be listed in chat as well as reflected in their respective playback inventory. Playbacks are controlled via the `replay` keyword. To add a monitor to the playback, simply specify the block position, entity selector, or UUID of the monitored inventory and the block position of the playback inventory. The command will use the inventory at the selected playback location if it exists, or create a new one otherwise. To remove monitors from the playback, specify the block position, entity selector, or UUID of the monitor followed by the `remove` keyword.

#### Playback Controls
To step forward or backward in the replay, use the `next` or `previous` keywords followed by either the `event` or `tick` keywords. To jump to a specific tick, use the `goto` keyword followed by the tick number. Optionally, you may also specify the event number to skip to that event in the specified tick.

#### Other Controls
 - The `reset` keyword removes all monitors and playbacks.
 - Specifying the `monitor` keyword without any other arguments shows a list of all active monitors. Similarly, specifying the `replay` keyword without any other arguments shows a list of all active playbacks.
 - Specifying the `replay` keyword and the block position, entity selector, or UUID of a monitor shows the location of the associated with the specified monitor.
 - The `clearFiles` keyword clears all files, but keeps the monitors and playbacks.
 - The `maxSize` keyword allows you to view/set a soft limit on the amount of disk space that the command will write. Any modifications after the limit is exceeded will be discarded. The default limit is 4GB.
 - The `currentSize` keyword allows you to see the estimated current disk space that the command is using.