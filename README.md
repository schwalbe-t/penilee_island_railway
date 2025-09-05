# penilee_island_railway
A VR game about signalling on an old island railway.

### Lever Colors

- Red - Signal
- Blue - Point
- Orange - Level Crossing

### Signalling Concept

- Lever Frame with interlocking
- Signals don't revert automatically
- Adverse signal changes are made impossible by interlocking
    - Cannot change exit signal of occupied block to danger
    - To allow setting a signal to proceed, the following must be true:
        - All points must align with the track the train is coming from
        - All points must align so that the train doesn't enter a signal from the wrong side
        - All blocks up to the next signal showing danger must be unoccupied
- Track diagram shows block occupancy with flap displays for train number
- Electronic block instruments for sending train to each connected signalling box:
    - CLEAR - signaller may send train
    - BLOCKED - line is occupied by train or receiving end doesn't have space
- Bell for each connected signalling box used to notify downstream signaller to clear receiving end
- Other side of signalling box has a dynamic timetable (in the style of a table on paper) on the wall that automatically gets updated with the latest info for each train (train number, next target station and platform, target region exit direction)
- Trains simply proceed if the signal shows proceed and stop before signals showing danger

### Scoring Concept

- Trains have an expected time for each of their stops / exit, displayed on the timetable (`-02:31` until arrival at "Some Place")
- Trains that arrive at their stop on time add to the score, with delays decreasing the amount of points added
- A train arriving at the wrong platform is a mistake and causes the player to loose points
- A train leaving the region from a wrong exit (if the box connects to multiple other boxes) is a major mistake and causes the player to loose many points
- To encourage the player to keep level crossings open (instead of just always leaving them closed) they give points for the amount of time they are open

### Level Concepts

1. Very basic layout with a single signal, where trains arrive from one direction and leave towards the other - teaches the player about scoring, the EBI, bell, signals and how to change them
2. Layout with a single signal and level crossing, where trains arrive from one direction and leave towards the other - teaches the player about level crossings and interlocking (level crossing can't be opened if the signal is at proceed and vice versa)
3. Layout with a single signal and point, where trains arrive from one direction and can leave towards two others - teaches the player about points (and interlocking with points) and the timetable
4. ... (more complex layouts) ...