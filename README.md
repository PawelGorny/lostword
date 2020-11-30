# LostWord
Tool for finding missing word for BIP39/BIP32 seed, having (n-1) known ordered words.

Usage:
`java -jar lostWord.jar configurationFile`

If your problem cannot be covered by any of modes below, please contact me, I will try to modify program accordingly or help you find a seed.

How to use it
-------------
Program looks for a missing word for BIP39 seed, using BIP32 derivation path.
Please check files in /examples/ folder to see how to set up the configuration file.
Configuration file expects: address, number of words, known words and additionally derivation path. If not specified, the default will be used (m/0/0).
This version checks only one address - for the given path. In the future (or if requested) I will add possibility to verify all the addresses up to address number x. Today, if you know address but you do not know if it was first or second from the derivation path, you must launch program twice, using two different paths (m/0/0 and m/0/1).
It is possible to launch tests against 'hardened' addresses, using ' (apostrophe) as the last character of path.
Using page https://iancoleman.io/bip39/ you may easily check what to expect for the given seed.

TODO
----
<ol>
<li>checking several addresses for the path (up to address number X)</li>
<li>add support for BIP44, BIP49, BIP84,BIP141</li>
</ol>