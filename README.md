
# LostWord

Usage:
`java -jar lostWord.jar ADDRESS word1 word2 ... word23`

How to use it
-------------
Program looks for a missing work for 24 BIP39 words. Example of usage:
`java -jar lostWord.jar 19ME3Ea2Lw2MZe5f9AvPeoVPXPtKeUWPBv twelve early treat random theme belt display impulse rookie dignity real wedding dose situate leisure idle river knife december betray economy cloth mad`
finds word 'endless', so the correct seed is:
`twelve endless early treat random theme belt display impulse rookie dignity real wedding dose situate leisure idle river knife december betray economy cloth mad`
This simple demo version works only with 24-word seeds and first address for derivation path m/0/0.

