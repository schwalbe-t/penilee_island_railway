
# Level Layout Format

The level track layout format is made up of a grid of characters, where each
tile is one character tall and wide, and where each row and column of the grid
is split apart by another (mostly empty) character. For example, a 5x5 grid can
look like this (where each tile is denoted by a `.`):
```
. . . . .

. . . . .

. . . . .

. . . . .

. . . . .
```

In the track layout a dot (`.`) represents the absence of any track piece and
is used to clearly show the grid structure of the file. However, there are also
other characters we can use:
|Character|Represents|
|-|-|
|`-`|A straigt, horizontal track piece spanning one tile.|
|`\|`|A straight, vertical track piece spanning one tile.|
|`/` or `\`|A straight, diagonal track piece spanning one tile.|
|`*`|All possible track pieces that correctly connect to the neighboring tiles, possibly creating a point (if more than one of the track pieces connect to the same neighbor).|

Let's use this to construct a simple straight piece of track:
```
. . . . .

. . . . .

- - - - -

. . . . .

. . . . .
```

This will correctly place our straight track pieces, but there is an issue -
the train doesn't consider these pieces as being connected. This is where the
spaces that we left between the rows and columns come in - we can use them to
specify properties about the tiles around them:
|Character|Description|
|-|-|
|`-`|Connects the tile to its left with the tile to its right.|
|`\|`|Connects the tile above it with the tile below it.|
|`/`|Connects the tile to its bottom left with the tile to its top right.|
|`\`|Connects the tile to its bottom right with the tile to its top left.|
|`F`|Adds a directional signal when placed *to the right* of a *connecting* `-`, `\|`, `/` or `\`.|
|`#`|Adds a level crossing if placed *on both sides* of a *tile* `-` or `\|`.|
|`0`|Adds an entry point for trains when placed at one end of a *tile* `-`, `\|`, `/` or `\`.|
|`O`|Adds an exit point for trains when placed at the end of a *tile* `-`, `\|`, `/` or `\`.|

Note that `-`, `|`, `/` and `\` as connectors do not enforce a specific direction.
Also note that the `*` tile requires connectors to correctly filter for possible
track pieces (otherwise it will just place all of them) 

We can use this to make a simple loop with a spawn point siding:
```
. . *--0.
  F/      
. *---* .
 /     \F
* . . . *
|       |
* . . . *
F\     /  
. *---* .
```